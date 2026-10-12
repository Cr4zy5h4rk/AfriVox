package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Purchase;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Detection de l'acheteur d'une copie diffusee : indices sur les noms de fichiers et sur le marquage audio. */
class LeakCheckServiceTest {

    private static final List<String> ORIGINALS = List.of("audio/s1.wav", "audio/s2.wav", "audio/s3.wav", "audio/s4.wav", "audio/s5.wav");

    @TempDir
    Path dir;

    private Path storage;
    private PurchaseRepository purchases;
    private LeakCheckService service;
    private Purchase alice;
    private Purchase bruno;

    @BeforeEach
    void setUp() throws IOException {
        purchases = mock(PurchaseRepository.class);
        ApplicationProperties properties = new ApplicationProperties();
        // Racine de stockage = sous-dossier : « ../ » mene alors a un fichier du dossier temporaire du test, jamais ailleurs.
        storage = Files.createDirectory(dir.resolve("storage"));
        properties.getDatasets().setStorageDir(storage.toString());
        service = new LeakCheckService(purchases, properties);

        Path zip = storage.resolve("v1.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String name : ORIGINALS) {
                out.putNextEntry(new ZipEntry(name));
                out.write(new byte[] { 1, 2, 3 });
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("manifest.tsv"));
            out.write("id\tfile\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        DatasetVersion version = version(1L, "v1.zip");
        alice = purchase(11L, "Alice", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", version);
        bruno = purchase(12L, "Bruno", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", version);
        when(purchases.findAllTraceable()).thenReturn(List.of(alice, bruno));
    }

    private static DatasetVersion version(long id, String packagePath) {
        Dataset dataset = new Dataset();
        dataset.setSlug("agriculture-wolof");
        DatasetVersion version = new DatasetVersion().id(id);
        version.setDataset(dataset);
        version.setLabel("1.0");
        version.setPackagePath(packagePath);
        return version;
    }

    private static Purchase purchase(long id, String buyer, String fingerprint, DatasetVersion version) {
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setProfile(new UserProfile().displayName(buyer));
        purchase.setDatasetVersion(version);
        purchase.setFingerprint(fingerprint);
        purchase.setCreatedAt(Instant.parse("2026-10-10T12:00:00Z"));
        return purchase;
    }

    private static String hashed(Purchase purchase, String original) {
        // « audio/<16>.<ext> » : le meme motif qu'un manifeste de copie diffusee.
        return ArchivePersonalizer.hashedName(purchase.getFingerprint(), original);
    }

    private static String listing(Purchase purchase, int count) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add("1\t" + hashed(purchase, ORIGINALS.get(i)) + "\ttexte");
        }
        return String.join("\n", lines);
    }

    // ---- Noms de fichiers ----------------------------------------------------------------------------------------

    @Test
    void namesOfOneBuyersCopyPointToThatBuyer() {
        LeakCheckService.Report report = service.checkNames("id\tfile\ttext\n" + listing(alice, 5));

        assertThat(report.kind()).isEqualTo("NAMES");
        assertThat(report.inspected()).isEqualTo(5);
        assertThat(report.matches()).hasSize(1);
        LeakCheckService.Match match = report.matches().get(0);
        assertThat(match.purchaseId()).isEqualTo(11L);
        assertThat(match.buyer()).isEqualTo("Alice");
        assertThat(match.datasetSlug()).isEqualTo("agriculture-wolof");
        assertThat(match.versionLabel()).isEqualTo("1.0");
        assertThat(match.score()).isEqualTo(1.0);
    }

    @Test
    void aSubsetOfTheCopyIsStillRecognised() {
        // Trois fichiers sur cinq : le minimum pour qu'une concordance ait un sens.
        LeakCheckService.Report report = service.checkNames(listing(bruno, 3));

        assertThat(report.matches()).extracting(LeakCheckService.Match::purchaseId).containsExactly(12L);
    }

    @Test
    void fewerThanThreeNamesProveNothingAndDoNotQueryPurchases() {
        LeakCheckService.Report report = service.checkNames(listing(alice, 2));

        assertThat(report.matches()).isEmpty();
        assertThat(report.inspected()).isEqualTo(2);
        verify(purchases, never()).findAllTraceable();
    }

    @Test
    void namesDilutedInUnrelatedOnesDoNotMatch() {
        // 3 noms d'Alice noyes dans 4 noms quelconques : 3/7 < 50 %.
        String noise = "0123456789abcdef.wav\nfedcba9876543210.wav\n1111111111111111.wav\n2222222222222222.wav";

        LeakCheckService.Report report = service.checkNames(listing(alice, 3) + "\n" + noise);

        assertThat(report.matches()).isEmpty();
    }

    @Test
    void unrelatedFileGivesNoMatch() {
        LeakCheckService.Report report = service.checkNames(
            "audio/0123456789abcdef.wav\naudio/fedcba9876543210.wav\naudio/1111111111111111.wav\n"
        );

        assertThat(report.matches()).isEmpty();
        assertThat(report.inspected()).isEqualTo(3);
    }

    @Test
    void namesAreFoundInsideQuotedAndPathedListings() {
        String quoted =
            "\"" +
            hashed(alice, ORIGINALS.get(0)) +
            "\",\"copie/" +
            hashed(alice, ORIGINALS.get(1)) +
            "\",\"" +
            hashed(alice, ORIGINALS.get(2)) +
            "\"";

        assertThat(service.checkNames(quoted).matches()).extracting(LeakCheckService.Match::purchaseId).containsExactly(11L);
    }

    @Test
    void versionWithoutArchiveIsSkippedWithoutError() {
        DatasetVersion noArchive = version(2L, null);
        Purchase carol = purchase(13L, "Carol", "cccccccccccccccccccccccccccccccc", noArchive);
        when(purchases.findAllTraceable()).thenReturn(List.of(carol));

        assertThat(service.checkNames(listing(alice, 5)).matches()).isEmpty();
    }

    @Test
    void archivePathOutsideTheStorageRootIsIgnored() throws IOException {
        // Un vrai zip hors de la racine, avec les bons noms : seul le controle de chemin doit l'ecarter.
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(dir.resolve("evasion.zip")))) {
            for (String name : ORIGINALS) {
                out.putNextEntry(new ZipEntry(name));
                out.write(1);
                out.closeEntry();
            }
        }
        DatasetVersion escaping = version(3L, "../evasion.zip");
        Purchase dave = purchase(14L, "Dave", "dddddddddddddddddddddddddddddddd", escaping);
        when(purchases.findAllTraceable()).thenReturn(List.of(dave));

        assertThat(service.checkNames(listing(dave, 5)).matches()).isEmpty();
    }

    @Test
    void corruptedArchiveIsSkippedWithoutError() throws IOException {
        Files.writeString(storage.resolve("casse.zip"), "ceci n'est pas un zip");
        DatasetVersion broken = version(4L, "casse.zip");
        Purchase erin = purchase(15L, "Erin", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", broken);
        when(purchases.findAllTraceable()).thenReturn(List.of(erin));

        assertThat(service.checkNames(listing(erin, 5)).matches()).isEmpty();
    }

    // ---- Audio ---------------------------------------------------------------------------------------------------

    @Test
    void markedAudioPointsToTheBuyerWhoseKeyWasEmbedded() {
        byte[] original = TestAudio.pcm16Wav(8_000, 5);
        byte[] copy = WavWatermark.embed(original, ArchivePersonalizer.watermarkKey(bruno.getFingerprint()));

        LeakCheckService.Report report = service.checkAudio(copy);

        assertThat(report.kind()).isEqualTo("AUDIO");
        assertThat(report.inspected()).isEqualTo(2);
        assertThat(report.matches()).extracting(LeakCheckService.Match::purchaseId).containsExactly(12L);
        assertThat(report.matches().get(0).score()).isEqualTo(1.0);
    }

    @Test
    void unmarkedAudioMatchesNobody() {
        assertThat(service.checkAudio(TestAudio.pcm16Wav(8_000, 5)).matches()).isEmpty();
    }

    @Test
    void tooShortAudioGivesNoMatchInsteadOfAFalsePositive() {
        byte[] shortCopy = WavWatermark.embed(TestAudio.pcm16Wav(100, 5), ArchivePersonalizer.watermarkKey(alice.getFingerprint()));

        assertThat(service.checkAudio(shortCopy).matches()).isEmpty();
    }

    // ---- Point d'entree ------------------------------------------------------------------------------------------

    @Test
    void emptyFileIsRefused() {
        assertThatThrownBy(() -> service.check(new byte[0])).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo("emptyfile")
        );
    }

    @Test
    void wavContentIsAnalysedAsAudio() {
        byte[] copy = WavWatermark.embed(TestAudio.pcm16Wav(8_000, 9), ArchivePersonalizer.watermarkKey(alice.getFingerprint()));

        assertThat(service.check(copy).kind()).isEqualTo("AUDIO");
    }

    @Test
    void anythingElseIsAnalysedAsAListOfNames() {
        LeakCheckService.Report report = service.check(("id\tfile\n" + listing(alice, 4)).getBytes(StandardCharsets.UTF_8));

        assertThat(report.kind()).isEqualTo("NAMES");
        assertThat(report.matches()).extracting(LeakCheckService.Match::purchaseId).containsExactly(11L);
    }
}
