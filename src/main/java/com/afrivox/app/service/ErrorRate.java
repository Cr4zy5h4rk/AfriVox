package com.afrivox.app.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Taux d'erreur de mots (WER) et de caracteres (CER) : distance d'edition de Levenshtein entre la reference et la
 * prediction, rapportee a la longueur de la reference. Calcul pur, sans etat : a deux lignes de memoire pres, aucun
 * tableau complet n'est construit, donc un jeu de test de quelques milliers de phrases se calcule en quelques ms.
 * <p>
 * Normalisation (la meme pour la reference et la prediction, sinon le score mesurerait la ponctuation) : Unicode NFC,
 * minuscules, tout ce qui n'est ni lettre ni chiffre ni apostrophe devient une espace, espaces regroupees. Les lettres
 * propres au wolof et au sereer (n tilde, eng, voyelles accentuees) sont des lettres Unicode : elles sont conservees.
 */
public final class ErrorRate {

    private ErrorRate() {}

    /** Erreurs cumulees ({@code edits}) pour {@code referenceLength} elements de reference. */
    public record Counts(long edits, long referenceLength) {
        public Counts plus(Counts other) {
            return new Counts(edits + other.edits, referenceLength + other.referenceLength);
        }

        /** En pourcentage (0 = parfait, peut depasser 100). */
        public double percent() {
            return referenceLength == 0 ? 0 : (100d * edits) / referenceLength;
        }
    }

    public static String normalize(String text) {
        String nfc = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(nfc.length());
        boolean pendingSpace = false;
        for (int i = 0; i < nfc.length(); ) {
            int cp = nfc.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '’' || cp == '‘') {
                cp = '\'';
            }
            if (Character.isLetterOrDigit(cp) || cp == '\'' || Character.getType(cp) == Character.NON_SPACING_MARK) {
                if (pendingSpace && sb.length() > 0) {
                    sb.append(' ');
                }
                pendingSpace = false;
                sb.appendCodePoint(cp);
            } else {
                pendingSpace = true;
            }
        }
        return sb.toString();
    }

    public static Counts words(String reference, String prediction) {
        String[] ref = split(normalize(reference));
        String[] hyp = split(normalize(prediction));
        return new Counts(distance(ref, hyp), ref.length);
    }

    public static Counts characters(String reference, String prediction) {
        int[] ref = normalize(reference).codePoints().toArray();
        int[] hyp = normalize(prediction).codePoints().toArray();
        return new Counts(distance(ref, hyp), ref.length);
    }

    /** Nombre de mots d'un texte une fois normalise (sert a plafonner la longueur d'une ligne soumise). */
    public static int wordCount(String text) {
        return split(normalize(text)).length;
    }

    private static String[] split(String normalized) {
        return normalized.isEmpty() ? new String[0] : normalized.split(" ");
    }

    private static int distance(String[] a, String[] b) {
        int[] previous = new int[b.length + 1];
        int[] current = new int[b.length + 1];
        for (int j = 0; j <= b.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length; i++) {
            current[0] = i;
            for (int j = 1; j <= b.length; j++) {
                int substitution = previous[j - 1] + (a[i - 1].equals(b[j - 1]) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length];
    }

    private static int distance(int[] a, int[] b) {
        int[] previous = new int[b.length + 1];
        int[] current = new int[b.length + 1];
        for (int j = 0; j <= b.length; j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length; i++) {
            current[0] = i;
            for (int j = 1; j <= b.length; j++) {
                int substitution = previous[j - 1] + (a[i - 1] == b[j - 1] ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length];
    }
}
