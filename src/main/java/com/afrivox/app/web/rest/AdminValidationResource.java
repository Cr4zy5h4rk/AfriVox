package com.afrivox.app.web.rest;

import com.afrivox.app.service.ValidationService;
import com.afrivox.app.service.ValidationService.PendingQualification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Demandes de qualification des validateurs ({@code /api/admin/**} est reserve a ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/validation")
public class AdminValidationResource {

    public record Decision(@NotNull Boolean approve) {}

    private final ValidationService service;

    public AdminValidationResource(ValidationService service) {
        this.service = service;
    }

    @GetMapping("/qualifications")
    public List<PendingQualification> pending() {
        return service.pendingQualifications();
    }

    @PostMapping("/qualifications/{id}/decision")
    public void decide(@PathVariable Long id, @Valid @RequestBody Decision decision) {
        service.decide(id, decision.approve());
    }
}
