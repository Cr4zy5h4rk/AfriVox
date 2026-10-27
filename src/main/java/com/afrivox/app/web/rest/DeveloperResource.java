package com.afrivox.app.web.rest;

import com.afrivox.app.service.ContactService;
import com.afrivox.app.service.ContactService.ReceivedView;
import com.afrivox.app.service.ContactService.SentView;
import com.afrivox.app.service.DeveloperProfileService;
import com.afrivox.app.service.DeveloperProfileService.DeveloperProfile;
import com.afrivox.app.service.DeveloperProfileService.DeveloperSummary;
import com.afrivox.app.service.DeveloperProfileService.Settings;
import com.afrivox.app.service.PublicCatalogService.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Annuaire et profils des developpeurs, reglages de visibilite et prises de contact.
 * <ul>
 *   <li>lecture publique : {@code /api/public/developers} (profils rendus publics seulement) ;</li>
 *   <li>reglages : {@code /api/me/developer-profile} (developpeurs et admin) ;</li>
 *   <li>contact : {@code /api/community/**} ; seule une organisation envoie (voir SecurityConfiguration), tout compte
 *       connecte lit les demandes qui le concernent.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class DeveloperResource {

    public record SettingsRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @Size(max = 300) String bio,
        @Size(max = 300) String profileUrl,
        boolean publicProfile,
        boolean openToWork
    ) {}

    public record ContactBody(@NotBlank @Size(max = 1000) String message, @NotBlank @Size(max = 120) String contact) {}

    private final DeveloperProfileService profiles;
    private final ContactService contacts;

    public DeveloperResource(DeveloperProfileService profiles, ContactService contacts) {
        this.profiles = profiles;
        this.contacts = contacts;
    }

    @GetMapping("/public/developers")
    public PageView<DeveloperSummary> search(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "false") boolean openToWork,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return profiles.search(q, openToWork, page, size);
    }

    @GetMapping("/public/developers/{id}")
    public ResponseEntity<DeveloperProfile> get(@PathVariable Long id) {
        return profiles.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/developer-profile")
    public Settings settings() {
        return profiles.settings();
    }

    @PutMapping("/me/developer-profile")
    public Settings update(@Valid @RequestBody SettingsRequest request) {
        return profiles.update(
            new Settings(null, request.displayName(), request.bio(), request.profileUrl(), request.publicProfile(), request.openToWork())
        );
    }

    // ---- Contact ----

    @PostMapping("/community/developers/{id}/contact")
    public ResponseEntity<SentView> contact(@PathVariable Long id, @Valid @RequestBody ContactBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contacts.send(id, body.message(), body.contact()));
    }

    @GetMapping("/community/contact-requests/received")
    public List<ReceivedView> received() {
        return contacts.received();
    }

    @GetMapping("/community/contact-requests/sent")
    public List<SentView> sent() {
        return contacts.sent();
    }

    @PostMapping("/community/contact-requests/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        return contacts.answer(id, true) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/community/contact-requests/{id}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long id) {
        return contacts.answer(id, false) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
