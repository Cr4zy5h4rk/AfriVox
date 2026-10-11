package com.afrivox.app.domain.enumeration;

/**
 * Tranche d'age declaree par un contributeur (jamais la date de naissance). Declaratif, facultatif, et n'est publie
 * qu'en agregat dans la fiche d'un dataset.
 */
public enum AgeRange {
    R18_25,
    R26_35,
    R36_45,
    R46_60,
    R61_PLUS,
}
