package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DatasetVersionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static DatasetVersion getDatasetVersionSample1() {
        return new DatasetVersion().id(1L).label("label1").speakers(1).pricePerHourFcfa(1).packagePath("packagePath1").sha256("sha2561");
    }

    public static DatasetVersion getDatasetVersionSample2() {
        return new DatasetVersion().id(2L).label("label2").speakers(2).pricePerHourFcfa(2).packagePath("packagePath2").sha256("sha2562");
    }

    public static DatasetVersion getDatasetVersionRandomSampleGenerator() {
        return new DatasetVersion()
            .id(longCount.incrementAndGet())
            .label(UUID.randomUUID().toString())
            .speakers(intCount.incrementAndGet())
            .pricePerHourFcfa(intCount.incrementAndGet())
            .packagePath(UUID.randomUUID().toString())
            .sha256(UUID.randomUUID().toString());
    }
}
