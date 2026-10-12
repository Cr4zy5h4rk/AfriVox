package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.CreditRateKind;
import com.afrivox.app.service.CreditService;
import com.afrivox.app.service.CreditService.CampaignRates;
import com.afrivox.app.service.CreditService.RateChangeView;
import com.afrivox.app.service.CreditService.RateView;
import com.afrivox.app.service.CreditService.TransactionView;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

/** Tarifs de credits, ajustements et journal des changements ({@code /api/admin/**} est reserve a ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin/credits")
public class AdminCreditResource {

    private static final int MAX_PAGE_SIZE = 100;

    /** Tarifs propres a une campagne : une valeur absente ou nulle ramene au tarif global. */
    public record CampaignRatesInput(Integer validatedRecording, Integer reviewAgreed, Integer trapCorrect) {}

    public record AdjustInput(String login, int amount, String note) {}

    private final CreditService creditService;

    public AdminCreditResource(CreditService creditService) {
        this.creditService = creditService;
    }

    /** {@code GET /api/admin/credits/rates} : les trois tarifs globaux. */
    @GetMapping("/rates")
    public List<RateView> rates() {
        return creditService.rates();
    }

    /** {@code PUT /api/admin/credits/rates} : corps {@code {"VALIDATED_RECORDING": 10, ...}} ; renvoie les tarifs a jour. */
    @PutMapping("/rates")
    public List<RateView> updateRates(@RequestBody Map<CreditRateKind, Integer> amounts) {
        return creditService.updateRates(amounts);
    }

    /** {@code GET /api/admin/credits/rates/history} : qui a change quoi, les plus recents d'abord. */
    @GetMapping("/rates/history")
    public ResponseEntity<List<RateChangeView>> history(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "changedAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var result = creditService.history(pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(result.getTotalElements()));
        return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
    }

    /** {@code GET /api/admin/credits/campaigns/{id}} : surcharges de la campagne et tarifs effectifs. */
    @GetMapping("/campaigns/{id}")
    public CampaignRates campaignRates(@PathVariable("id") Long id) {
        return creditService.campaignRates(id);
    }

    /** {@code PUT /api/admin/credits/campaigns/{id}} : remplace les trois surcharges ({@code null} = tarif global). */
    @PutMapping("/campaigns/{id}")
    public CampaignRates setCampaignRates(@PathVariable("id") Long id, @RequestBody CampaignRatesInput input) {
        return creditService.setCampaignRates(id, input.validatedRecording(), input.reviewAgreed(), input.trapCorrect());
    }

    /** {@code POST /api/admin/credits/adjust} : correction manuelle (motif obligatoire, solde jamais negatif). */
    @PostMapping("/adjust")
    public TransactionView adjust(@RequestBody AdjustInput input) {
        return creditService.adjust(input.login(), input.amount(), input.note());
    }
}
