package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ApiKeyTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static ApiKey getApiKeySample1() {
        return new ApiKey().id(1L).name("name1").prefix("prefix1").keyHash("keyHash1");
    }

    public static ApiKey getApiKeySample2() {
        return new ApiKey().id(2L).name("name2").prefix("prefix2").keyHash("keyHash2");
    }

    public static ApiKey getApiKeyRandomSampleGenerator() {
        return new ApiKey()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .prefix(UUID.randomUUID().toString())
            .keyHash(UUID.randomUUID().toString());
    }
}
