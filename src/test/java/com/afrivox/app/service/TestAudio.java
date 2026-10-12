package com.afrivox.app.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/** Fabrique de fichiers audio synthetiques pour les tests (aucune donnee reelle). */
final class TestAudio {

    private TestAudio() {}

    /** WAV PCM 16 bits mono 16 kHz dont les echantillons sont pseudo-aleatoires mais reproductibles. */
    static byte[] pcm16Wav(int samples, long seed) {
        int dataSize = samples * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        b.putInt(36 + dataSize);
        b.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        b.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        b.putInt(16);
        b.putShort((short) 1);
        b.putShort((short) 1);
        b.putInt(16_000);
        b.putInt(32_000);
        b.putShort((short) 2);
        b.putShort((short) 16);
        b.put("data".getBytes(StandardCharsets.US_ASCII));
        b.putInt(dataSize);
        byte[] body = new byte[dataSize];
        new Random(seed).nextBytes(body);
        b.put(body);
        return b.array();
    }

    /** WAV PCM 16 bits d'echantillons donnes ([-1, 1]) ; en stereo, le meme signal sur les deux canaux. */
    static byte[] wav(double[] samples, int sampleRate, int channels) {
        int dataSize = samples.length * channels * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        b.putInt(36 + dataSize);
        b.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        b.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        b.putInt(16);
        b.putShort((short) 1);
        b.putShort((short) channels);
        b.putInt(sampleRate);
        b.putInt(sampleRate * channels * 2);
        b.putShort((short) (channels * 2));
        b.putShort((short) 16);
        b.put("data".getBytes(StandardCharsets.US_ASCII));
        b.putInt(dataSize);
        for (double s : samples) {
            short v = (short) Math.max(-32768, Math.min(32767, Math.round(s * 32768)));
            for (int c = 0; c < channels; c++) {
                b.putShort(v);
            }
        }
        return b.array();
    }

    /**
     * Signal « parole » : salves de 300 ms (ton de 220 Hz, amplitude {@code amplitude}) separees de pauses de
     * 200 ms au niveau {@code pauseLevel} (bruit de fond). Un vrai rythme de phrase, pas un ton continu.
     */
    static double[] speechLike(int sampleRate, double seconds, double amplitude, double pauseLevel) {
        int n = (int) (sampleRate * seconds);
        double[] x = new double[n];
        Random noise = new Random(11);
        int burst = (int) (0.3 * sampleRate);
        int pause = (int) (0.2 * sampleRate);
        for (int i = 0; i < n; i++) {
            boolean inBurst = i % (burst + pause) < burst;
            x[i] = inBurst ? amplitude * Math.sin((2 * Math.PI * 220 * i) / sampleRate) : pauseLevel * (noise.nextDouble() * 2 - 1);
        }
        return x;
    }

    static double[] constant(int n, double value) {
        double[] x = new double[n];
        java.util.Arrays.fill(x, value);
        return x;
    }
}
