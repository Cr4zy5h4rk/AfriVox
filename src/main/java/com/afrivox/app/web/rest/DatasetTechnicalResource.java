package com.afrivox.app.web.rest;

import com.afrivox.app.service.DatasetTechnicalService;
import com.afrivox.app.service.DatasetTechnicalService.PreviewRow;
import com.afrivox.app.service.DatasetTechnicalService.Technical;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vue technique d'une version publiée. Les statistiques sont publiques (comme le catalogue) ; l'aperçu des lignes
 * (texte et durées) exige un compte connecté : {@code /api/**} est authentifié par défaut (SecurityConfiguration).
 */
@RestController
public class DatasetTechnicalResource {

    private final DatasetTechnicalService technicalService;

    public DatasetTechnicalResource(DatasetTechnicalService technicalService) {
        this.technicalService = technicalService;
    }

    /** {@code version} absent = dernière version publiée. 404 si aucune version publiée ou archive absente. */
    @GetMapping("/api/public/datasets/{slug}/technical")
    public ResponseEntity<Technical> technical(@PathVariable String slug, @RequestParam(required = false) String version) {
        return technicalService
            .technical(slug, version)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Premières lignes du manifeste, sans audio. */
    @GetMapping("/api/catalog/datasets/{slug}/preview")
    public ResponseEntity<List<PreviewRow>> preview(@PathVariable String slug, @RequestParam(required = false) String version) {
        return technicalService
            .preview(slug, version)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
