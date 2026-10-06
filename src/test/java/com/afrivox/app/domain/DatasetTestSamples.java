package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DatasetTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Dataset getDatasetSample1() {
        return new Dataset().id(1L).slug("slug1").title("title1").description("description1").domain("domain1");
    }

    public static Dataset getDatasetSample2() {
        return new Dataset().id(2L).slug("slug2").title("title2").description("description2").domain("domain2");
    }

    public static Dataset getDatasetRandomSampleGenerator() {
        return new Dataset()
            .id(longCount.incrementAndGet())
            .slug(UUID.randomUUID().toString())
            .title(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .domain(UUID.randomUUID().toString());
    }
}
