package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.afrivox.app.service.AdminRequestOverviewService;
import com.afrivox.app.service.AdminRequestOverviewService.RequestRow;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Liste des demandes d'organisation avec filtres ({@code /api/admin/**} est réservé à ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/requests")
public class AdminRequestResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminRequestOverviewService overviewService;

    public AdminRequestResource(AdminRequestOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /** Filtres cumulables : {@code q} (organisation), {@code status}, {@code sector}, {@code languageId}, {@code usageKind}. */
    @GetMapping
    public ResponseEntity<List<RequestRow>> list(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "status", required = false) RequestStatus status,
        @RequestParam(name = "sector", required = false) Sector sector,
        @RequestParam(name = "languageId", required = false) Long languageId,
        @RequestParam(name = "usageKind", required = false) UsageKind usageKind,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var result = overviewService.search(query, status, sector, languageId, usageKind, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }
}
