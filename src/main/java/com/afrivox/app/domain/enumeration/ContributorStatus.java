package com.afrivox.app.domain.enumeration;

/**
 * Etat d'un contributeur vis-a-vis de la periode d'essai.
 * <ul>
 *   <li>{@code TRIAL} : nombre d'envois plafonne tant que ses premiers enregistrements n'ont pas ete releves ;</li>
 *   <li>{@code ACTIVE} : plus de plafond (promotion automatique ou decision de l'admin) ;</li>
 *   <li>{@code SUSPENDED} : n'a plus le droit d'envoyer (decision de l'admin).</li>
 * </ul>
 */
public enum ContributorStatus {
    TRIAL,
    ACTIVE,
    SUSPENDED,
}
