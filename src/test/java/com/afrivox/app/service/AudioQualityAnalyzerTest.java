package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.afrivox.app.service.AudioQualityAnalyzer.Issue;
import com.afrivox.app.service.AudioQualityAnalyzer.Report;
import com.afrivox.app.service.AudioQualityAnalyzer.Thresholds;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Controles qualite : chaque defaut est teste sur un signal synthetique dont on connait la valeur exacte. */
class AudioQualityAnalyzerTest {

    private static final Thresholds T = Thresholds.defaults();
    private static final int SR = 16_000;

    private static Report analyze(double[] signal, int sampleRate, int channels, Integer declaredMs) {
        return AudioQualityAnalyzer.analyze(TestAudio.wav(signal, sampleRate, channels), T, declaredMs).orElseThrow();
    }

    private static List<String> codes(Report report) {
        return report.issues().stream().map(Issue::code).toList();
    }

    // ---- Un bon enregistrement -----------------------------------------------------------------------------------

    @Test
    void normalSpeechPassesWithAHighScoreAndNoIssue() {
        Report report = analyze(TestAudio.speechLike(SR, 3, 0.5, 0.002), SR, 1, 3000);

        assertThat(report.passed()).isTrue();
        assertThat(report.issues()).isEmpty();
        assertThat(report.score()).isGreaterThan(95f);
        assertThat(report.durationMs()).isEqualTo(3000);
        assertThat(report.sampleRate()).isEqualTo(SR);
        // Amplitude 0,5 = -6 dBFS
        assertThat(report.peakDbfs()).isCloseTo(-6.0, within(0.2));
        assertThat(report.clippingRatio()).isZero();
    }

    @Test
    void stereoIsMixedAndKeepsTheRealDuration() {
        Report report = analyze(TestAudio.speechLike(SR, 2, 0.5, 0.002), SR, 2, null);

        assertThat(report.channels()).isEqualTo(2);
        assertThat(report.durationMs()).isEqualTo(2000);
        assertThat(report.passed()).isTrue();
    }

    // ---- Defauts bloquants ---------------------------------------------------------------------------------------

    @Test
    void digitalSilenceIsBlocking() {
        Report report = analyze(TestAudio.constant(SR * 2, 0), SR, 1, 2000);

        assertThat(report.passed()).isFalse();
        assertThat(report.firstHard().orElseThrow().code()).isEqualTo("qcsilent");
    }

    @Test
    void audibleButTooLowSignalIsTooQuietNotSilent() {
        // Crete a 0,005 = -46 dBFS : un signal existe, mais inexploitable
        Report report = analyze(TestAudio.speechLike(SR, 3, 0.005, 0.0005), SR, 1, 3000);

        assertThat(report.firstHard().orElseThrow().code()).isEqualTo("qctooquiet");
        assertThat(report.passed()).isFalse();
    }

    @Test
    void almostNoVoiceIsSilentEvenWithAPeak() {
        // 10 s de quasi-silence et une seule salve de 0,3 s : 97 % de trames vides
        double[] x = TestAudio.constant(SR * 10, 0.0005);
        double[] burst = TestAudio.speechLike(SR, 0.3, 0.5, 0.0);
        System.arraycopy(burst, 0, x, SR, burst.length);

        Report report = analyze(x, SR, 1, 10_000);

        assertThat(report.silenceRatio()).isGreaterThan(0.9);
        assertThat(report.firstHard().orElseThrow().code()).isEqualTo("qcsilent");
    }

    @Test
    void heavyClippingIsBlocking() {
        // Onde carree pleine echelle : tous les echantillons sont satures
        double[] x = new double[SR * 2];
        for (int i = 0; i < x.length; i++) {
            x[i] = (i / 40) % 2 == 0 ? 1.0 : -1.0;
        }

        Report report = analyze(x, SR, 1, 2000);

        assertThat(report.clippingRatio()).isGreaterThan(0.9);
        assertThat(codes(report)).contains("qcclipping");
        assertThat(report.passed()).isFalse();
    }

    @Test
    void declaredDurationFarFromTheRealOneIsBlocking() {
        double[] x = TestAudio.speechLike(SR, 2, 0.5, 0.002);

        assertThat(codes(analyze(x, SR, 1, 5000))).contains("qcmismatch");
        assertThat(analyze(x, SR, 1, 5000).passed()).isFalse();
    }

    @Test
    void smallDurationDifferenceIsTolerated() {
        double[] x = TestAudio.speechLike(SR, 2, 0.5, 0.002);

        assertThat(codes(analyze(x, SR, 1, 2300))).doesNotContain("qcmismatch");
        assertThat(codes(analyze(x, SR, 1, 1800))).doesNotContain("qcmismatch");
    }

    @Test
    void absentDeclaredDurationSkipsTheMismatchCheck() {
        assertThat(codes(analyze(TestAudio.speechLike(SR, 2, 0.5, 0.002), SR, 1, null))).doesNotContain("qcmismatch");
    }

    @Test
    void telephoneBandIsAcceptedButVeryLowRateIsBlocking() {
        assertThat(codes(analyze(TestAudio.speechLike(8_000, 2, 0.5, 0.002), 8_000, 1, null)))
            .contains("lowsamplerate")
            .doesNotContain("qcsamplerate");

        Report tooLow = analyze(TestAudio.speechLike(4_000, 2, 0.5, 0.002), 4_000, 1, null);
        assertThat(codes(tooLow)).contains("qcsamplerate");
        assertThat(tooLow.passed()).isFalse();
    }

    // ---- Defauts legers (note seulement) -------------------------------------------------------------------------

    @Test
    void lightClippingOnlyLowersTheScore() {
        double[] x = TestAudio.speechLike(SR, 3, 0.5, 0.002);
        // 1 % d'echantillons satures : au-dessus du seuil leger (0,5 %), sous le seuil bloquant (5 %)
        for (int i = 0; i < x.length / 100; i++) {
            x[i * 100] = 1.0;
        }

        Report report = analyze(x, SR, 1, 3000);

        assertThat(report.passed()).isTrue();
        assertThat(codes(report)).contains("clipping");
        assertThat(report.score()).isLessThan(95f);
    }

    @Test
    void continuousNoiseFloorIsFlaggedNoisyButNotBlocked() {
        // Pauses presque aussi fortes que la parole : 6 dB d'ecart seulement
        Report report = analyze(TestAudio.speechLike(SR, 3, 0.4, 0.2), SR, 1, 3000);

        assertThat(codes(report)).contains("noisy");
        assertThat(report.passed()).isTrue();
        assertThat(report.snrDb()).isLessThan(12);
        assertThat(report.score()).isLessThan(90f);
    }

    @Test
    void constantOffsetIsReported() {
        double[] x = TestAudio.speechLike(SR, 2, 0.3, 0.002);
        for (int i = 0; i < x.length; i++) {
            x[i] += 0.2;
        }

        Report report = analyze(x, SR, 1, 2000);

        assertThat(codes(report)).contains("dcoffset");
        assertThat(report.dcOffset()).isGreaterThan(0.1);
    }

    // ---- Non analysable / rapport --------------------------------------------------------------------------------

    @Test
    void contentThatIsNotPcm16WavIsNotAnalysed() {
        byte[] webm = new byte[200];
        webm[0] = 0x1A;
        webm[1] = 0x45;
        webm[2] = (byte) 0xDF;
        webm[3] = (byte) 0xA3;
        assertThat(AudioQualityAnalyzer.analyze(webm, T, null)).isEmpty();
        assertThat(AudioQualityAnalyzer.analyze(new byte[10], T, null)).isEmpty();

        // WAV flottant 32 bits et 8 bits : pas du PCM 16 bits
        byte[] floatWav = TestAudio.pcm16Wav(1000, 1);
        floatWav[20] = 3; // format = IEEE float
        assertThat(AudioQualityAnalyzer.analyze(floatWav, T, null)).isEmpty();
        byte[] eightBit = TestAudio.pcm16Wav(1000, 1);
        eightBit[34] = 8;
        assertThat(AudioQualityAnalyzer.analyze(eightBit, T, null)).isEmpty();
    }

    @Test
    void headerOnlyWavIsNotAnalysed() {
        assertThat(AudioQualityAnalyzer.analyze(TestAudio.wav(new double[0], SR, 1), T, null)).isEmpty();
    }

    @Test
    void reportJsonIsCompactAndListsTheIssues() {
        Report report = analyze(TestAudio.speechLike(SR, 2, 0.5, 0.002), SR, 1, 2000);
        String json = report.toJson();

        assertThat(json).startsWith("{").endsWith("}").contains("\"sr\":16000").contains("\"ms\":2000").contains("\"issues\":[]");
        assertThat(json.length()).isLessThan(1000);

        Report bad = analyze(TestAudio.constant(SR, 0), SR, 1, 1000);
        assertThat(bad.toJson()).contains("\"issues\":[\"qcsilent\"");
    }

    @Test
    void scoreStaysWithinZeroAndOneHundred() {
        double[] worst = new double[SR * 2];
        Random random = new Random(3);
        for (int i = 0; i < worst.length; i++) {
            worst[i] = random.nextBoolean() ? 1.0 : -1.0;
        }

        Report report = analyze(worst, SR, 1, 2000);

        assertThat(report.score()).isBetween(0f, 100f);
    }

    @Test
    void thresholdsComeFromConfiguration() {
        // Meme signal : accepte avec les seuils par defaut, refuse si on exige une crete bien plus haute
        double[] x = TestAudio.speechLike(SR, 2, 0.1, 0.001); // crete -20 dBFS
        byte[] wav = TestAudio.wav(x, SR, 1);

        Optional<Report> lenient = AudioQualityAnalyzer.analyze(wav, T, 2000);
        Optional<Report> strict = AudioQualityAnalyzer.analyze(wav, new Thresholds(-10, -50, 0.9, 0.05, 0.005, 12, 0.25, 500), 2000);

        assertThat(lenient.orElseThrow().passed()).isTrue();
        assertThat(strict.orElseThrow().firstHard().orElseThrow().code()).isEqualTo("qctooquiet");
        assertThat(new String(wav, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
    }
}
