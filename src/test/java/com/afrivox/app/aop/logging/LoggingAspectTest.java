package com.afrivox.app.aop.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class LoggingAspectTest {

    private static final String DECLARING_TYPE = "com.afrivox.app.service.LoggedService";
    private static final String METHOD = "loggedMethod";

    private final Environment env = mock(Environment.class);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final ListAppender appender = new ListAppender();
    private LoggingAspect aspect;
    private LoggerConfig loggerConfig;

    @BeforeEach
    void setup() {
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringTypeName()).thenReturn(DECLARING_TYPE);
        when(signature.getName()).thenReturn(METHOD);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg" });
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        // Appender Log4j2 en memoire rattache au logger teste, niveau DEBUG
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        loggerConfig = new LoggerConfig(DECLARING_TYPE, Level.DEBUG, false);
        loggerConfig.addAppender(appender, Level.DEBUG, null);
        config.addLogger(DECLARING_TYPE, loggerConfig);
        context.updateLoggers();

        aspect = new LoggingAspect(env);
    }

    @AfterEach
    void teardown() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().removeLogger(DECLARING_TYPE);
        loggerConfig.removeAppender(appender.getName());
        appender.stop();
        context.updateLoggers();
    }

    private List<String> messages() {
        return appender.events
            .stream()
            .map(e -> e.getMessage().getFormattedMessage())
            .toList();
    }

    @Test
    void logAroundLogsEnterAndExit() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logAround(joinPoint);

        assertThat(result).isEqualTo("result");
        assertThat(messages()).containsExactly(
            "Enter: loggedMethod() with argument[s] = [arg]",
            "Exit: loggedMethod() with result = result"
        );
    }

    @Test
    void logAroundRethrowsIllegalArgumentException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> aspect.logAround(joinPoint))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid");

        assertThat(messages()).contains("Illegal argument: [arg] in loggedMethod()");
    }

    @Test
    void logAfterThrowingLogsExceptionWithStackTraceInDevProfile() {
        aspect.logAfterThrowing(joinPoint, new IllegalStateException("failed"));

        assertThat(messages()).containsExactly("Exception in loggedMethod() with cause = 'NULL' and exception = 'failed'");
        assertThat(appender.events.get(0).getThrown()).isNotNull();
    }

    @Test
    void logAfterThrowingLogsCauseOnlyOutsideDevProfile() {
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        aspect.logAfterThrowing(joinPoint, new IllegalStateException("failed", new RuntimeException("root")));

        assertThat(messages()).containsExactly("Exception in loggedMethod() with cause = java.lang.RuntimeException: root");
        assertThat(appender.events.get(0).getThrown()).isNull();
    }

    /** Appender Log4j2 minimal qui garde les evenements en memoire pour les assertions. */
    private static final class ListAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        ListAppender() {
            super("LoggingAspectTestAppender", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
