package com.afrivox.app.service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Marquage d'un fichier WAV PCM 16 bits : le bit de poids faible de chaque echantillon est remplace par un bit d'une
 * suite pseudo-aleatoire derivee d'une cle (l'empreinte secrete de l'achat). La modification est de l'ordre d'un
 * pas de quantification (inaudible), identique en taille, et relue sans le fichier d'origine : on compare les bits de
 * poids faible du fichier suspect a la suite attendue pour chaque achat (1,0 quand le fichier n'a pas ete retouche,
 * environ 0,5 sinon).
 * <p>
 * Limites assumees : un nouvel encodage avec perte (MP3, Opus), un reechantillonnage ou l'ajout de bruit efface le
 * marquage ; les formats autres que WAV PCM 16 bits ne sont pas marques.
 */
final class WavWatermark {

    /** Nombre d'echantillons relus pour decider (le debut du fichier suffit largement). */
    static final int DETECT_SAMPLES = 65_536;

    /** Sous ce nombre d'echantillons, une concordance ne prouve rien. */
    static final int MIN_DETECT_SAMPLES = 512;

    /** Zone des echantillons : debut dans le tableau et nombre d'echantillons. */
    private record Region(int start, int samples) {}

    private WavWatermark() {}

    /** Copie marquee, ou {@code null} si le fichier n'est pas un WAV PCM 16 bits. */
    static byte[] embed(byte[] wav, byte[] key) {
        Region region = locate(wav);
        if (region == null) {
            return null;
        }
        byte[] marked = wav.clone();
        BitStream bits = new BitStream(key);
        for (int i = 0; i < region.samples(); i++) {
            int at = region.start() + 2 * i;
            marked[at] = (byte) ((marked[at] & 0xFE) | bits.bit(i));
        }
        return marked;
    }

    /**
     * Part des bits de poids faible qui concordent avec la suite de cette cle, entre 0 et 1 ; {@code NaN} si le
     * fichier n'est pas un WAV PCM 16 bits ou est trop court.
     */
    static double score(byte[] wav, byte[] key) {
        Region region = locate(wav);
        if (region == null) {
            return Double.NaN;
        }
        int n = Math.min(region.samples(), DETECT_SAMPLES);
        if (n < MIN_DETECT_SAMPLES) {
            return Double.NaN;
        }
        BitStream bits = new BitStream(key);
        int same = 0;
        for (int i = 0; i < n; i++) {
            if ((wav[region.start() + 2 * i] & 1) == bits.bit(i)) {
                same++;
            }
        }
        return (double) same / n;
    }

    private static Region locate(byte[] w) {
        if (w.length < 44 || !tag(w, 0, "RIFF") || !tag(w, 8, "WAVE")) {
            return null;
        }
        boolean pcm16 = false;
        long pos = 12;
        while (pos + 8 <= w.length) {
            int at = (int) pos;
            long size = leInt(w, at + 4) & 0xFFFFFFFFL;
            int body = at + 8;
            if (tag(w, at, "fmt ")) {
                if (body + 16 > w.length) {
                    return null;
                }
                int format = leShort(w, body);
                int bitsPerSample = leShort(w, body + 14);
                // 1 = PCM ; 0xFFFE = extensible (les sources de la plateforme restent du PCM 16 bits)
                pcm16 = (format == 1 || format == 0xFFFE) && bitsPerSample == 16;
            } else if (tag(w, at, "data")) {
                if (!pcm16) {
                    return null;
                }
                long available = w.length - body;
                // Taille 0 ou trop grande : flux ecrit sans taille definitive, on prend le reste du fichier
                long length = size == 0 || size > available ? available : size;
                int samples = (int) (length / 2);
                return samples > 0 ? new Region(body, samples) : null;
            }
            pos = (long) body + size + (size & 1);
        }
        return null;
    }

    private static boolean tag(byte[] w, int at, String tag) {
        for (int i = 0; i < 4; i++) {
            if (w[at + i] != (byte) tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int leInt(byte[] w, int at) {
        return (w[at] & 0xFF) | ((w[at + 1] & 0xFF) << 8) | ((w[at + 2] & 0xFF) << 16) | ((w[at + 3] & 0xFF) << 24);
    }

    private static int leShort(byte[] w, int at) {
        return (w[at] & 0xFF) | ((w[at + 1] & 0xFF) << 8);
    }

    /** Suite de bits : SHA-256(cle || numero de bloc), 256 bits par bloc. */
    private static final class BitStream {

        private final MessageDigest digest;
        private final byte[] key;
        private byte[] block;
        private long blockIndex = -1;

        BitStream(byte[] key) {
            this.key = key;
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        int bit(int index) {
            long wanted = index >>> 8;
            if (wanted != blockIndex) {
                digest.reset();
                digest.update(key);
                digest.update(ByteBuffer.allocate(8).putLong(wanted).array());
                block = digest.digest();
                blockIndex = wanted;
            }
            int k = index & 255;
            return (block[k >>> 3] >>> (k & 7)) & 1;
        }
    }
}
