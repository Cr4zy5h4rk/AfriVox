package com.afrivox.app.web.rest;

import com.afrivox.app.service.ContributorTrialService;
import com.afrivox.app.service.ContributorTrialService.TrialView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ma periode d'essai : etat, plafond et ce qui est deja envoye ou releve. */
@RestController
@RequestMapping("/api/me/contributor")
public class MyContributorResource {

    private final ContributorTrialService trialService;

    public MyContributorResource(ContributorTrialService trialService) {
        this.trialService = trialService;
    }

    /** {@code GET /api/me/contributor} */
    @GetMapping("")
    public TrialView get() {
        return trialService.mine();
    }
}
