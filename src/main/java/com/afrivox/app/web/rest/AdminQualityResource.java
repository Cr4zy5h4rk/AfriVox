package com.afrivox.app.web.rest;

import com.afrivox.app.service.QualityBackfillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contrôle qualité des enregistrements déjà présents : état et analyse par lots (admin, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/admin/quality")
public class AdminQualityResource {

    private final QualityBackfillService service;

    public AdminQualityResource(QualityBackfillService service) {
        this.service = service;
    }

    @GetMapping("")
    public QualityBackfillService.Summary summary() {
        return service.summary();
    }

    /** Analyse un lot de WAV sans note. À répéter tant que {@code remaining} est positif. */
    @PostMapping("/run")
    public QualityBackfillService.Result run(@RequestParam(defaultValue = "200") int limit) {
        return service.run(limit);
    }
}
