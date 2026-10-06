package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CampaignTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Campaign getCampaignSample1() {
        return new Campaign().id(1L).title("title1").description("description1").domain("domain1").targetSentences(1);
    }

    public static Campaign getCampaignSample2() {
        return new Campaign().id(2L).title("title2").description("description2").domain("domain2").targetSentences(2);
    }

    public static Campaign getCampaignRandomSampleGenerator() {
        return new Campaign()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .domain(UUID.randomUUID().toString())
            .targetSentences(intCount.incrementAndGet());
    }
}
