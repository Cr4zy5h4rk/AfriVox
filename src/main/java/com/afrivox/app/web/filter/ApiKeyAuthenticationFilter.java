package com.afrivox.app.web.filter;

import com.afrivox.app.domain.enumeration.ApiKeyScope;
import com.afrivox.app.service.DeveloperApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentifie les requêtes qui portent une clé d'API : en-tête {@code X-API-Key: afv_…} ou
 * {@code Authorization: ApiKey afv_…}. Ce n'est pas le schéma « Bearer » : le jeton JWT des navigateurs n'est donc
 * jamais confondu avec une clé.
 *
 * <p>Ordre des contrôles : clé valide (401 sinon, toujours le même message), appel autorisé pour une clé et portée
 * suffisante (403), débit (429). Les lectures publiques ignorent la clé. Ce filtre n'est pas déclaré comme bean :
 * il n'est installé que dans la chaîne de sécurité (voir {@code SecurityConfiguration}).
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";
    private static final String SCHEME = "ApiKey ";

    private final DeveloperApiKeyService keys;

    public ApiKeyAuthenticationFilter(DeveloperApiKeyService keys) {
        this.keys = keys;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String presented = extract(request);
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (presented == null || ApiKeyAccess.isPublic(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<DeveloperApiKeyService.Authenticated> auth = keys.authenticate(presented);
        if (auth.isEmpty()) {
            response.setHeader("WWW-Authenticate", "ApiKey realm=\"afrivox\"");
            problem(response, HttpServletResponse.SC_UNAUTHORIZED, "Cle d'API invalide, revoquee ou expiree");
            return;
        }
        DeveloperApiKeyService.Authenticated key = auth.orElseThrow();

        Optional<Set<ApiKeyScope>> allowed = ApiKeyAccess.allowedScopes(method, path);
        if (allowed.isEmpty() || Collections.disjoint(allowed.orElseThrow(), key.scopes())) {
            problem(response, HttpServletResponse.SC_FORBIDDEN, "Cette cle n'a pas le droit d'appeler cette ressource");
            return;
        }
        if (!keys.allowRequest(key.keyId())) {
            response.setHeader("Retry-After", "60");
            problem(response, 429, "Trop de requetes pour cette cle");
            return;
        }

        List<GrantedAuthority> authorities = key.authorities().stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(key.login(), null, authorities)
        );
        chain.doFilter(request, response);
    }

    private static String extract(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            String value = authorization.substring(SCHEME.length()).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private static void problem(HttpServletResponse response, int status, String title) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title.replace("\"", "'") + "\",\"status\":" + status + "}");
    }
}
