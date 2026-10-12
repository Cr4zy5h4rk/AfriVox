package com.afrivox.app.domain.enumeration;

/**
 * Ce qu'une clé d'API a le droit de faire. Une clé n'ouvre que ces actions, jamais le reste du compte
 * (ni achat, ni administration, ni gestion des clés).
 */
public enum ApiKeyScope {
    /** Lister ses achats et obtenir un lien de téléchargement. */
    DOWNLOAD,
    /** Lister ses modèles et envoyer des prédictions pour le classement. */
    EVALUATE,
    /** Publier un modèle. */
    PUBLISH,
}
