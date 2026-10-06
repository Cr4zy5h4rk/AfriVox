package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PromptTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Prompt getPromptSample1() {
        return new Prompt().id(1L).content("content1");
    }

    public static Prompt getPromptSample2() {
        return new Prompt().id(2L).content("content2");
    }

    public static Prompt getPromptRandomSampleGenerator() {
        return new Prompt().id(longCount.incrementAndGet()).content(UUID.randomUUID().toString());
    }
}
