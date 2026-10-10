package com.afrivox.app.web.rest;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.service.AdminDatasetOverviewService;
import com.afrivox.app.service.AdminDatasetOverviewService.DatasetRow;
import com.afrivox.app.service.DatasetPackagingService;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestion des datasets et de leurs versions ({@code /api/admin/**} est reserve a ROLE_ADMIN, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/admin/datasets")
public class AdminDatasetResource {

    private static final int MAX_PAGE_SIZE = 100;

    /** Le numero de version sert dans un nom de fichier : lettres, chiffres, point, tiret seulement. */
    public record PublishRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,19}$") String label,
        @NotNull LicenseKind license,
        @Min(0) Integer pricePerHourFcfa,
        /** Taille maximale de la version (plan Starter = echantillon) ; vide = tous les enregistrements eligibles. */
        @Min(1) Integer maxRecordings
    ) {}

    public record PublishedVersion(Long id, String label, Float hours, Integer speakers, String sha256) {}

    public record StatusRequest(@NotNull VersionStatus status) {}

    public record VersionStatusResponse(Long id, VersionStatus status) {}

    private final DatasetPackagingService packagingService;
    private final AdminDatasetOverviewService overviewService;

    public AdminDatasetResource(DatasetPackagingService packagingService, AdminDatasetOverviewService overviewService) {
        this.packagingService = packagingService;
        this.overviewService = overviewService;
    }

    /**
     * Liste paginée avec versions et compteurs. Filtres cumulables : {@code q} (titre ou slug), {@code languageId},
     * {@code plan}, {@code domain}, {@code published} (au moins une version publiée, ou aucune).
     */
    @GetMapping
    public ResponseEntity<List<DatasetRow>> list(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "languageId", required = false) Long languageId,
        @RequestParam(name = "plan", required = false) DatasetPlan plan,
        @RequestParam(name = "domain", required = false) String domain,
        @RequestParam(name = "published", required = false) Boolean published,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) int size
    ) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("title").ascending().and(Sort.by("id")));
        Page<DatasetRow> result = overviewService.search(query, languageId, plan, domain, published, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }

    /** Domaines existants, pour alimenter le filtre. */
    @GetMapping("/domains")
    public List<String> domains() {
        return overviewService.domains();
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishedVersion publish(@PathVariable Long id, @Valid @RequestBody PublishRequest request) {
        DatasetVersion v = packagingService.publish(
            id,
            request.label(),
            request.license(),
            request.pricePerHourFcfa(),
            request.maxRecordings()
        );
        return new PublishedVersion(v.getId(), v.getLabel(), v.getHours(), v.getSpeakers(), v.getSha256());
    }

    /**
     * Retire (WITHDRAWN) ou remet en ligne (PUBLISHED) une version. Le contenu d'une version ne change jamais :
     * seul son état de visibilité bouge. Une version retirée sort du catalogue et ne délivre plus de liens.
     */
    @PutMapping("/versions/{versionId}/status")
    public VersionStatusResponse setStatus(@PathVariable Long versionId, @Valid @RequestBody StatusRequest request) {
        if (request.status() == VersionStatus.DRAFT) {
            throw new BadRequestAlertException("Etat non autorise", "datasetVersion", "invalidstatus");
        }
        return overviewService
            .setVersionStatus(versionId, request.status())
            .map(v -> new VersionStatusResponse(v.getId(), v.getStatus()))
            .orElseThrow(() -> new BadRequestAlertException("Version introuvable", "datasetVersion", "notfound"));
    }
}
