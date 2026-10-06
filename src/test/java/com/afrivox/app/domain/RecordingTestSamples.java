package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RecordingTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Recording getRecordingSample1() {
        return new Recording().id(1L).audioPath("audioPath1").durationMs(1).sha256("sha2561").transcript("transcript1");
    }

    public static Recording getRecordingSample2() {
        return new Recording().id(2L).audioPath("audioPath2").durationMs(2).sha256("sha2562").transcript("transcript2");
    }

    public static Recording getRecordingRandomSampleGenerator() {
        return new Recording()
            .id(longCount.incrementAndGet())
            .audioPath(UUID.randomUUID().toString())
            .durationMs(intCount.incrementAndGet())
            .sha256(UUID.randomUUID().toString())
            .transcript(UUID.randomUUID().toString());
    }
}
