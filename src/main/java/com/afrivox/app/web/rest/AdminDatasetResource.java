package com.afrivox.app.web.rest;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.service.DatasetPackagingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Publication d'une version de dataset ({@code /api/admin/**} est reserve a ROLE_ADMIN, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/admin/datasets")
public class AdminDatasetResource {

    /** Le numero de version sert dans un nom de fichier : lettres, chiffres, point, tiret seulement. */
    public record PublishRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,19}$") String label,
        @NotNull LicenseKind license,
        @Min(0) Integer pricePerHourFcfa
    ) {}

    public record PublishedVersion(Long id, String label, Float hours, Integer speakers, String sha256) {}

    private final DatasetPackagingService packagingService;

    public AdminDatasetResource(DatasetPackagingService packagingService) {
        this.packagingService = packagingService;
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishedVersion publish(@PathVariable Long id, @Valid @RequestBody PublishRequest request) {
        DatasetVersion v = packagingService.publish(id, request.label(), request.license(), request.pricePerHourFcfa());
        return new PublishedVersion(v.getId(), v.getLabel(), v.getHours(), v.getSpeakers(), v.getSha256());
    }
}
