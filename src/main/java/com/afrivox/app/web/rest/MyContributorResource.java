package com.afrivox.app.web.rest;

import com.afrivox.app.service.ContributorProfileService;
import com.afrivox.app.service.ContributorProfileService.ProfileInput;
import com.afrivox.app.service.ContributorProfileService.ProfileView;
import com.afrivox.app.service.ContributorTrialService;
import com.afrivox.app.service.ContributorTrialService.TrialView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mon espace contributeur : periode d'essai (etat, plafond) et informations declarees. */
@RestController
@RequestMapping("/api/me/contributor")
public class MyContributorResource {

    private final ContributorTrialService trialService;

    private final ContributorProfileService profileService;

    public MyContributorResource(ContributorTrialService trialService, ContributorProfileService profileService) {
        this.trialService = trialService;
        this.profileService = profileService;
    }

    /** {@code GET /api/me/contributor} */
    @GetMapping("")
    public TrialView get() {
        return trialService.mine();
    }

    /** {@code GET /api/me/contributor/profile} : ce que j'ai declare (age, genre, pays...). */
    @GetMapping("/profile")
    public ProfileView profile() {
        return profileService.mine();
    }

    /** {@code PUT /api/me/contributor/profile} : tout est facultatif ; une reponse vide compte comme « passer ». */
    @PutMapping("/profile")
    public ProfileView saveProfile(@Valid @RequestBody ProfileInput input) {
        return profileService.save(input);
    }
}
