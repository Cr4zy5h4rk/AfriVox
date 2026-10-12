package com.afrivox.app.domain.enumeration;

/** Ce que rapporte un tarif de credits (reglable par l'admin, globalement ou par campagne). */
public enum CreditRateKind {
    /** Un enregistrement passe en VALIDATED : verse a son auteur. */
    VALIDATED_RECORDING,
    /** Un avis concordant avec le consensus : verse au validateur. */
    REVIEW_AGREED,
    /** Une tache piege reussie : versee au validateur. */
    TRAP_CORRECT,
}
