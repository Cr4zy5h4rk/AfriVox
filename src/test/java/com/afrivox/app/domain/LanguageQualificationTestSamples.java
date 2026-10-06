package com.afrivox.app.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class LanguageQualificationTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static LanguageQualification getLanguageQualificationSample1() {
        return new LanguageQualification().id(1L);
    }

    public static LanguageQualification getLanguageQualificationSample2() {
        return new LanguageQualification().id(2L);
    }

    public static LanguageQualification getLanguageQualificationRandomSampleGenerator() {
        return new LanguageQualification().id(longCount.incrementAndGet());
    }
}
