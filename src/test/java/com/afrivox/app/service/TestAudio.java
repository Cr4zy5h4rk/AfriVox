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
}
