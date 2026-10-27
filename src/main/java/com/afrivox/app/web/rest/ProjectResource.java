package com.afrivox.app.web.rest;

import com.afrivox.app.service.ProjectService;
import com.afrivox.app.service.ProjectService.ProjectInput;
import com.afrivox.app.service.ProjectService.ProjectView;
import com.afrivox.app.service.PublicCatalogService.PageView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Projets du hub. Lecture publique ({@code /api/public/projects}) ; publier, modifier et supprimer ({@code /api/me/projects})
 * sont reserves aux developpeurs et a l'admin (voir SecurityConfiguration), chacun ne gerant que les siens.
 */
@RestController
@RequestMapping("/api")
public class ProjectResource {

    /** Corps de {@code POST /api/me/projects} et de {@code PUT /api/me/projects/{id}}. */
    public record ProjectRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 1000) String summary,
        @Size(max = 300) String repoUrl,
        @Size(max = 300) String demoUrl,
        @Size(max = 10) String languageCode,
        @Size(max = 10) List<@Size(max = 60) String> tags,
        Long modelId
    ) {
        ProjectInput toInput() {
            return new ProjectInput(title, summary, repoUrl, demoUrl, languageCode, tags, modelId);
        }
    }

    private final ProjectService service;

    public ProjectResource(ProjectService service) {
        this.service = service;
    }

    @GetMapping("/public/projects")
    public PageView<ProjectView> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String tag,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return service.search(q, language, tag, page, size);
    }

    @GetMapping("/public/projects/{id}")
    public ResponseEntity<ProjectView> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/projects")
    public List<ProjectView> mine() {
        return service.mine();
    }

    @PostMapping("/me/projects")
    public ResponseEntity<ProjectView> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toInput()));
    }

    @PutMapping("/me/projects/{id}")
    public ResponseEntity<ProjectView> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return service.update(id, request.toInput()).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/me/projects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
