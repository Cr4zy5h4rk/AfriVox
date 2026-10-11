package com.afrivox.app.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Personnalise l'archive d'une version pour un acheteur, a partir de l'archive de reference (qui ne change pas) :
 * <ul>
 *   <li>les fichiers audio changent de nom ({@code audio/<16 caracteres>.<ext>}, derive de l'empreinte de l'achat) ;</li>
 *   <li>les lignes de {@code manifest.tsv} et de {@code splits/*.tsv} sont renommees et reordonnees de la meme facon
 *       (la colonne {@code id} reste celle de la plateforme : le classement des modeles s'en sert) ;</li>
 *   <li>les audios WAV PCM 16 bits portent un marquage inaudible propre a l'achat ({@link WavWatermark}) ;</li>
 *   <li>{@code checksums.sha256} est recalcule et {@code PROVENANCE.txt} previent que la copie est numerotee.</li>
 * </ul>
 * Le contenu (audio, transcriptions, decoupage) est identique pour tous les acheteurs. Rien ici n'empeche une copie :
 * l'objectif est de pouvoir retrouver l'acheteur d'un jeu de donnees diffuse tel quel ou presque, et de dissuader.
 */
final class ArchivePersonalizer {

    private static final String AUDIO_PREFIX = "audio/";

    /** Niveau de compression : l'audio est deja peu compressible, la vitesse prime (copie de plusieurs centaines de Mo). */
    private static final int ZIP_LEVEL = 1;

    /** Resultat : nombre de fichiers audio ecrits et nombre de ceux qui ont recu le marquage. */
    record Result(int audioFiles, int watermarked) {}

    private ArchivePersonalizer() {}

    /** Nom d'un fichier audio dans la copie d'un acheteur ; deterministe pour une empreinte donnee. */
    static String hashedName(String fingerprint, String originalName) {
        String digest = HexFormat.of().formatHex(hmac(fingerprint, "name:" + originalName));
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 && dot > originalName.lastIndexOf('/') ? originalName.substring(dot) : "";
        return AUDIO_PREFIX + digest.substring(0, 16) + extension;
    }

    /** Cle du marquage audio d'un achat. */
    static byte[] watermarkKey(String fingerprint) {
        return ("wm:" + fingerprint).getBytes(StandardCharsets.UTF_8);
    }

    /** Noms d'origine des fichiers audio d'une archive de reference. */
    static List<String> audioNames(Path archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            List<String> names = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(AUDIO_PREFIX)) {
                    names.add(entry.getName());
                }
            }
            return names;
        }
    }

    /**
     * Ecrit la copie personnalisee. L'appelant ferme {@code target}. Les fichiers audio sont lus un a un (5 Mo au
     * plus), jamais l'archive entiere en memoire.
     */
    static Result personalize(
        Path archive,
        OutputStream target,
        String fingerprint,
        String provenance,
        String licenseText,
        String licenseName
    ) throws IOException {
        byte[] watermarkKey = watermarkKey(fingerprint);
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            List<ZipEntry> audio = new ArrayList<>();
            List<ZipEntry> others = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                (entry.getName().startsWith(AUDIO_PREFIX) ? audio : others).add(entry);
            }

            Map<String, String> renamed = new HashMap<>();
            Set<String> taken = new HashSet<>();
            for (ZipEntry entry : audio) {
                String name = hashedName(fingerprint, entry.getName());
                if (!taken.add(name)) {
                    throw new IllegalStateException("Collision de nom dans la copie personnalisee : " + name);
                }
                renamed.put(entry.getName(), name);
            }
            audio.sort(Comparator.comparing(entry -> renamed.get(entry.getName())));

            ZipOutputStream out = new ZipOutputStream(target);
            out.setLevel(ZIP_LEVEL);
            StringBuilder checksums = new StringBuilder();
            int watermarked = 0;

            for (ZipEntry entry : audio) {
                byte[] bytes = read(zip, entry);
                if (entry.getName().toLowerCase(Locale.ROOT).endsWith(".wav")) {
                    byte[] marked = WavWatermark.embed(bytes, watermarkKey);
                    if (marked != null) {
                        bytes = marked;
                        watermarked++;
                    }
                }
                add(out, checksums, renamed.get(entry.getName()), bytes);
            }

            for (ZipEntry entry : others) {
                String name = entry.getName();
                if (name.equals("checksums.sha256")) {
                    continue; // recalcule plus bas : les noms et le contenu ont change
                }
                byte[] bytes = read(zip, entry);
                if (name.equals("manifest.tsv") || (name.startsWith("splits/") && name.endsWith(".tsv"))) {
                    bytes = rewriteTable(new String(bytes, StandardCharsets.UTF_8), renamed, fingerprint).getBytes(StandardCharsets.UTF_8);
                } else if (name.equals("LICENSE.txt")) {
                    // La licence livree est celle que l'acheteur a choisie, pas celle fixee a la publication de la version
                    bytes = licenseText.getBytes(StandardCharsets.UTF_8);
                } else if (name.equals("DATASHEET.md")) {
                    bytes = withLicenseLine(new String(bytes, StandardCharsets.UTF_8), licenseName).getBytes(StandardCharsets.UTF_8);
                }
                add(out, checksums, name, bytes);
            }
            add(out, checksums, "PROVENANCE.txt", provenance.getBytes(StandardCharsets.UTF_8));
            add(out, null, "checksums.sha256", checksums.toString().getBytes(StandardCharsets.UTF_8));
            out.finish();
            return new Result(audio.size(), watermarked);
        }
    }

    /**
     * Renomme la colonne {@code file} (deuxieme colonne) et reordonne les lignes de facon propre a l'acheteur.
     * L'entete reste en premiere ligne ; la colonne {@code id} n'est pas modifiee.
     */
    static String rewriteTable(String content, Map<String, String> renamed, String fingerprint) {
        String[] lines = content.split("\n", -1);
        if (lines.length == 0) {
            return content;
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] columns = lines[i].split("\t", -1);
            if (columns.length > 1 && renamed.containsKey(columns[1])) {
                columns[1] = renamed.get(columns[1]);
            }
            rows.add(columns);
        }
        rows.sort(Comparator.comparing((String[] columns) -> HexFormat.of().formatHex(hmac(fingerprint, "row:" + columns[0]))));
        StringBuilder sb = new StringBuilder(lines[0]).append('\n');
        for (String[] columns : rows) {
            sb.append(String.join("\t", columns)).append('\n');
        }
        return sb.toString();
    }

    /** Remplace la ligne « - Licence : ... » de la fiche par la licence reellement acquise. */
    static String withLicenseLine(String datasheet, String licenseName) {
        return datasheet.replaceAll(
            "(?m)^- Licence : .*$",
            java.util.regex.Matcher.quoteReplacement("- Licence : " + licenseName + " (voir LICENSE.txt)")
        );
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    private static void add(ZipOutputStream out, StringBuilder checksums, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
        if (checksums != null) {
            checksums.append(sha256Hex(bytes)).append("  ").append(name).append('\n');
        }
    }

    private static byte[] hmac(String key, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
