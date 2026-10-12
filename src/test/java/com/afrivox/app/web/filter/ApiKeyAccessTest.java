package com.afrivox.app.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.domain.enumeration.ApiKeyScope;
import org.junit.jupiter.api.Test;

/** Liste blanche des appels permis à une clé d'API : tout le reste doit rester refusé. */
class ApiKeyAccessTest {

    @Test
    void downloadScopeOpensPurchaseListingAndDownloadLinks() {
        assertThat(ApiKeyAccess.allowedScopes("GET", "/api/me/purchases")).contains(java.util.EnumSet.of(ApiKeyScope.DOWNLOAD));
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/purchases/42/download-link")).isPresent();
    }

    @Test
    void evaluationAndPublicationRoutesCarryTheirOwnScope() {
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/me/models/12/evaluations")).contains(
            java.util.EnumSet.of(ApiKeyScope.EVALUATE)
        );
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/me/models")).contains(java.util.EnumSet.of(ApiKeyScope.PUBLISH));
        assertThat(ApiKeyAccess.allowedScopes("GET", "/api/me/models").orElseThrow()).containsExactlyInAnyOrder(
            ApiKeyScope.EVALUATE,
            ApiKeyScope.PUBLISH
        );
    }

    @Test
    void methodIsPartOfTheRule() {
        assertThat(ApiKeyAccess.allowedScopes("GET", "/api/me/models/12/evaluations")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("DELETE", "/api/me/models")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("post", "/api/me/models")).isPresent();
    }

    @Test
    void everythingSensitiveIsOutsideTheWhitelist() {
        // Acheter, gerer les cles, administrer, supprimer un modele, lire le compte, modifier le profil...
        String[][] forbidden = {
            { "POST", "/api/me/purchases" },
            { "GET", "/api/me/api-keys" },
            { "POST", "/api/me/api-keys" },
            { "DELETE", "/api/me/api-keys/3" },
            { "DELETE", "/api/me/models/12" },
            { "GET", "/api/admin/users" },
            { "POST", "/api/admin/credits/adjust" },
            { "GET", "/api/account" },
            { "POST", "/api/account/change-password" },
            { "PUT", "/api/me/contributor/profile" },
            { "GET", "/api/me/credits" },
            { "POST", "/api/community/models/1/comments" },
            { "GET", "/management/health" },
        };
        for (String[] call : forbidden) {
            assertThat(ApiKeyAccess.allowedScopes(call[0], call[1])).as("%s %s", call[0], call[1]).isEmpty();
        }
    }

    @Test
    void pathMustMatchExactlyNoTrailingOrPrefixTricks() {
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/purchases/42/download-link/extra")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/purchases/abc/download-link")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("POST", "/api/purchases/../admin/download-link")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("GET", "/api/me/purchases/")).isEmpty();
        assertThat(ApiKeyAccess.allowedScopes("GET", "/x/api/me/purchases")).isEmpty();
    }

    @Test
    void publicReadsAreRecognised() {
        assertThat(ApiKeyAccess.isPublic("GET", "/api/public/datasets")).isTrue();
        assertThat(ApiKeyAccess.isPublic("GET", "/api/public/test-sets/5/template.csv")).isTrue();
        assertThat(ApiKeyAccess.isPublic("GET", "/api/consent/current")).isTrue();
        // Une ecriture sous /api/public (le telechargement par jeton est un GET, rien d'autre) n'est pas « publique »
        assertThat(ApiKeyAccess.isPublic("POST", "/api/public/datasets")).isFalse();
        assertThat(ApiKeyAccess.isPublic("GET", "/api/me/purchases")).isFalse();
    }
}
