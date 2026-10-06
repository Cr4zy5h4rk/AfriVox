package com.afrivox.app.web.rest;

import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.service.RecordingUploadService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Enregistrements du contributeur connecte. Remplace, pour les utilisateurs, le CRUD genere
 * ({@code /api/recordings}, reserve a l'admin). Le chemin du fichier sur le serveur n'est jamais renvoye.
 */
@RestController
@RequestMapping("/api/me/recordings")
public class MyRecordingResource {

    /** Vue exposee au contributeur. */
    public record RecordingSummary(
        Long id,
        Long promptId,
        Integer durationMs,
        RecordingStatus status,
        QcStatus qcStatus,
        Instant createdAt
    ) {
        static RecordingSummary of(Recording r) {
            return new RecordingSummary(
                r.getId(),
                r.getPrompt() == null ? null : r.getPrompt().getId(),
                r.getDurationMs(),
                r.getStatus(),
                r.getQcStatus(),
                r.getCreatedAt()
            );
        }
    }

    private final RecordingUploadService uploadService;

    public MyRecordingResource(RecordingUploadService uploadService) {
        this.uploadService = uploadService;
    }

    /** {@code POST /api/me/recordings} (multipart) : {@code promptId}, {@code durationMs}, {@code file}. */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<RecordingSummary> upload(
        @RequestParam("promptId") Long promptId,
        @RequestParam("durationMs") int durationMs,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        Recording saved = uploadService.upload(promptId, durationMs, file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecordingSummary.of(saved));
    }

    /** {@code GET /api/me/recordings} : mes enregistrements, pagines. */
    @GetMapping("")
    public ResponseEntity<List<RecordingSummary>> getMine(Pageable pageable) {
        Page<RecordingSummary> page = uploadService.findMine(pageable).map(RecordingSummary::of);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
