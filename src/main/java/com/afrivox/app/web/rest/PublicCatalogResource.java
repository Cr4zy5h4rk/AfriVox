package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.service.PublicCatalogService;
import com.afrivox.app.service.PublicCatalogService.DatasetDetail;
import com.afrivox.app.service.PublicCatalogService.DatasetSummary;
import com.afrivox.app.service.PublicCatalogService.EvaluationRow;
import com.afrivox.app.service.PublicCatalogService.LanguageView;
import com.afrivox.app.service.PublicCatalogService.ModelDetail;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import com.afrivox.app.service.PublicCatalogService.PageView;
import com.afrivox.app.service.PublicCatalogService.Stats;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogue public, sans connexion (comme un hub de modeles) : on peut parcourir avant de s'inscrire. Lecture seule ;
 * telechargements et ecritures passent par des endpoints authentifies. Voir {@link PublicCatalogService} pour ce
 * qui est expose.
 */
@RestController
@RequestMapping("/api/public")
public class PublicCatalogResource {

    private final PublicCatalogService catalog;

    public PublicCatalogResource(PublicCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/stats")
    public Stats stats() {
        return catalog.stats();
    }

    @GetMapping("/languages")
    public List<LanguageView> languages() {
        return catalog.languages();
    }

    @GetMapping("/datasets")
    public PageView<DatasetSummary> datasets(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) DatasetPlan plan,
        @RequestParam(required = false) String domain,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return catalog.searchDatasets(q, language, plan, domain, page, size);
    }

    @GetMapping("/datasets/{slug}")
    public ResponseEntity<DatasetDetail> dataset(@PathVariable String slug) {
        return catalog
            .dataset(slug)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/models")
    public PageView<ModelSummary> models(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) MlTask task,
        @RequestParam(required = false) String language,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return catalog.searchModels(q, task, language, page, size);
    }

    @GetMapping("/models/{id}")
    public ResponseEntity<ModelDetail> model(@PathVariable Long id) {
        return catalog
            .model(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/leaderboard")
    public List<EvaluationRow> leaderboard(@RequestParam(required = false) String language, @RequestParam(defaultValue = "50") int limit) {
        return catalog.leaderboard(language, limit);
    }
}
