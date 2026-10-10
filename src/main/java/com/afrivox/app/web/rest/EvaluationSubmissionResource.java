package com.afrivox.app.web.rest;

import com.afrivox.app.service.EvaluationSubmissionService;
import com.afrivox.app.service.EvaluationSubmissionService.Submission;
import com.afrivox.app.service.EvaluationSubmissionService.TestSetView;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Classement par predictions. Lecture publique des jeux de test et du modele de CSV ({@code /api/public/**}) ; l'envoi
 * est reserve au proprietaire du modele (role developpeur ou admin, voir SecurityConfiguration).
 */
@RestController
@RequestMapping("/api")
public class EvaluationSubmissionResource {

    private final EvaluationSubmissionService service;

    public EvaluationSubmissionResource(EvaluationSubmissionService service) {
        this.service = service;
    }

    @GetMapping("/public/test-sets")
    public List<TestSetView> testSets(@RequestParam(required = false) String language) {
        return service.testSets(language);
    }

    @GetMapping("/public/test-sets/{versionId}/template.csv")
    public ResponseEntity<byte[]> template(@PathVariable Long versionId) {
        byte[] body = service.template(versionId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"predictions-template.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(body);
    }

    @PostMapping("/me/models/{modelId}/evaluations")
    public ResponseEntity<Submission> submit(
        @PathVariable Long modelId,
        @RequestParam("testSetId") Long testSetId,
        @RequestParam("file") MultipartFile file
    ) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestAlertException("Fichier illisible", "evaluation", "csvinvalid");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(modelId, testSetId, bytes));
    }
}
