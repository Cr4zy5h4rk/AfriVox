package com.afrivox.app.service;

import java.util.Optional;

/**
 * Detection du format audio par les premiers octets du fichier. Le type MIME et le nom envoyes par le client ne sont
 * jamais utilises : ils sont falsifiables. Formats acceptes : ceux que produit {@code MediaRecorder} selon le
 * navigateur (webm, ogg, mp4/m4a) et le WAV.
 */
public final class AudioFormat {

    private AudioFormat() {}

    /** Extension du fichier a stocker, ou vide si le contenu n'est pas un format audio accepte. */
    public static Optional<String> detectExtension(byte[] head) {
        if (head.length < 12) {
            return Optional.empty();
        }
        if ((head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45 && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3) {
            return Optional.of("webm"); // en-tete EBML (Matroska/WebM)
        }
        if (startsWith(head, 0, "OggS")) {
            return Optional.of("ogg");
        }
        if (startsWith(head, 0, "RIFF") && startsWith(head, 8, "WAVE")) {
            return Optional.of("wav");
        }
        if (startsWith(head, 4, "ftyp")) {
            return Optional.of("m4a");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, String ascii) {
        for (int i = 0; i < ascii.length(); i++) {
            if (data[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
