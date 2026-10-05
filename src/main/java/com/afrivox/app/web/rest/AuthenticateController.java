package com.afrivox.app.web.rest;

import static com.afrivox.app.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.afrivox.app.security.SecurityUtils.JWT_ALGORITHM;
import static com.afrivox.app.security.SecurityUtils.USER_ID_CLAIM;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.security.DomainUserDetailsService.UserWithId;
import com.afrivox.app.service.InvalidRefreshTokenException;
import com.afrivox.app.service.LoginAttemptService;
import com.afrivox.app.service.RefreshTokenService;
import com.afrivox.app.service.RefreshTokenService.IssuedToken;
import com.afrivox.app.service.RefreshTokenService.RotatedToken;
import com.afrivox.app.web.rest.vm.LoginVM;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

/**
 * Authentification : jeton d'acces court (JWT, en memoire cote client) + jeton de rafraichissement
 * opaque dans un cookie HttpOnly, rotatif et revocable.
 * <p>
 * Le cookie est limite au chemin {@code /api/authenticate} et {@code SameSite=Strict} : le navigateur ne
 * l'envoie jamais depuis un autre site, ce qui protege /refresh et /logout contre le CSRF sans jeton supplementaire.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

    static final String REFRESH_COOKIE = "refresh_token";

    static final String REFRESH_COOKIE_PATH = "/api/authenticate";

    private final JwtEncoder jwtEncoder;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final RefreshTokenService refreshTokenService;

    private final LoginAttemptService loginAttemptService;

    private final ApplicationProperties applicationProperties;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        RefreshTokenService refreshTokenService,
        LoginAttemptService loginAttemptService,
        ApplicationProperties applicationProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.applicationProperties = applicationProperties;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String loginKey = LoginAttemptService.loginKey(ip, loginVM.getUsername());
        String ipKey = LoginAttemptService.ipKey(ip);

        long retryAfter = loginAttemptService.retryAfterSeconds(loginKey, ipKey);
        if (retryAfter > 0) {
            LOG.warn("Login temporarily blocked for {}, retry in {}s", ip, retryAfter);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter)).build();
        }

        var authenticationToken = new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());
        Authentication authentication;
        try {
            authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(loginKey, ipKey);
            throw e;
        }
        loginAttemptService.recordSuccess(loginKey);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = createAccessToken(
            authentication.getName(),
            authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()),
            authentication.getPrincipal() instanceof UserWithId user ? user.getId() : null
        );
        if (!(authentication.getPrincipal() instanceof UserWithId principal) || principal.getId() == null) {
            throw new IllegalStateException("Authenticated principal has no user id");
        }
        IssuedToken refresh = refreshTokenService.issue(principal.getId(), loginVM.isRememberMe());
        return tokenResponse(jwt, refresh);
    }

    /**
     * {@code POST /authenticate/refresh} : echange le cookie de rafraichissement contre un nouveau jeton d'acces
     * (et un nouveau cookie). {@code 401} si le cookie est absent, invalide, expire ou rejoue.
     */
    @PostMapping("/authenticate/refresh")
    public ResponseEntity<JWTToken> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return unauthorizedClearingCookie();
        }
        RotatedToken rotated;
        try {
            rotated = refreshTokenService.rotate(refreshToken);
        } catch (InvalidRefreshTokenException e) {
            LOG.debug("Refresh refused: {}", e.getMessage());
            return unauthorizedClearingCookie();
        }
        String jwt = createAccessToken(rotated.login(), rotated.authorities(), rotated.userId());
        return tokenResponse(jwt, rotated.token());
    }

    /** {@code POST /authenticate/logout} : revoque la session (toute la famille de jetons) et efface le cookie. */
    @PostMapping("/authenticate/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearedCookie().toString()).build();
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)},
     * or with status {@code 401 (Unauthorized)} if not authenticated.
     */
    @GetMapping("/authenticate")
    public ResponseEntity<Void> isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.status(principal == null ? HttpStatus.UNAUTHORIZED : HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<JWTToken> tokenResponse(String jwt, IssuedToken refresh) {
        var httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);
        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshCookie(refresh).toString());
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    private ResponseEntity<JWTToken> unauthorizedClearingCookie() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.SET_COOKIE, clearedCookie().toString()).build();
    }

    private ResponseCookie refreshCookie(IssuedToken token) {
        return baseCookie(token.value()).maxAge(Duration.ofSeconds(token.maxAgeSeconds())).build();
    }

    private ResponseCookie clearedCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true)
            .secure(applicationProperties.getSecurity().isRefreshCookieSecure())
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH);
    }

    private String createAccessToken(String subject, Collection<String> authorities, Long userId) {
        var now = Instant.now();
        Instant validity = now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);

        // @formatter:off
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(subject)
            .claim(AUTHORITIES_CLAIM, String.join(" ", authorities));
        // @formatter:on
        if (userId != null) {
            builder.claim(USER_ID_CLAIM, userId);
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
