package com.afrivox.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Contrôles qualité d'un enregistrement WAV PCM 16 bits : durée réelle, niveau, saturation, silence, bruit.
 * Calcul pur, sur les échantillons du fichier (jamais sur ce que le client déclare), sans bibliothèque externe.
 *
 * <p><b>Ce qui est mesuré</b> (sur le signal mono, trames de 20 ms) :
 * <ul>
 *   <li>niveau crête et niveau efficace (RMS), en dBFS (0 = pleine échelle) ;</li>
 *   <li>part d'échantillons saturés (écrêtage) ;</li>
 *   <li>part de trames silencieuses ;</li>
 *   <li>écart entre les trames fortes (90e centile) et faibles (10e centile) : c'est une <em>estimation</em> du
 *       rapport signal/bruit, pas une mesure de laboratoire ;</li>
 *   <li>composante continue.</li>
 * </ul>
 *
 * <p><b>Décision</b> : un défaut « bloquant » fait refuser l'envoi (silence, trop faible, saturé, durée déclarée
 * incohérente) ; un défaut « léger » (bruit, saturation légère, faible fréquence d'échantillonnage) n'abaisse que
 * la note de 0 à 100. Seuls les WAV PCM 16 bits sont analysés : les formats compressés (webm, ogg, m4a) demanderaient
 * un décodeur, ils restent {@code PENDING} (non vérifiés).
 */
public final class AudioQualityAnalyzer {

    private AudioQualityAnalyzer() {}

    /** Seuils ; les valeurs par défaut sont celles de {@code application.audio.qc.*}. */
    public record Thresholds(
        double minPeakDbfs,
        double silenceFrameDbfs,
        double maxSilenceRatio,
        double hardClippingRatio,
        double softClippingRatio,
        double minSnrDb,
        double durationToleranceRatio,
        int durationToleranceMs
    ) {
        public static Thresholds defaults() {
            return new Thresholds(-30, -50, 0.9, 0.05, 0.005, 12, 0.25, 500);
        }
    }

    /** Un défaut détecté. {@code code} est stable (clé d'erreur côté interface) ; {@code hard} = envoi refusé. */
    public record Issue(String code, boolean hard) {}

    public record Report(
        int sampleRate,
        int channels,
        int durationMs,
        double peakDbfs,
        double rmsDbfs,
        double clippingRatio,
        double silenceRatio,
        double snrDb,
        double dcOffset,
        float score,
        List<Issue> issues
    ) {
        /** Vrai si aucun défaut bloquant. */
        public boolean passed() {
            return issues.stream().noneMatch(Issue::hard);
        }

        /** Premier défaut bloquant (le plus grave, dans l'ordre de détection). */
        public Optional<Issue> firstHard() {
            return issues.stream().filter(Issue::hard).findFirst();
        }

        /** Rapport compact conservé avec l'enregistrement (colonne {@code qc_report}). */
        public String toJson() {
            StringBuilder codes = new StringBuilder();
            for (Issue issue : issues) {
                if (codes.length() > 0) {
                    codes.append(',');
                }
                codes.append('"').append(issue.code()).append('"');
            }
            return String.format(
                Locale.ROOT,
                "{\"sr\":%d,\"ch\":%d,\"ms\":%d,\"peak\":%.1f,\"rms\":%.1f,\"clip\":%.4f,\"silence\":%.3f,\"snr\":%.1f,\"dc\":%.4f,\"score\":%.1f,\"issues\":[%s]}",
                sampleRate,
                channels,
                durationMs,
                peakDbfs,
                rmsDbfs,
                clippingRatio,
                silenceRatio,
                snrDb,
                dcOffset,
                score,
                codes
            );
        }
    }

    private static final double FLOOR_DB = -120;
    private static final double CLIP_LEVEL = 32_700d / 32_768d;
    private static final int FRAME_MS = 20;
    /** Sous cette fréquence, un signal vocal est trop pauvre (la parole utile monte à 4 kHz au moins). */
    private static final int MIN_SAMPLE_RATE = 8_000;

    /**
     * Analyse un fichier WAV.
     *
     * @param declaredDurationMs durée annoncée par le client (peut être {@code null}) : un écart important est
     *                           un défaut bloquant, la durée mesurée fait foi
     * @return vide si le contenu n'est pas un WAV PCM 16 bits lisible (non analysable, pas une erreur)
     */
    public static Optional<Report> analyze(byte[] wav, Thresholds t, Integer declaredDurationMs) {
        Header h = parse(wav);
        if (h == null) {
            return Optional.empty();
        }
        int frameCount = h.length / (2 * h.channels);
        if (frameCount == 0) {
            return Optional.empty();
        }

        // Mixage mono, échantillons dans [-1, 1)
        double[] x = new double[frameCount];
        double sum = 0;
        double peak = 0;
        double sumSquares = 0;
        long clipped = 0;
        for (int i = 0; i < frameCount; i++) {
            double v = 0;
            for (int c = 0; c < h.channels; c++) {
                int at = h.start + (i * h.channels + c) * 2;
                v += (short) ((wav[at] & 0xFF) | (wav[at + 1] << 8)) / 32_768d;
            }
            v /= h.channels;
            x[i] = v;
            sum += v;
            sumSquares += v * v;
            double abs = Math.abs(v);
            peak = Math.max(peak, abs);
            if (abs >= CLIP_LEVEL) {
                clipped++;
            }
        }
        double rms = Math.sqrt(sumSquares / frameCount);
        double dc = Math.abs(sum / frameCount);
        double clipping = (double) clipped / frameCount;

        // Trames de 20 ms : niveau de chacune
        int frameLength = Math.max(1, (h.sampleRate * FRAME_MS) / 1000);
        int frames = Math.max(1, frameCount / frameLength);
        double[] frameDb = new double[frames];
        int silent = 0;
        for (int f = 0; f < frames; f++) {
            double s = 0;
            int from = f * frameLength;
            int to = Math.min(frameCount, from + frameLength);
            for (int i = from; i < to; i++) {
                s += x[i] * x[i];
            }
            frameDb[f] = db(Math.sqrt(s / Math.max(1, to - from)));
            if (frameDb[f] < t.silenceFrameDbfs()) {
                silent++;
            }
        }
        double silenceRatio = (double) silent / frames;
        double[] sorted = frameDb.clone();
        Arrays.sort(sorted);
        double loud = sorted[(int) Math.min(sorted.length - 1, Math.floor(0.9 * sorted.length))];
        double quiet = sorted[(int) Math.floor(0.1 * (sorted.length - 1))];
        double snr = Math.max(0, loud - quiet);

        int durationMs = (int) Math.round((frameCount * 1000d) / h.sampleRate);
        double peakDb = db(peak);
        double rmsDb = db(rms);

        List<Issue> issues = new ArrayList<>();
        // ---- Défauts bloquants (envoi refusé), du plus grave au moins grave --------------------------------------
        // « Silencieux » = rien d'audible (crête sous -60 dBFS) ou presque aucune trame avec du signal.
        // « Trop faible » = un signal existe mais trop bas pour etre exploite (micro eloigne, gain a zero).
        boolean allSilent;
        if (peakDb < -60) {
            allSilent = true;
            issues.add(new Issue("qcsilent", true));
        } else if (peakDb < t.minPeakDbfs()) {
            allSilent = false;
            issues.add(new Issue("qctooquiet", true));
        } else if (silenceRatio > t.maxSilenceRatio()) {
            allSilent = true;
            issues.add(new Issue("qcsilent", true));
        } else {
            allSilent = false;
        }
        if (clipping > t.hardClippingRatio()) {
            issues.add(new Issue("qcclipping", true));
        }
        if (declaredDurationMs != null) {
            double allowed = Math.max(t.durationToleranceMs(), t.durationToleranceRatio() * durationMs);
            if (Math.abs(declaredDurationMs - durationMs) > allowed) {
                issues.add(new Issue("qcmismatch", true));
            }
        }
        // ---- Défauts légers (note seulement) ----------------------------------------------------------------------
        if (clipping > t.softClippingRatio() && clipping <= t.hardClippingRatio()) {
            issues.add(new Issue("clipping", false));
        }
        if (!allSilent && snr < t.minSnrDb()) {
            issues.add(new Issue("noisy", false));
        }
        if (h.sampleRate < 16_000) {
            issues.add(new Issue("lowsamplerate", false));
        }
        if (dc > 0.05) {
            issues.add(new Issue("dcoffset", false));
        }
        if (h.sampleRate < MIN_SAMPLE_RATE) {
            issues.add(new Issue("qcsamplerate", true));
        }

        double score = 100;
        score -= Math.min(40, clipping * 800); // 5 % saturé = -40
        if (snr < 20) {
            score -= Math.min(30, (20 - snr) * 2.5);
        }
        if (peakDb < -12) {
            score -= Math.min(20, (-12 - peakDb) * 2);
        }
        if (silenceRatio > 0.5) {
            score -= Math.min(20, (silenceRatio - 0.5) * 50);
        }
        if (h.sampleRate < 16_000) {
            score -= 10;
        }
        if (dc > 0.05) {
            score -= 5;
        }
        float finalScore = (float) Math.max(0, Math.min(100, Math.round(score * 10) / 10d));
        return Optional.of(
            new Report(
                h.sampleRate,
                h.channels,
                durationMs,
                peakDb,
                rmsDb,
                clipping,
                silenceRatio,
                snr,
                dc,
                finalScore,
                List.copyOf(issues)
            )
        );
    }

    private static double db(double linear) {
        return linear <= 1e-6 ? FLOOR_DB : Math.max(FLOOR_DB, 20 * Math.log10(linear));
    }

    // ---- En-tête WAV ---------------------------------------------------------------------------------------------

    private record Header(int sampleRate, int channels, int start, int length) {}

    /** {@code null} si ce n'est pas un WAV PCM 16 bits (PCM = 1, ou extensible = 0xFFFE) d'un ou deux canaux. */
    private static Header parse(byte[] w) {
        if (w.length < 44 || !tag(w, 0, "RIFF") || !tag(w, 8, "WAVE")) {
            return null;
        }
        int sampleRate = 0;
        int channels = 0;
        boolean pcm16 = false;
        long pos = 12;
        while (pos + 8 <= w.length) {
            int at = (int) pos;
            long size = leInt(w, at + 4) & 0xFFFFFFFFL;
            int body = at + 8;
            if (tag(w, at, "fmt ")) {
                if (body + 16 > w.length) {
                    return null;
                }
                int format = leShort(w, body);
                channels = leShort(w, body + 2);
                sampleRate = leInt(w, body + 4);
                int bits = leShort(w, body + 14);
                pcm16 = (format == 1 || format == 0xFFFE) && bits == 16;
            } else if (tag(w, at, "data")) {
                if (!pcm16 || channels < 1 || channels > 2 || sampleRate < 1_000 || sampleRate > 192_000) {
                    return null;
                }
                long available = w.length - body;
                long length = size == 0 || size > available ? available : size;
                return length >= 2L * channels ? new Header(sampleRate, channels, body, (int) length) : null;
            }
            pos = (long) body + size + (size & 1);
        }
        return null;
    }

    private static boolean tag(byte[] w, int at, String tag) {
        for (int i = 0; i < 4; i++) {
            if (w[at + i] != (byte) tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int leInt(byte[] w, int at) {
        return (w[at] & 0xFF) | ((w[at + 1] & 0xFF) << 8) | ((w[at + 2] & 0xFF) << 16) | ((w[at + 3] & 0xFF) << 24);
    }

    private static int leShort(byte[] w, int at) {
        return (w[at] & 0xFF) | ((w[at + 1] & 0xFF) << 8);
    }
}
