package com.afrivox.app.web.rest;

import com.afrivox.app.config.ApplicationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Version du consentement en vigueur. Le frontend l'affiche et la renvoie a l'inscription ({@code consentVersion}) ;
 * le serveur refuse une version differente (voir {@link AccountResource#registerAccount}).
 */
@RestController
@RequestMapping("/api/consent")
public class ConsentResource {

    private final ApplicationProperties applicationProperties;

    public ConsentResource(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /** Reponse de {@code GET /api/consent/current}. */
    public record CurrentConsent(String version) {}

    /**
     * {@code GET /api/consent/current} : version du consentement a accepter. Public : necessaire avant l'inscription.
     */
    @GetMapping("/current")
    public CurrentConsent getCurrentConsent() {
        return new CurrentConsent(applicationProperties.getConsent().getCurrentVersion());
    }
}
