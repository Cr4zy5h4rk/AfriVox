package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadLogTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static DownloadLog getDownloadLogSample1() {
        return new DownloadLog().id(1L).ip("ip1");
    }

    public static DownloadLog getDownloadLogSample2() {
        return new DownloadLog().id(2L).ip("ip2");
    }

    public static DownloadLog getDownloadLogRandomSampleGenerator() {
        return new DownloadLog().id(longCount.incrementAndGet()).ip(UUID.randomUUID().toString());
    }
}
