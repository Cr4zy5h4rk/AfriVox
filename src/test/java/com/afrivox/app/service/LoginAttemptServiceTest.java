package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.config.ApplicationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    /** Horloge reglable : permet de tester les durees de blocage sans attendre. */
    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-05T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private LoginAttemptService service;

    private final String loginKey = LoginAttemptService.loginKey("1.2.3.4", "Alice");
    private final String ipKey = LoginAttemptService.ipKey("1.2.3.4");

    @BeforeEach
    void setUp() {
        ApplicationProperties props = new ApplicationProperties();
        props.getSecurity().setLoginMaxAttempts(3);
        props.getSecurity().setIpMaxAttempts(5);
        props.getSecurity().setLoginLockSeconds(600);
        clock = new MutableClock();
        service = new LoginAttemptService(props, clock);
    }

    @Test
    void notBlockedBeforeThreshold() {
        service.recordFailure(loginKey, ipKey);
        service.recordFailure(loginKey, ipKey);

        assertThat(service.retryAfterSeconds(loginKey, ipKey)).isZero();
    }

    @Test
    void blockedAtThresholdWithRetryDelay() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(loginKey, ipKey);
        }

        assertThat(service.retryAfterSeconds(loginKey, ipKey)).isBetween(599L, 601L);
    }

    @Test
    void unblockedAfterLockDuration() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(loginKey, ipKey);
        }

        clock.advance(Duration.ofSeconds(601));

        assertThat(service.retryAfterSeconds(loginKey, ipKey)).isZero();
    }

    @Test
    void loginKeyIsCaseInsensitive() {
        assertThat(LoginAttemptService.loginKey("1.2.3.4", "ALICE")).isEqualTo(loginKey);
    }

    @Test
    void successResetsLoginCounterOnly() {
        service.recordFailure(loginKey, ipKey);
        service.recordFailure(loginKey, ipKey);
        service.recordSuccess(loginKey);
        service.recordFailure(loginKey, ipKey);
        service.recordFailure(loginKey, ipKey);

        // 2 echecs apres la reussite : sous le seuil de 3 pour le couple
        assertThat(service.retryAfterSeconds(loginKey, ipKey)).isZero();
        // mais l'adresse IP a cumule 4 echecs, un de plus la bloque
        service.recordFailure("login:1.2.3.4:other", ipKey);
        assertThat(service.retryAfterSeconds("login:1.2.3.4:other", ipKey)).isPositive();
    }

    @Test
    void ipCounterBlocksAcrossDifferentLogins() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure(LoginAttemptService.loginKey("1.2.3.4", "user" + i), ipKey);
        }

        // un identifiant jamais essaye est quand meme bloque depuis cette adresse
        assertThat(service.retryAfterSeconds(LoginAttemptService.loginKey("1.2.3.4", "fresh"), ipKey)).isPositive();
    }

    @Test
    void anotherIpIsNotAffected() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(loginKey, ipKey);
        }

        String otherLogin = LoginAttemptService.loginKey("5.6.7.8", "Alice");
        String otherIp = LoginAttemptService.ipKey("5.6.7.8");
        assertThat(service.retryAfterSeconds(otherLogin, otherIp)).isZero();
    }

    @Test
    void windowExpiresSoOldFailuresDoNotAccumulate() {
        service.recordFailure(loginKey, ipKey);
        service.recordFailure(loginKey, ipKey);
        clock.advance(Duration.ofSeconds(601));
        service.recordFailure(loginKey, ipKey);

        assertThat(service.retryAfterSeconds(loginKey, ipKey)).isZero();
    }
}
