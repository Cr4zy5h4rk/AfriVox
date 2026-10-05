package com.afrivox.app.security.jwt;

import static org.mockito.Mockito.mock;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.service.RefreshTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Dependances de AuthenticateController pour les tests de securite sans base de donnees :
 * les proprietes par defaut et un service de jetons de rafraichissement factice.
 */
@TestConfiguration
public class AuthenticationTestBeans {

    @Bean
    ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }

    @Bean
    RefreshTokenService refreshTokenService() {
        return mock(RefreshTokenService.class);
    }
}
