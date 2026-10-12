package com.afrivox.app.web.filter;

import com.afrivox.app.domain.enumeration.ApiKeyScope;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Liste blanche des appels qu'une clé d'API peut faire. Tout ce qui n'y figure pas est refusé pour une clé (achat,
 * gestion des clés, administration, compte…), même si le compte propriétaire y a droit avec une session.
 * Une règle = méthode HTTP + chemin + au moins une des portées listées.
 */
public final class ApiKeyAccess {

    private record Rule(String method, Pattern path, Set<ApiKeyScope> anyOf) {}

    private static final List<Rule> RULES = List.of(
        // Récupérer un dataset acheté : lister mes achats, puis demander un lien de téléchargement.
        new Rule("GET", Pattern.compile("^/api/me/purchases$"), EnumSet.of(ApiKeyScope.DOWNLOAD)),
        new Rule("POST", Pattern.compile("^/api/purchases/\\d+/download-link$"), EnumSet.of(ApiKeyScope.DOWNLOAD)),
        // Classement : retrouver l'identifiant d'un de mes modèles, puis envoyer des prédictions.
        new Rule("GET", Pattern.compile("^/api/me/models$"), EnumSet.of(ApiKeyScope.EVALUATE, ApiKeyScope.PUBLISH)),
        new Rule("POST", Pattern.compile("^/api/me/models/\\d+/evaluations$"), EnumSet.of(ApiKeyScope.EVALUATE)),
        // Publier un modèle.
        new Rule("POST", Pattern.compile("^/api/me/models$"), EnumSet.of(ApiKeyScope.PUBLISH))
    );

    private ApiKeyAccess() {}

    /** Portées dont l'une suffit pour cet appel ; vide si une clé n'a pas le droit de l'appeler du tout. */
    public static Optional<Set<ApiKeyScope>> allowedScopes(String method, String path) {
        for (Rule rule : RULES) {
            if (rule.method().equalsIgnoreCase(method) && rule.path().matcher(path).matches()) {
                return Optional.of(rule.anyOf());
            }
        }
        return Optional.empty();
    }

    /**
     * Lectures publiques (catalogue, classement, modèle de CSV) : ouvertes à tous, sans authentification. Un client
     * qui envoie sa clé sur toutes ses requêtes ne doit pas être refusé ici.
     */
    public static boolean isPublic(String method, String path) {
        return "GET".equalsIgnoreCase(method) && (path.startsWith("/api/public/") || path.equals("/api/consent/current"));
    }
}
