package com.afrivox.app.web.rest;

import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.service.DatasetTechnicalService;
import com.afrivox.app.service.DatasetTechnicalService.PreviewRow;
import com.afrivox.app.service.DatasetTechnicalService.Technical;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vue technique d'une version publiée. Les statistiques sont publiques (comme le catalogue) ; l'aperçu des lignes
 * (texte et durées) et les clips audio de l'échantillon exigent un compte connecté : {@code /api/**} est
 * authentifié par défaut (SecurityConfiguration).
 */
@RestController
public class DatasetTechnicalResource {

    /** Clips servis par minute et par compte : assez pour écouter l'échantillon, trop peu pour le moissonner. */
    private static final int MAX_CLIPS_PER_MINUTE = 30;

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Horodatages (ms) des clips récents par compte ; en mémoire, remis à zéro au redémarrage. */
    private final Map<String, Deque<Long>> recentClips = new ConcurrentHashMap<>();

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

    /** Premières lignes du manifeste ; {@code audio} indique les lignes dont un clip est écoutable. */
    @GetMapping("/api/catalog/datasets/{slug}/preview")
    public ResponseEntity<List<PreviewRow>> preview(@PathVariable String slug, @RequestParam(required = false) String version) {
        return technicalService
            .preview(slug, version)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Clip audio d'une ligne de l'échantillon. 404 si l'interrupteur du dataset est coupé ou si la ligne n'est pas
     * dans l'échantillon (même réponse : on ne révèle rien) ; 429 au-delà de {@value #MAX_CLIPS_PER_MINUTE} par minute.
     */
    @GetMapping("/api/catalog/datasets/{slug}/preview/{id}/audio")
    public ResponseEntity<byte[]> previewAudio(
        @PathVariable String slug,
        @PathVariable long id,
        @RequestParam(required = false) String version
    ) {
        String who = SecurityUtils.getCurrentUserLogin().orElse("anonymous");
        if (!allow(who)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "60").build();
        }
        return technicalService
            .previewAudio(slug, version, id)
            .map(clip ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(clip.contentType()))
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(clip.data())
            )
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Fenêtre glissante d'une minute par compte. */
    private boolean allow(String who) {
        long now = System.currentTimeMillis();
        if (recentClips.size() > 10_000) {
            // Borne la mémoire : on oublie les comptes sans clip récent.
            recentClips.values().removeIf(d -> d.isEmpty() || now - d.peekLast() > WINDOW.toMillis());
        }
        Deque<Long> hits = recentClips.computeIfAbsent(who, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW.toMillis()) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_CLIPS_PER_MINUTE) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }
}
