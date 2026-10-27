package com.afrivox.app.service;

import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.net.URI;

/**
 * Liens saisis par les utilisateurs (projets, profils) : ils sont affiches comme liens cliquables, donc on n'accepte
 * que https avec un nom d'hote (ni javascript:, ni data:, ni http en clair, ni identifiants dans l'adresse).
 */
final class SafeUrl {

    private SafeUrl() {}

    /** Adresse nettoyee, ou {@code null} si la valeur est vide ; {@link BadRequestAlertException} si elle est refusee. */
    static String https(String url, String entity, String errorKey) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("lien refuse");
            }
            return uri.toString();
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Le lien doit etre une adresse https valide", entity, errorKey);
        }
    }
}
