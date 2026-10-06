package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.ConsentKind;
import com.afrivox.app.service.ConsentService;
import com.afrivox.app.service.dto.ConsentDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consentements de l'utilisateur connecte. Ecrit a la main, sans CRUD generique : un utilisateur ne voit et ne modifie
 * que les siens, et la version de la politique est decidee par le serveur. Ne pas confondre avec
 * {@link ConsentResource}, qui expose la version en vigueur a l'inscription.
 */
@RestController
@RequestMapping("/api/consents")
public class ConsentRecordResource {

    /** Corps de {@code POST /api/consents}. */
    public record GrantRequest(@NotNull ConsentKind kind) {}

    private final ConsentService consentService;

    public ConsentRecordResource(ConsentService consentService) {
        this.consentService = consentService;
    }

    /** {@code GET /api/consents} : mes consentements. */
    @GetMapping("")
    public List<ConsentDTO> getMyConsents() {
        return consentService.findMine();
    }

    /** {@code POST /api/consents} : j'accorde un consentement ; renvoie l'existant s'il est deja actif. */
    @PostMapping("")
    public ResponseEntity<ConsentDTO> grant(@Valid @RequestBody GrantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentService.grant(request.kind()));
    }

    /** {@code POST /api/consents/{id}/revoke} : je retire un de mes consentements (404 s'il n'est pas a moi). */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ConsentDTO> revoke(@PathVariable("id") Long id) {
        return consentService
            .revoke(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
