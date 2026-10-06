package com.afrivox.app.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CreditTransactionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static CreditTransaction getCreditTransactionSample1() {
        return new CreditTransaction().id(1L).amount(1);
    }

    public static CreditTransaction getCreditTransactionSample2() {
        return new CreditTransaction().id(2L).amount(2);
    }

    public static CreditTransaction getCreditTransactionRandomSampleGenerator() {
        return new CreditTransaction().id(longCount.incrementAndGet()).amount(intCount.incrementAndGet());
    }
}
