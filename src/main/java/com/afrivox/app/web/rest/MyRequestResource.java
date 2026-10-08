package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.afrivox.app.service.OrganizationRequestService;
import com.afrivox.app.service.OrganizationRequestService.NewRequest;
import com.afrivox.app.service.OrganizationRequestService.RequestView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demandes de dataset de l'organisation connectée (rôle organisation ou admin, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/me/requests")
public class MyRequestResource {

    /** Corps de {@code POST /api/me/requests} : ni statut, ni devis, ni demandeur (fixés par le serveur). */
    public record SubmitRequest(
        @NotBlank @Size(max = 120) String orgName,
        @NotNull Sector sector,
        @NotBlank @Size(max = 10) String languageCode,
        @NotNull @Min(1) @Max(100000) Integer hours,
        @Min(1) @Max(1000000) Integer speakersTarget,
        @Size(max = 120) String conversationType,
        @NotNull UsageKind usageKind,
        LocalDate deadline,
        @Size(max = 1500) String description
    ) {}

    private final OrganizationRequestService service;

    public MyRequestResource(OrganizationRequestService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<RequestView> mine() {
        return service.mine();
    }

    @PostMapping("")
    public ResponseEntity<RequestView> submit(@Valid @RequestBody SubmitRequest body) {
        NewRequest input = new NewRequest(
            body.orgName(),
            body.sector(),
            body.languageCode(),
            body.hours(),
            body.speakersTarget(),
            body.conversationType(),
            body.usageKind(),
            body.deadline(),
            body.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(input));
    }
}
