package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MlModelTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static MlModel getMlModelSample1() {
        return new MlModel().id(1L).name("name1").description("description1").license("license1").url("url1").downloads(1);
    }

    public static MlModel getMlModelSample2() {
        return new MlModel().id(2L).name("name2").description("description2").license("license2").url("url2").downloads(2);
    }

    public static MlModel getMlModelRandomSampleGenerator() {
        return new MlModel()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .license(UUID.randomUUID().toString())
            .url(UUID.randomUUID().toString())
            .downloads(intCount.incrementAndGet());
    }
}
