package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DatasetRequestTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static DatasetRequest getDatasetRequestSample1() {
        return new DatasetRequest()
            .id(1L)
            .orgName("orgName1")
            .hours(1)
            .speakersTarget(1)
            .conversationType("conversationType1")
            .description("description1")
            .quoteAmountFcfa(1);
    }

    public static DatasetRequest getDatasetRequestSample2() {
        return new DatasetRequest()
            .id(2L)
            .orgName("orgName2")
            .hours(2)
            .speakersTarget(2)
            .conversationType("conversationType2")
            .description("description2")
            .quoteAmountFcfa(2);
    }

    public static DatasetRequest getDatasetRequestRandomSampleGenerator() {
        return new DatasetRequest()
            .id(longCount.incrementAndGet())
            .orgName(UUID.randomUUID().toString())
            .hours(intCount.incrementAndGet())
            .speakersTarget(intCount.incrementAndGet())
            .conversationType(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .quoteAmountFcfa(intCount.incrementAndGet());
    }
}
