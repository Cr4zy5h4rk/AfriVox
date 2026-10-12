package com.afrivox.app.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.enumeration.ApiKeyScope;
import com.afrivox.app.service.DeveloperApiKeyService;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Le filtre branche une clé d'API sur la sécurité : 401, 403, 429 ou authentification selon le cas. */
class ApiKeyAuthenticationFilterTest {

    private static final String KEY = "afv_Abcd1234_" + "x".repeat(43);

    private DeveloperApiKeyService keys;
    private ApiKeyAuthenticationFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        keys = mock(DeveloperApiKeyService.class);
        filter = new ApiKeyAuthenticationFilter(keys);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        when(keys.allowRequest(any())).thenReturn(true);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void keyIsValid(Set<ApiKeyScope> scopes) {
        when(keys.authenticate(KEY)).thenReturn(
            Optional.of(new DeveloperApiKeyService.Authenticated(9L, "dev_amadou", scopes, Set.of("ROLE_DEVELOPER")))
        );
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private void run(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, response, chain);
    }

    private boolean chainWasCalled() {
        return chain.getRequest() != null;
    }

    @Test
    void requestWithoutKeyPassesUntouched() throws Exception {
        run(request("GET", "/api/me/purchases"));

        assertThat(chainWasCalled()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(keys, never()).authenticate(anyString());
    }

    @Test
    void bearerTokenIsNotTreatedAsAnApiKey() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/me/purchases");
        request.addHeader("Authorization", "Bearer eyJhbGciOi.payload.signature");

        run(request);

        assertThat(chainWasCalled()).isTrue();
        verify(keys, never()).authenticate(anyString());
    }

    @Test
    void validKeyInHeaderAuthenticatesTheOwner() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.DOWNLOAD));
        MockHttpServletRequest request = request("GET", "/api/me/purchases");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(chainWasCalled()).isTrue();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("dev_amadou");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_DEVELOPER");
    }

    @Test
    void authorizationApiKeySchemeIsAccepted() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.DOWNLOAD));
        MockHttpServletRequest request = request("POST", "/api/purchases/3/download-link");
        request.addHeader("Authorization", "ApiKey " + KEY);

        run(request);

        assertThat(chainWasCalled()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("dev_amadou");
    }

    @Test
    void invalidKeyGetsUnauthorizedWithTheSameMessageWhateverTheReason() throws Exception {
        when(keys.authenticate(KEY)).thenReturn(Optional.empty());
        MockHttpServletRequest request = request("GET", "/api/me/purchases");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).startsWith("ApiKey");
        assertThat(response.getContentAsString()).contains("invalide, revoquee ou expiree");
        assertThat(chainWasCalled()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void publicReadsIgnoreTheKeyEvenIfItIsInvalid() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/public/datasets");
        request.addHeader("X-API-Key", "n-importe-quoi");

        run(request);

        assertThat(chainWasCalled()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(keys, never()).authenticate(anyString());
    }

    @Test
    void routesOutsideTheWhitelistAreForbiddenForAKey() throws Exception {
        keyIsValid(EnumSet.allOf(ApiKeyScope.class));
        for (String[] call : new String[][] {
            { "POST", "/api/me/purchases" },
            { "POST", "/api/me/api-keys" },
            { "GET", "/api/admin/users" },
            { "GET", "/api/account" },
        }) {
            response = new MockHttpServletResponse();
            chain = new MockFilterChain();
            MockHttpServletRequest request = request(call[0], call[1]);
            request.addHeader("X-API-Key", KEY);

            run(request);

            assertThat(response.getStatus()).as("%s %s", call[0], call[1]).isEqualTo(403);
            assertThat(chainWasCalled()).as("%s %s", call[0], call[1]).isFalse();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test
    void keyWithoutTheRightScopeIsForbidden() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.DOWNLOAD));
        MockHttpServletRequest request = request("POST", "/api/me/models");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chainWasCalled()).isFalse();
    }

    @Test
    void oneMatchingScopeIsEnough() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.PUBLISH));
        MockHttpServletRequest request = request("GET", "/api/me/models");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(chainWasCalled()).isTrue();
    }

    @Test
    void tooManyRequestsGetTooManyRequests() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.DOWNLOAD));
        when(keys.allowRequest(eq(9L))).thenReturn(false);
        MockHttpServletRequest request = request("GET", "/api/me/purchases");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(chainWasCalled()).isFalse();
    }

    @Test
    void contextPathIsIgnoredWhenMatchingRoutes() throws Exception {
        keyIsValid(EnumSet.of(ApiKeyScope.DOWNLOAD));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/afrivox/api/me/purchases");
        request.setContextPath("/afrivox");
        request.addHeader("X-API-Key", KEY);

        run(request);

        assertThat(chainWasCalled()).isTrue();
    }
}
