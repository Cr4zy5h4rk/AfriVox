package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Copie personnalisee d'une archive : noms, ordre, marquage, sommes de controle et textes de licence. */
class ArchivePersonalizerTest {

    private static final String FINGERPRINT = "00112233445566778899aabbccddeeff";
    private static final String OTHER_FINGERPRINT = "ffeeddccbbaa99887766554433221100";

    @TempDir
    Path dir;

    private Path archive;
    private byte[] wav;
    private byte[] webm;

    @BeforeEach
    void setUp() throws IOException {
        wav = TestAudio.pcm16Wav(4_000, 1);
        webm = new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 1, 2, 3, 4, 5, 6, 7, 8 };

        String manifest = "id\tfile\ttext\n1\taudio/a1.wav\tnanga def\n2\taudio/b2.webm\tmangi fi rekk\n";
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("audio/a1.wav", wav);
        entries.put("audio/b2.webm", webm);
        entries.put("manifest.tsv", utf8(manifest));
        entries.put("splits/train.tsv", utf8("id\tfile\ttext\n1\taudio/a1.wav\tnanga def\n"));
        entries.put("splits/test.tsv", utf8("id\tfile\ttext\n2\taudio/b2.webm\t\n"));
        entries.put("LICENSE.txt", utf8("licence de publication"));
        entries.put("DATASHEET.md", utf8("# Fiche\n- Licence : RESEARCH\n- Heures : 1\n"));
        entries.put("checksums.sha256", utf8("ancienne somme  audio/a1.wav\n"));
        archive = dir.resolve("reference.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue());
                out.closeEntry();
            }
        }
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, byte[]> personalize(String fingerprint) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArchivePersonalizer.personalize(archive, out, fingerprint, "PROVENANCE", "licence acquise", "COMMERCIAL");
        return unzip(out.toByteArray());
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                entries.put(e.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String text(Map<String, byte[]> entries, String name) {
        return new String(entries.get(name), StandardCharsets.UTF_8);
    }

    // ---- Noms ----------------------------------------------------------------------------------------------------

    @Test
    void hashedNameIsDeterministicAndKeepsTheExtension() {
        String name = ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav");

        assertThat(name).matches("audio/[0-9a-f]{16}\\.wav");
        assertThat(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav")).isEqualTo(name);
    }

    @Test
    void hashedNameDependsOnBothFingerprintAndOriginalName() {
        String name = ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav");

        assertThat(ArchivePersonalizer.hashedName(OTHER_FINGERPRINT, "audio/a1.wav")).isNotEqualTo(name);
        assertThat(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a2.wav")).isNotEqualTo(name);
    }

    @Test
    void hashedNameHandlesAFileWithoutExtension() {
        assertThat(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/sans_extension")).matches("audio/[0-9a-f]{16}");
    }

    @Test
    void dotInADirectoryIsNotAnExtension() {
        assertThat(ArchivePersonalizer.hashedName(FINGERPRINT, "audio.v2/fichier")).matches("audio/[0-9a-f]{16}");
    }

    @Test
    void audioNamesListsOnlyTheAudioEntries() throws IOException {
        assertThat(ArchivePersonalizer.audioNames(archive)).containsExactlyInAnyOrder("audio/a1.wav", "audio/b2.webm");
    }

    // ---- Copie ---------------------------------------------------------------------------------------------------

    @Test
    void audioFilesAreRenamedAndOriginalNamesDisappear() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        assertThat(copy).doesNotContainKeys("audio/a1.wav", "audio/b2.webm");
        assertThat(copy).containsKeys(
            ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav"),
            ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm")
        );
    }

    @Test
    void reportsHowManyFilesWereWatermarked() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ArchivePersonalizer.Result result = ArchivePersonalizer.personalize(archive, out, FINGERPRINT, "p", "l", "RESEARCH");

        assertThat(result.audioFiles()).isEqualTo(2);
        // Seul le WAV PCM 16 bits est marque ; le webm ne l'est pas (limite assumee).
        assertThat(result.watermarked()).isEqualTo(1);
    }

    @Test
    void wavIsWatermarkedWithTheBuyersKeyAndOtherFormatsAreUntouched() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        byte[] marked = copy.get(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav"));
        assertThat(marked).isNotEqualTo(wav);
        assertThat(WavWatermark.score(marked, ArchivePersonalizer.watermarkKey(FINGERPRINT))).isEqualTo(1.0);
        assertThat(WavWatermark.score(marked, ArchivePersonalizer.watermarkKey(OTHER_FINGERPRINT))).isBetween(0.4, 0.6);
        assertThat(copy.get(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm"))).isEqualTo(webm);
    }

    @Test
    void manifestPointsToTheNewNamesAndKeepsPlatformIdentifiers() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);
        String a = ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav");
        String b = ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm");

        String[] lines = text(copy, "manifest.tsv").split("\n");

        assertThat(lines[0]).isEqualTo("id\tfile\ttext");
        assertThat(List.of(lines).subList(1, lines.length)).containsExactlyInAnyOrder(
            "1\t" + a + "\tnanga def",
            "2\t" + b + "\tmangi fi rekk"
        );
    }

    @Test
    void splitsAreRenamedToo() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        assertThat(text(copy, "splits/train.tsv")).contains(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav"));
        assertThat(text(copy, "splits/test.tsv")).contains(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm"));
        assertThat(text(copy, "splits/test.tsv")).doesNotContain("audio/b2.webm");
    }

    @Test
    void licenseAndDatasheetReflectWhatWasPurchased() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        assertThat(text(copy, "LICENSE.txt")).isEqualTo("licence acquise");
        assertThat(text(copy, "DATASHEET.md"))
            .contains("- Licence : COMMERCIAL (voir LICENSE.txt)")
            .doesNotContain("RESEARCH")
            .contains("- Heures : 1");
    }

    @Test
    void provenanceIsAdded() throws IOException {
        assertThat(text(personalize(FINGERPRINT), "PROVENANCE.txt")).isEqualTo("PROVENANCE");
    }

    @Test
    void checksumsAreRecomputedForTheCopy() throws Exception {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        Map<String, String> listed = new HashMap<>();
        for (String line : text(copy, "checksums.sha256").split("\n")) {
            String[] parts = line.split("  ", 2);
            listed.put(parts[1], parts[0]);
        }
        assertThat(listed).doesNotContainKey("checksums.sha256");
        assertThat(listed.keySet()).containsExactlyInAnyOrderElementsOf(withoutChecksumFile(copy));
        for (Map.Entry<String, String> e : listed.entrySet()) {
            assertThat(e.getValue())
                .as(e.getKey())
                .isEqualTo(sha256(copy.get(e.getKey())));
        }
        assertThat(text(copy, "checksums.sha256")).doesNotContain("ancienne somme");
    }

    private static List<String> withoutChecksumFile(Map<String, byte[]> copy) {
        List<String> names = new ArrayList<>(copy.keySet());
        names.remove("checksums.sha256");
        return names;
    }

    @Test
    void sameFingerprintGivesTheSameCopyContent() throws IOException {
        Map<String, byte[]> first = personalize(FINGERPRINT);
        Map<String, byte[]> second = personalize(FINGERPRINT);

        assertThat(second.keySet()).containsExactlyElementsOf(first.keySet());
        first.forEach((name, bytes) -> assertThat(second.get(name)).as(name).isEqualTo(bytes));
    }

    @Test
    void differentBuyersGetDifferentNamesButTheSameData() throws IOException {
        Map<String, byte[]> a = personalize(FINGERPRINT);
        Map<String, byte[]> b = personalize(OTHER_FINGERPRINT);

        assertThat(a.keySet()).isNotEqualTo(b.keySet());
        // Le webm n'est pas marque : son contenu est identique sous deux noms differents.
        assertThat(a.get(ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm"))).isEqualTo(
            b.get(ArchivePersonalizer.hashedName(OTHER_FINGERPRINT, "audio/b2.webm"))
        );
    }

    @Test
    void writesNothingElseThanTheExpectedEntries() throws IOException {
        Map<String, byte[]> copy = personalize(FINGERPRINT);

        assertThat(copy.keySet()).containsExactlyInAnyOrder(
            ArchivePersonalizer.hashedName(FINGERPRINT, "audio/a1.wav"),
            ArchivePersonalizer.hashedName(FINGERPRINT, "audio/b2.webm"),
            "manifest.tsv",
            "splits/train.tsv",
            "splits/test.tsv",
            "LICENSE.txt",
            "DATASHEET.md",
            "PROVENANCE.txt",
            "checksums.sha256"
        );
    }

    @Test
    void referenceArchiveIsLeftUntouched() throws IOException {
        byte[] before = Files.readAllBytes(archive);
        OutputStream out = new ByteArrayOutputStream();

        ArchivePersonalizer.personalize(archive, out, FINGERPRINT, "p", "l", "RESEARCH");

        assertThat(Files.readAllBytes(archive)).isEqualTo(before);
    }

    // ---- Tables --------------------------------------------------------------------------------------------------

    @Test
    void rewriteTableKeepsTheHeaderFirstAndDropsBlankLines() {
        String table = "id\tfile\ttext\n1\taudio/a.wav\tx\n\n2\taudio/b.wav\ty\n";

        String[] lines = ArchivePersonalizer.rewriteTable(table, Map.of("audio/a.wav", "audio/AAA.wav"), FINGERPRINT).split("\n");

        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("id\tfile\ttext");
    }

    @Test
    void rewriteTableLeavesUnknownFileNamesAlone() {
        String result = ArchivePersonalizer.rewriteTable("id\tfile\n1\taudio/inconnu.wav\n", Map.of(), FINGERPRINT);

        assertThat(result).isEqualTo("id\tfile\n1\taudio/inconnu.wav\n");
    }

    @Test
    void rowOrderIsPerBuyerButStableForOneBuyer() {
        StringBuilder table = new StringBuilder("id\tfile\n");
        for (int i = 1; i <= 30; i++) {
            table.append(i).append("\taudio/").append(i).append(".wav\n");
        }

        String first = ArchivePersonalizer.rewriteTable(table.toString(), Map.of(), FINGERPRINT);

        assertThat(ArchivePersonalizer.rewriteTable(table.toString(), Map.of(), FINGERPRINT)).isEqualTo(first);
        assertThat(ArchivePersonalizer.rewriteTable(table.toString(), Map.of(), OTHER_FINGERPRINT)).isNotEqualTo(first);
        assertThat(first).isNotEqualTo(table.toString());
    }

    // ---- Fiche ---------------------------------------------------------------------------------------------------

    @Test
    void licenseLineIsReplacedAndOtherLinesAreKept() {
        String sheet = "# Fiche\n- Licence : RESEARCH\n- Locuteurs : 12\n";

        assertThat(ArchivePersonalizer.withLicenseLine(sheet, "PROTOTYPE")).isEqualTo(
            "# Fiche\n- Licence : PROTOTYPE (voir LICENSE.txt)\n- Locuteurs : 12\n"
        );
    }

    @Test
    void licenseNameWithRegexCharactersIsInsertedLiterally() {
        assertThat(ArchivePersonalizer.withLicenseLine("- Licence : X\n", "A$1\\B")).contains("- Licence : A$1\\B (voir LICENSE.txt)");
    }

    @Test
    void datasheetWithoutLicenseLineIsUnchanged() {
        assertThat(ArchivePersonalizer.withLicenseLine("# Fiche\n", "COMMERCIAL")).isEqualTo("# Fiche\n");
    }
}
