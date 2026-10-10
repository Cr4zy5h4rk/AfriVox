package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.service.AdminCampaignOverviewService;
import com.afrivox.app.service.AdminCampaignOverviewService.CampaignRow;
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

/** Liste des campagnes avec filtres et compteurs ({@code /api/admin/**} est réservé à ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminCampaignOverviewService overviewService;

    public AdminCampaignResource(AdminCampaignOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /** Filtres cumulables : {@code q} (titre), {@code languageId}, {@code kind}, {@code status}, {@code domain}. Récentes d'abord. */
    @GetMapping
    public ResponseEntity<List<CampaignRow>> list(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "languageId", required = false) Long languageId,
        @RequestParam(name = "kind", required = false) CampaignKind kind,
        @RequestParam(name = "status", required = false) CampaignStatus status,
        @RequestParam(name = "domain", required = false) String domain,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var result = overviewService.search(query, languageId, kind, status, domain, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/domains")
    public List<String> domains() {
        return overviewService.domains();
    }
}
