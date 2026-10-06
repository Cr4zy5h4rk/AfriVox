package com.afrivox.app.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RecordingReviewTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static RecordingReview getRecordingReviewSample1() {
        return new RecordingReview().id(1L).note("note1");
    }

    public static RecordingReview getRecordingReviewSample2() {
        return new RecordingReview().id(2L).note("note2");
    }

    public static RecordingReview getRecordingReviewRandomSampleGenerator() {
        return new RecordingReview().id(longCount.incrementAndGet()).note(UUID.randomUUID().toString());
    }
}
