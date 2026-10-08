package com.afrivox.app.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.afrivox.app.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/authenticate").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/consent/current").permitAll()
                    // Catalogue public en lecture seule (stats, langues, datasets, modèles, classement) : sans connexion.
                    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/authenticate/refresh").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/authenticate/logout").permitAll()
                    .requestMatchers("/api/register").permitAll()
                    .requestMatchers("/api/activate").permitAll()
                    .requestMatchers("/api/account/reset-password/init").permitAll()
                    .requestMatchers("/api/account/reset-password/finish").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    // Mes consentements : le service ne lit et ne modifie que ceux de l'utilisateur connecté.
                    .requestMatchers("/api/consents/**").authenticated()
                    // Envoi d'audio : contributeurs (et admin). Chaque utilisateur ne voit que ses enregistrements.
                    .requestMatchers("/api/me/recordings/**", "/api/me/campaigns/**").hasAnyAuthority(AuthoritiesConstants.CONTRIBUTOR, AuthoritiesConstants.ADMIN)
                    // Publication de modèles : développeurs (et admin). Chacun ne gère que les siens.
                    .requestMatchers("/api/me/models/**").hasAnyAuthority(AuthoritiesConstants.DEVELOPER, AuthoritiesConstants.ADMIN)
                    // Telechargement de datasets : developpeurs et organisations (et admin) demandent un lien signe.
                    .requestMatchers("/api/me/downloads/**").hasAnyAuthority(AuthoritiesConstants.DEVELOPER, AuthoritiesConstants.ORGANIZATION, AuthoritiesConstants.ADMIN)
                    // Demandes de dataset sur mesure : organisations (et admin). Chacune ne voit que les siennes.
                    .requestMatchers("/api/me/requests/**").hasAnyAuthority(AuthoritiesConstants.ORGANIZATION, AuthoritiesConstants.ADMIN)
                    // CRUD généré par JHipster : référentiels lisibles par tout utilisateur connecté, écriture réservée à l'admin.
                    .requestMatchers(HttpMethod.GET, "/api/languages/**", "/api/campaigns/**", "/api/prompts/**", "/api/datasets/**", "/api/ml-models/**", "/api/evaluations/**").authenticated()
                    // Tout le reste du CRUD généré (données personnelles, audio, crédits, clés d'API, chemins de fichiers) :
                    // admin seulement. Les usages réels (mes enregistrements, mes clés...) passeront par des endpoints
                    // dédiés qui filtrent par propriétaire ; refus par défaut en attendant.
                    .requestMatchers(
                        "/api/languages/**", "/api/campaigns/**", "/api/prompts/**", "/api/datasets/**", "/api/ml-models/**", "/api/evaluations/**",
                        "/api/dataset-versions/**", "/api/dataset-requests/**", "/api/download-logs/**", "/api/credit-transactions/**",
                        "/api/api-keys/**", "/api/recordings/**", "/api/recording-reviews/**", "/api/user-profiles/**",
                        "/api/language-qualifications/**"
                    ).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/**").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }
}
