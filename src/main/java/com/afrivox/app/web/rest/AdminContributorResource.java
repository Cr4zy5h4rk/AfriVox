package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.ContributorStatus;
import com.afrivox.app.service.ContributorTrialService;
import com.afrivox.app.service.ContributorTrialService.ContributorRow;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contributeurs et periode d'essai ({@code /api/admin/**} est reserve a ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/contributors")
public class AdminContributorResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContributorTrialService trialService;

    public AdminContributorResource(ContributorTrialService trialService) {
        this.trialService = trialService;
    }

    /** Filtre facultatif {@code status} ; les plus recents d'abord ; total dans {@code X-Total-Count}. */
    @GetMapping
    public ResponseEntity<List<ContributorRow>> list(
        @RequestParam(name = "status", required = false) ContributorStatus status,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var result = trialService.list(status, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }

    /** {@code POST /api/admin/contributors/{id}/status?value=ACTIVE|SUSPENDED|TRIAL} */
    @PostMapping("/{id}/status")
    public ResponseEntity<Void> setStatus(@PathVariable("id") Long id, @RequestParam("value") ContributorStatus value) {
        trialService.setStatus(id, value);
        return ResponseEntity.noContent().build();
    }
}
