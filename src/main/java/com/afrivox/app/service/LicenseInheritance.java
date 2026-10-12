package com.afrivox.app.service;

import com.afrivox.app.domain.enumeration.LicenseKind;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Héritage de licence : un modèle ne peut pas être publié avec plus de droits que les données qui l'ont entraîné.
 *
 * <p>L'acheteur d'un dataset choisit une licence d'usage : {@code PROTOTYPE} (essais) &lt; {@code RESEARCH}
 * (recherche, non commercial) &lt; {@code COMMERCIAL}. Un modèle entraîné sur plusieurs versions est borné par la
 * <b>plus restrictive</b> des licences que son auteur détient (le « plafond »). Le développeur déclare l'usage qu'il
 * accorde à son modèle : il ne peut pas dépasser le plafond.
 *
 * <p>Deuxième garde-fou : le nom de licence libre du modèle ({@code MIT}, {@code CC-BY-NC-4.0}…). Une licence qui
 * autorise l'usage commercial est incompatible avec un plafond plus bas, même si l'usage déclaré est correct, sinon
 * le modèle serait publié sous un texte qui contredit ses données. Un nom inconnu n'est pas jugé : seul l'usage
 * déclaré fait foi.
 *
 * <p>Limites assumées : on vérifie les licences d'achat de l'auteur sur la plateforme, pas ce que le modèle a
 * réellement vu à l'entraînement (les datasets cités sont déclaratifs) ; le texte de la licence du modèle n'est pas
 * analysé au-delà de quelques noms courants.
 */
public final class LicenseInheritance {

    /** Clé d'erreur : l'usage déclaré dépasse ce que permettent les datasets. */
    public static final String EXCEEDS_CEILING = "licenseinheritance";

    /** Clé d'erreur : le nom de la licence du modèle autorise plus que les datasets. */
    public static final String LICENSE_CONFLICT = "licenseconflict";

    private static final Pattern NON_COMMERCIAL = Pattern.compile(
        "(non[- ]?commercial|(^|[^a-z])nc([^a-z]|$)|research[- ]only|research[- ]use|recherche|academic|acad[ée]mique)"
    );
    private static final Pattern PROTOTYPE = Pattern.compile("(prototype|prototypage|evaluation[- ]only|trial)");
    private static final Pattern COMMERCIAL_OK = Pattern.compile(
        "((^|[^a-z])(mit|isc|bsd|mpl|zlib|wtfpl|gpl|lgpl|agpl|apache|openrail|unlicense|cc0|cc[- ]by)([^a-z]|$)|public domain)"
    );

    private LicenseInheritance() {}

    /** 0 = prototype, 1 = recherche, 2 = commercial. */
    public static int rank(LicenseKind kind) {
        return switch (kind) {
            case PROTOTYPE -> 0;
            case RESEARCH -> 1;
            case COMMERCIAL -> 2;
        };
    }

    /** La plus large de plusieurs licences détenues pour une même version (plusieurs achats possibles). */
    public static Optional<LicenseKind> strongest(Collection<LicenseKind> held) {
        return held.stream().max((a, b) -> Integer.compare(rank(a), rank(b)));
    }

    /** Le plafond d'un modèle : la plus restrictive des licences détenues, une par version citée. */
    public static Optional<LicenseKind> ceiling(Collection<LicenseKind> onePerVersion) {
        return onePerVersion.stream().min((a, b) -> Integer.compare(rank(a), rank(b)));
    }

    /** Usage que laisse présumer le nom de licence saisi ; vide s'il n'est pas reconnu. */
    public static Optional<LicenseKind> impliedUsage(String licenseName) {
        if (licenseName == null || licenseName.isBlank()) {
            return Optional.empty();
        }
        String name = licenseName.toLowerCase(Locale.ROOT).trim();
        // Les mentions restrictives d'abord : « CC-BY-NC » contient « cc-by » mais n'autorise pas le commercial
        if (NON_COMMERCIAL.matcher(name).find()) {
            return Optional.of(LicenseKind.RESEARCH);
        }
        if (PROTOTYPE.matcher(name).find()) {
            return Optional.of(LicenseKind.PROTOTYPE);
        }
        if (COMMERCIAL_OK.matcher(name).find()) {
            return Optional.of(LicenseKind.COMMERCIAL);
        }
        return Optional.empty();
    }

    /**
     * Vérifie un modèle contre son plafond.
     *
     * @param ceiling     plafond des datasets cités ; {@code null} = aucun dataset cité, rien à hériter
     * @param declared    usage déclaré par l'auteur
     * @param licenseName nom de licence libre du modèle (peut être {@code null})
     * @return la clé d'erreur à renvoyer, ou vide si tout est cohérent
     */
    public static Optional<String> violation(LicenseKind ceiling, LicenseKind declared, String licenseName) {
        if (ceiling == null) {
            return Optional.empty();
        }
        if (declared != null && rank(declared) > rank(ceiling)) {
            return Optional.of(EXCEEDS_CEILING);
        }
        Optional<LicenseKind> implied = impliedUsage(licenseName);
        if (implied.isPresent() && rank(implied.orElseThrow()) > rank(ceiling)) {
            return Optional.of(LICENSE_CONFLICT);
        }
        return Optional.empty();
    }
}
