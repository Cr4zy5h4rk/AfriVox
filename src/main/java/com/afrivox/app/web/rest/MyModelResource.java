package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.service.ModelPublishingService;
import com.afrivox.app.service.ModelPublishingService.NewModel;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Modeles du developpeur connecte (role developpeur ou admin, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/me/models")
public class MyModelResource {

    /** Corps de {@code POST /api/me/models}. */
    public record ModelRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull MlTask task,
        @Size(max = 1000) String description,
        @Size(max = 60) String license,
        @Size(max = 300) String url,
        @Size(max = 10) String languageCode,
        Long trainedOnVersionId,
        @Size(max = 10) List<Long> otherVersionIds
    ) {}

    private final ModelPublishingService service;

    public MyModelResource(ModelPublishingService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<ModelSummary> mine() {
        return service.mine();
    }

    @PostMapping("")
    public ResponseEntity<ModelSummary> publish(@Valid @RequestBody ModelRequest request) {
        NewModel input = new NewModel(
            request.name(),
            request.task(),
            request.description(),
            request.license(),
            request.url(),
            request.languageCode(),
            request.trainedOnVersionId(),
            request.otherVersionIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(service.publish(input));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.deleteMine(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
