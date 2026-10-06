package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LanguageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Language getLanguageSample1() {
        return new Language().id(1L).code("code1").name("name1").dialect("dialect1").minValidators(1);
    }

    public static Language getLanguageSample2() {
        return new Language().id(2L).code("code2").name("name2").dialect("dialect2").minValidators(2);
    }

    public static Language getLanguageRandomSampleGenerator() {
        return new Language()
            .id(longCount.incrementAndGet())
            .code(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .dialect(UUID.randomUUID().toString())
            .minValidators(intCount.incrementAndGet());
    }
}
