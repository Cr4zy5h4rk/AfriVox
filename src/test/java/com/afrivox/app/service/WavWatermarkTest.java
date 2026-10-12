package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Marquage inaudible des copies achetees : sert a retrouver l'acheteur d'un fichier diffuse. */
class WavWatermarkTest {

    private static final byte[] KEY_A = "empreinte-achat-A".getBytes(StandardCharsets.UTF_8);
    private static final byte[] KEY_B = "empreinte-achat-B".getBytes(StandardCharsets.UTF_8);

    /** WAV mono 16 kHz ; {@code format} 1 = PCM, 3 = flottant ; {@code bits} = profondeur. */
    private static byte[] wav(int samples, int format, int bits) {
        int bytesPerSample = bits / 8;
        int dataSize = samples * bytesPerSample;
        ByteBuffer b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        b.putInt(36 + dataSize);
        b.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        b.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        b.putInt(16);
        b.putShort((short) format);
        b.putShort((short) 1);
        b.putInt(16_000);
        b.putInt(16_000 * bytesPerSample);
        b.putShort((short) bytesPerSample);
        b.putShort((short) bits);
        b.put("data".getBytes(StandardCharsets.US_ASCII));
        b.putInt(dataSize);
        byte[] body = new byte[dataSize];
        new Random(42).nextBytes(body);
        b.put(body);
        return b.array();
    }

    private static byte[] pcm16(int samples) {
        return wav(samples, 1, 16);
    }

    @Test
    void embeddedMarkIsFoundWithItsKey() {
        byte[] marked = WavWatermark.embed(pcm16(8_000), KEY_A);

        assertThat(marked).isNotNull();
        assertThat(WavWatermark.score(marked, KEY_A)).isEqualTo(1.0);
    }

    @Test
    void anotherBuyersKeyDoesNotMatch() {
        byte[] marked = WavWatermark.embed(pcm16(8_000), KEY_A);

        // Sans marquage commun, les bits concordent au hasard : autour de 0,5.
        assertThat(WavWatermark.score(marked, KEY_B)).isBetween(0.45, 0.55);
    }

    @Test
    void anUnmarkedFileDoesNotMatchAnyKey() {
        assertThat(WavWatermark.score(pcm16(8_000), KEY_A)).isBetween(0.45, 0.55);
    }

    @Test
    void markingChangesEachSampleByOneStepAtMostAndKeepsTheHeader() {
        byte[] original = pcm16(4_000);
        byte[] marked = WavWatermark.embed(original, KEY_A);

        assertThat(marked).hasSameSizeAs(original);
        assertThat(Arrays.copyOfRange(marked, 0, 44)).isEqualTo(Arrays.copyOfRange(original, 0, 44));
        for (int i = 44; i < original.length; i++) {
            // Seul le bit de poids faible peut changer : l'ecart est de 0 ou 1.
            assertThat(Math.abs((marked[i] & 0xFF) - (original[i] & 0xFF))).isLessThanOrEqualTo(1);
            if ((i - 44) % 2 == 1) {
                // Octet de poids fort de chaque echantillon : jamais touche.
                assertThat(marked[i]).isEqualTo(original[i]);
            }
        }
    }

    @Test
    void embeddingDoesNotModifyTheInput() {
        byte[] original = pcm16(2_000);
        byte[] copy = original.clone();

        WavWatermark.embed(original, KEY_A);

        assertThat(original).isEqualTo(copy);
    }

    @Test
    void markIsDeterministicForAGivenKey() {
        byte[] original = pcm16(2_000);

        assertThat(WavWatermark.embed(original, KEY_A)).isEqualTo(WavWatermark.embed(original, KEY_A));
        assertThat(WavWatermark.embed(original, KEY_A)).isNotEqualTo(WavWatermark.embed(original, KEY_B));
    }

    @Test
    void floatWavIsNotMarked() {
        byte[] floatWav = wav(4_000, 3, 32);

        assertThat(WavWatermark.embed(floatWav, KEY_A)).isNull();
        assertThat(WavWatermark.score(floatWav, KEY_A)).isNaN();
    }

    @Test
    void eightBitWavIsNotMarked() {
        assertThat(WavWatermark.embed(wav(4_000, 1, 8), KEY_A)).isNull();
    }

    @Test
    void nonWavContentIsNotMarked() {
        byte[] webm = new byte[2_000];
        webm[0] = 0x1A;
        webm[1] = 0x45;
        webm[2] = (byte) 0xDF;
        webm[3] = (byte) 0xA3;

        assertThat(WavWatermark.embed(webm, KEY_A)).isNull();
        assertThat(WavWatermark.score(webm, KEY_A)).isNaN();
        assertThat(WavWatermark.embed(new byte[10], KEY_A)).isNull();
    }

    @Test
    void tooShortFileGivesNoVerdict() {
        // Sous 512 echantillons, une concordance ne prouverait rien : pas de score plutot qu'un faux 1,0.
        byte[] marked = WavWatermark.embed(pcm16(WavWatermark.MIN_DETECT_SAMPLES - 1), KEY_A);

        assertThat(marked).isNotNull();
        assertThat(WavWatermark.score(marked, KEY_A)).isNaN();
    }

    @Test
    void streamedWavWithZeroDataSizeIsStillMarked() {
        // Certains encodeurs ecrivent une taille de donnees a 0 : on prend alors le reste du fichier.
        byte[] original = pcm16(4_000);
        ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN).putInt(40, 0);

        byte[] marked = WavWatermark.embed(original, KEY_A);

        assertThat(marked).isNotNull();
        assertThat(WavWatermark.score(marked, KEY_A)).isEqualTo(1.0);
    }

    @Test
    void partialRewriteLowersTheScoreAccordingly() {
        // Un acheteur qui retouche la moitie du fichier : la concordance tombe vers 0,75 (moitie intacte, moitie au hasard).
        byte[] marked = WavWatermark.embed(pcm16(8_000), KEY_A);
        byte[] noise = new byte[marked.length];
        new Random(7).nextBytes(noise);
        for (int i = 44 + 8_000; i < marked.length; i++) {
            marked[i] = noise[i];
        }

        assertThat(WavWatermark.score(marked, KEY_A)).isBetween(0.70, 0.80);
    }
}
