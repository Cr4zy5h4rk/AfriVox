package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Calcul du WER et du CER : c'est lui qui fait foi pour le classement des modeles. */
class ErrorRateTest {

    @Test
    void identicalSentencesHaveNoError() {
        ErrorRate.Counts counts = ErrorRate.words("nanga def", "nanga def");

        assertThat(counts.edits()).isZero();
        assertThat(counts.referenceLength()).isEqualTo(2);
        assertThat(counts.percent()).isZero();
    }

    @Test
    void oneSubstitutionInFourWordsIsTwentyFivePercent() {
        ErrorRate.Counts counts = ErrorRate.words("mangi fi rekk jàmm", "mangi fi rekk jam");

        assertThat(counts.edits()).isEqualTo(1);
        assertThat(counts.referenceLength()).isEqualTo(4);
        assertThat(counts.percent()).isEqualTo(25.0, within(1e-9));
    }

    @Test
    void countsInsertionsAndDeletions() {
        assertThat(ErrorRate.words("a b c", "a c").edits()).isEqualTo(1);
        assertThat(ErrorRate.words("a c", "a b c").edits()).isEqualTo(1);
    }

    @Test
    void rateCanExceedOneHundredPercent() {
        ErrorRate.Counts counts = ErrorRate.words("a", "a b c");

        assertThat(counts.edits()).isEqualTo(2);
        assertThat(counts.percent()).isEqualTo(200.0, within(1e-9));
    }

    @Test
    void emptyPredictionCostsTheWholeReference() {
        ErrorRate.Counts counts = ErrorRate.words("un deux trois", "");

        assertThat(counts.edits()).isEqualTo(3);
        assertThat(counts.percent()).isEqualTo(100.0, within(1e-9));
    }

    @Test
    void emptyReferenceGivesZeroPercentInsteadOfDividingByZero() {
        assertThat(ErrorRate.words("", "bruit").percent()).isZero();
    }

    @Test
    void caseAndPunctuationAreIgnored() {
        ErrorRate.Counts counts = ErrorRate.words("Nanga def, nit ku baax !", "nanga   DEF nit ku baax");

        assertThat(counts.edits()).isZero();
    }

    @Test
    void typographicApostropheEqualsStraightApostrophe() {
        assertThat(ErrorRate.words("dafa d’oon", "dafa d'oon").edits()).isZero();
    }

    @Test
    void decomposedAccentsEqualPrecomposedOnes() {
        // « é » en un seul caractere contre « e » suivi d'un accent combinant : meme texte pour un humain.
        assertThat(ErrorRate.words("téere", "téere").edits()).isZero();
    }

    @Test
    void specificLettersAreKeptSoTheyCanBeWrong() {
        // Le n tilde du wolof est une lettre : « ñaar » et « naar » sont deux mots differents.
        assertThat(ErrorRate.words("ñaar", "naar").edits()).isEqualTo(1);
    }

    @Test
    void characterErrorRateUsesLevenshteinOnCharacters() {
        // kitten -> sitting : 3 operations pour 6 caracteres de reference.
        ErrorRate.Counts counts = ErrorRate.characters("kitten", "sitting");

        assertThat(counts.edits()).isEqualTo(3);
        assertThat(counts.referenceLength()).isEqualTo(6);
        assertThat(counts.percent()).isEqualTo(50.0, within(1e-9));
    }

    @Test
    void countsAreSummedAcrossSentencesBeforeTheRatioIsTaken() {
        // Un corpus se note sur le total (micro-moyenne), pas sur la moyenne des phrases :
        // 1 erreur sur 1 mot + 0 erreur sur 9 mots = 10 %, et non 50 %.
        ErrorRate.Counts total = ErrorRate.words("a", "b").plus(ErrorRate.words("a b c d e f g h i", "a b c d e f g h i"));

        assertThat(total.edits()).isEqualTo(1);
        assertThat(total.referenceLength()).isEqualTo(10);
        assertThat(total.percent()).isEqualTo(10.0, within(1e-9));
    }

    @Test
    void wordCountWorksOnNormalizedText() {
        assertThat(ErrorRate.wordCount("  Bonjour,   le  monde ! ")).isEqualTo(3);
        assertThat(ErrorRate.wordCount("")).isZero();
        assertThat(ErrorRate.wordCount(null)).isZero();
        assertThat(ErrorRate.wordCount("...")).isZero();
    }
}
