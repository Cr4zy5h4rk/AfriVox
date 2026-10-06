package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfileTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static UserProfile getUserProfileSample1() {
        return new UserProfile().id(1L).displayName("displayName1").country("country1").bio("bio1");
    }

    public static UserProfile getUserProfileSample2() {
        return new UserProfile().id(2L).displayName("displayName2").country("country2").bio("bio2");
    }

    public static UserProfile getUserProfileRandomSampleGenerator() {
        return new UserProfile()
            .id(longCount.incrementAndGet())
            .displayName(UUID.randomUUID().toString())
            .country(UUID.randomUUID().toString())
            .bio(UUID.randomUUID().toString());
    }
}
