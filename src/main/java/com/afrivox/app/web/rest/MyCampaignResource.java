package com.afrivox.app.web.rest;

import com.afrivox.app.service.ContributionService;
import com.afrivox.app.service.ContributionService.CampaignView;
import com.afrivox.app.service.ContributionService.PromptView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entree de l'ecran d'enregistrement : quelles campagnes, et quelles phrases lire ensuite.
 * Les campagnes et phrases generees ({@code /api/campaigns}, {@code /api/prompts}) restent pour l'admin.
 */
@RestController
@RequestMapping("/api/me/campaigns")
public class MyCampaignResource {

    private final ContributionService contributionService;

    public MyCampaignResource(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    /** {@code GET /api/me/campaigns} : campagnes ouvertes, avec le nombre de phrases et ce que j'ai deja enregistre. */
    @GetMapping("")
    public List<CampaignView> getOpenCampaigns() {
        return contributionService.openCampaigns();
    }

    /**
     * {@code GET /api/me/campaigns/{id}/todo?limit=10} : phrases que je n'ai pas encore enregistrees (liste vide :
     * campagne terminee pour moi ; 404 : campagne inconnue ou fermee).
     */
    @GetMapping("/{id}/todo")
    public ResponseEntity<List<PromptView>> getTodo(@PathVariable("id") Long id, @RequestParam(defaultValue = "10") int limit) {
        return contributionService
            .todo(id, limit)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
