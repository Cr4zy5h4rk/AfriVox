package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.domain.enumeration.LicenseKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Héritage de licence : l'ordre des usages, le plafond, et la lecture des noms de licence courants. */
class LicenseInheritanceTest {

    @Test
    void usesAreOrderedPrototypeThenResearchThenCommercial() {
        assertThat(LicenseInheritance.rank(LicenseKind.PROTOTYPE)).isLessThan(LicenseInheritance.rank(LicenseKind.RESEARCH));
        assertThat(LicenseInheritance.rank(LicenseKind.RESEARCH)).isLessThan(LicenseInheritance.rank(LicenseKind.COMMERCIAL));
    }

    @Test
    void ceilingIsTheMostRestrictiveLicenseHeld() {
        assertThat(LicenseInheritance.ceiling(List.of(LicenseKind.COMMERCIAL, LicenseKind.RESEARCH, LicenseKind.COMMERCIAL))).contains(
            LicenseKind.RESEARCH
        );
        assertThat(LicenseInheritance.ceiling(List.of(LicenseKind.COMMERCIAL))).contains(LicenseKind.COMMERCIAL);
        assertThat(LicenseInheritance.ceiling(List.of())).isEmpty();
    }

    @Test
    void severalPurchasesOfTheSameVersionGiveTheBroadestRight() {
        assertThat(LicenseInheritance.strongest(List.of(LicenseKind.PROTOTYPE, LicenseKind.COMMERCIAL, LicenseKind.RESEARCH))).contains(
            LicenseKind.COMMERCIAL
        );
    }

    @Test
    void declaredUseCannotExceedTheCeiling() {
        assertThat(LicenseInheritance.violation(LicenseKind.RESEARCH, LicenseKind.COMMERCIAL, null)).contains(
            LicenseInheritance.EXCEEDS_CEILING
        );
        assertThat(LicenseInheritance.violation(LicenseKind.PROTOTYPE, LicenseKind.RESEARCH, null)).contains(
            LicenseInheritance.EXCEEDS_CEILING
        );
    }

    @Test
    void declaredUseAtOrBelowTheCeilingIsFine() {
        assertThat(LicenseInheritance.violation(LicenseKind.RESEARCH, LicenseKind.RESEARCH, null)).isEmpty();
        assertThat(LicenseInheritance.violation(LicenseKind.COMMERCIAL, LicenseKind.PROTOTYPE, null)).isEmpty();
        assertThat(LicenseInheritance.violation(LicenseKind.COMMERCIAL, LicenseKind.COMMERCIAL, "MIT")).isEmpty();
    }

    @Test
    void noCeilingMeansNothingToInherit() {
        assertThat(LicenseInheritance.violation(null, LicenseKind.COMMERCIAL, "MIT")).isEmpty();
    }

    @Test
    void permissiveLicenseNameConflictsWithALowerCeiling() {
        // L'usage declare est correct (recherche) mais le texte « MIT » autorise le commercial : incoherent
        assertThat(LicenseInheritance.violation(LicenseKind.RESEARCH, LicenseKind.RESEARCH, "MIT")).contains(
            LicenseInheritance.LICENSE_CONFLICT
        );
        assertThat(LicenseInheritance.violation(LicenseKind.PROTOTYPE, LicenseKind.PROTOTYPE, "Apache-2.0")).contains(
            LicenseInheritance.LICENSE_CONFLICT
        );
    }

    @Test
    void nonCommercialLicenseNameIsCompatibleWithResearch() {
        assertThat(LicenseInheritance.violation(LicenseKind.RESEARCH, LicenseKind.RESEARCH, "CC-BY-NC-4.0")).isEmpty();
        assertThat(LicenseInheritance.violation(LicenseKind.RESEARCH, LicenseKind.RESEARCH, "CC-BY-NC-SA-4.0")).isEmpty();
    }

    @Test
    void nameRecognitionCoversCommonLicenses() {
        for (String commercial : List.of(
            "MIT",
            "mit",
            "Apache-2.0",
            "BSD-3-Clause",
            "ISC",
            "CC0-1.0",
            "CC-BY-4.0",
            "CC-BY-SA-4.0",
            "OpenRAIL-M",
            "GPL-3.0",
            "LGPL-3.0",
            "AGPL-3.0",
            "MPL-2.0"
        )) {
            assertThat(LicenseInheritance.impliedUsage(commercial)).as(commercial).contains(LicenseKind.COMMERCIAL);
        }
        for (String research : List.of(
            "CC-BY-NC-4.0",
            "CC BY-NC",
            "Non-commercial",
            "research only",
            "Licence recherche",
            "Academic use"
        )) {
            assertThat(LicenseInheritance.impliedUsage(research)).as(research).contains(LicenseKind.RESEARCH);
        }
        assertThat(LicenseInheritance.impliedUsage("Prototype evaluation licence")).contains(LicenseKind.PROTOTYPE);
    }

    @Test
    void unknownOrEmptyNamesAreNotJudged() {
        assertThat(LicenseInheritance.impliedUsage("Licence maison")).isEqualTo(Optional.empty());
        assertThat(LicenseInheritance.impliedUsage("")).isEmpty();
        assertThat(LicenseInheritance.impliedUsage(null)).isEmpty();
    }

    @Test
    void wordsContainingTheLettersOfALicenseNameDoNotMatch() {
        // « permit », « limited », « committee » contiennent « mit » sans etre la licence MIT
        assertThat(LicenseInheritance.impliedUsage("permit granted by the committee")).isEmpty();
        assertThat(LicenseInheritance.impliedUsage("Limited use")).isEmpty();
    }
}
