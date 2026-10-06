package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AudioFormatTest {

    private static byte[] padded(byte[] start) {
        byte[] data = new byte[32];
        System.arraycopy(start, 0, data, 0, start.length);
        return data;
    }

    @Test
    void detectsWebm() {
        assertThat(AudioFormat.detectExtension(padded(new byte[] { 0x1A, 0x45, (byte) 0xDF, (byte) 0xA3 }))).contains("webm");
    }

    @Test
    void detectsOgg() {
        assertThat(AudioFormat.detectExtension(padded("OggS".getBytes(StandardCharsets.US_ASCII)))).contains("ogg");
    }

    @Test
    void detectsWav() {
        byte[] data = padded("RIFF".getBytes(StandardCharsets.US_ASCII));
        System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, data, 8, 4);
        assertThat(AudioFormat.detectExtension(data)).contains("wav");
    }

    @Test
    void detectsM4a() {
        byte[] data = new byte[32];
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, data, 4, 4);
        assertThat(AudioFormat.detectExtension(data)).contains("m4a");
    }

    @Test
    void rejectsRiffThatIsNotWave() {
        byte[] data = padded("RIFF".getBytes(StandardCharsets.US_ASCII));
        System.arraycopy("AVI ".getBytes(StandardCharsets.US_ASCII), 0, data, 8, 4);
        assertThat(AudioFormat.detectExtension(data)).isEmpty();
    }

    @Test
    void rejectsExecutableAndTooShortContent() {
        assertThat(AudioFormat.detectExtension(padded("MZ".getBytes(StandardCharsets.US_ASCII)))).isEmpty();
        assertThat(AudioFormat.detectExtension(new byte[] { 1, 2, 3 })).isEmpty();
    }
}
