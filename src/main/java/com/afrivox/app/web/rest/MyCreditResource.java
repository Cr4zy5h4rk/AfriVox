package com.afrivox.app.web.rest;

import com.afrivox.app.service.CreditService;
import com.afrivox.app.service.CreditService.Summary;
import com.afrivox.app.service.CreditService.TransactionView;
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

/** Mes credits : solde et historique du compte connecte (jamais ceux d'un autre). */
@RestController
@RequestMapping("/api/me/credits")
public class MyCreditResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final CreditService creditService;

    public MyCreditResource(CreditService creditService) {
        this.creditService = creditService;
    }

    /** {@code GET /api/me/credits} : solde, gains par source et tarifs globaux en vigueur. */
    @GetMapping
    public Summary summary() {
        return creditService.mine();
    }

    /** {@code GET /api/me/credits/history} : mouvements, les plus recents d'abord ; total dans {@code X-Total-Count}. */
    @GetMapping("/history")
    public ResponseEntity<List<TransactionView>> history(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var result = creditService.myHistory(pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }
}
