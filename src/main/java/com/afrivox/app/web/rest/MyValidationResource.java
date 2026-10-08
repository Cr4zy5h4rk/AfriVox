package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.Verdict;
import com.afrivox.app.service.ValidationService;
import com.afrivox.app.service.ValidationService.AudioFile;
import com.afrivox.app.service.ValidationService.QualificationView;
import com.afrivox.app.service.ValidationService.ReviewInput;
import com.afrivox.app.service.ValidationService.TaskView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Espace du validateur : ses qualifications, la prochaine tache, l'audio a ecouter et son avis. */
@RestController
@RequestMapping("/api/me/validation")
public class MyValidationResource {

    public record QualificationRequest(@NotBlank @Size(max = 12) String languageCode) {}

    public record ReviewRequest(
        @NotNull Long recordingId,
        @NotNull Verdict verdict,
        Boolean audioOk,
        Boolean pronunciationOk,
        Boolean transcriptOk,
        @Size(max = 300) String note
    ) {}

    private final ValidationService service;

    public MyValidationResource(ValidationService service) {
        this.service = service;
    }

    @GetMapping("/qualifications")
    public List<QualificationView> qualifications() {
        return service.myQualifications();
    }

    @PostMapping("/qualifications")
    @ResponseStatus(HttpStatus.CREATED)
    public QualificationView requestQualification(@Valid @RequestBody QualificationRequest request) {
        return service.requestQualification(request.languageCode());
    }

    /** 204 quand il n'y a plus rien a relire dans cette langue. */
    @GetMapping("/next")
    public ResponseEntity<TaskView> next(@RequestParam("language") String language) {
        return service
            .next(language)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/audio/{recordingId}")
    public ResponseEntity<Resource> audio(@PathVariable Long recordingId) {
        AudioFile audio = service.audio(recordingId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(audio.contentType()))
            .cacheControl(CacheControl.noStore())
            .body(new FileSystemResource(audio.file()));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody ReviewRequest request) {
        service.submit(
            new ReviewInput(
                request.recordingId(),
                request.verdict(),
                request.audioOk(),
                request.pronunciationOk(),
                request.transcriptOk(),
                request.note()
            )
        );
    }
}
