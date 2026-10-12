package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.Consent;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.AgeRange;
import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.domain.enumeration.ConsentKind;
import com.afrivox.app.domain.enumeration.Gender;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.repository.ConsentRepository;
import com.afrivox.app.repository.PromptRepository;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Le controle qualite a l'envoi : refus des defauts bloquants, note enregistree, formats compresses non analyses. */
class RecordingUploadQualityTest {

    private static final int SR = 16_000;

    @TempDir
    Path storage;

    private ApplicationProperties properties;
    private RecordingRepository recordings;
    private RecordingUploadService service;

    @BeforeEach
    void setUp() {
        properties = new ApplicationProperties();
        properties.getAudio().setStorageDir(storage.toString());
        recordings = mock(RecordingRepository.class);
        PromptRepository prompts = mock(PromptRepository.class);
        ConsentRepository consents = mock(ConsentRepository.class);
        UserProfileService profileService = mock(UserProfileService.class);

        Campaign campaign = new Campaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setKind(CampaignKind.OPEN);
        Prompt prompt = new Prompt().id(1L);
        prompt.setCampaign(campaign);
        when(prompts.findById(1L)).thenReturn(Optional.of(prompt));

        UserProfile profile = new UserProfile().id(5L);
        profile.setDemographicsAnsweredAt(Instant.now());
        profile.setAgeRange(AgeRange.R18_25);
        profile.setGender(Gender.FEMALE);
        profile.setCountry("SN");
        profile.setRegion("Dakar");
        when(profileService.getOrCreateCurrent()).thenReturn(profile);
        when(consents.findFirstByProfileIdAndKindAndRevokedAtIsNull(5L, ConsentKind.COLLECTION)).thenReturn(Optional.of(new Consent()));
        when(recordings.save(any(Recording.class))).thenAnswer(call -> call.getArgument(0));

        service = new RecordingUploadService(
            properties,
            recordings,
            prompts,
            consents,
            profileService,
            mock(ContributorTrialService.class),
            mock(CreditService.class)
        );
    }

    private static byte[] speech(double seconds, double amplitude) {
        return TestAudio.wav(TestAudio.speechLike(SR, seconds, amplitude, 0.002), SR, 1);
    }

    private long storedFiles() throws IOException {
        try (Stream<Path> files = Files.walk(storage)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private static void assertRejected(Runnable upload, String errorKey) {
        assertThatThrownBy(upload::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(errorKey)
        );
    }

    @Test
    void goodWavIsPassedWithAScoreAndAReport() throws IOException {
        Recording saved = service.upload(1L, 3000, speech(3, 0.5));

        assertThat(saved.getQcStatus()).isEqualTo(QcStatus.PASSED);
        assertThat(saved.getQcScore()).isGreaterThan(95f);
        assertThat(saved.getQcReport()).contains("\"sr\":16000").contains("\"issues\":[]");
        assertThat(saved.getAudioPath()).endsWith(".wav");
        assertThat(storedFiles()).isEqualTo(1);
    }

    @Test
    void measuredDurationReplacesTheOneTheClientDeclared() {
        // Le client annonce 3 s, le fichier fait 3,15 s (dans la tolerance) : la valeur du fichier est conservee
        Recording saved = service.upload(1L, 3000, TestAudio.wav(TestAudio.speechLike(SR, 3.15, 0.5, 0.002), SR, 1));

        assertThat(saved.getDurationMs()).isEqualTo(3150);
    }

    @Test
    void silentRecordingIsRefusedAndNothingIsStored() throws IOException {
        byte[] silent = TestAudio.wav(TestAudio.constant(SR * 3, 0), SR, 1);

        assertRejected(() -> service.upload(1L, 3000, silent), "qcsilent");

        verify(recordings, never()).save(any());
        assertThat(storedFiles()).isZero();
    }

    @Test
    void tooQuietRecordingIsRefused() {
        assertRejected(() -> service.upload(1L, 3000, speech(3, 0.005)), "qctooquiet");
    }

    @Test
    void recordingWhoseLengthContradictsTheDeclaredOneIsRefused() {
        assertRejected(() -> service.upload(1L, 8000, speech(2, 0.5)), "qcmismatch");
    }

    @Test
    void measuredDurationBelowTheMinimumIsRefused() {
        // Declare 600 ms (accepte par le controle d'entree) mais le fichier ne fait que 300 ms
        byte[] tooShort = TestAudio.wav(TestAudio.speechLike(SR, 0.3, 0.5, 0.002), SR, 1);

        assertRejected(() -> service.upload(1L, 600, tooShort), "invalidduration");
    }

    @Test
    void compressedFormatsAreNotAnalysedAndStayPending() {
        byte[] webm = new byte[4000];
        webm[0] = 0x1A;
        webm[1] = 0x45;
        webm[2] = (byte) 0xDF;
        webm[3] = (byte) 0xA3;
        new java.util.Random(5).nextBytes(java.util.Arrays.copyOfRange(webm, 4, 4000));

        Recording saved = service.upload(1L, 3000, webm);

        assertThat(saved.getQcStatus()).isEqualTo(QcStatus.PENDING);
        assertThat(saved.getQcScore()).isNull();
        assertThat(saved.getQcReport()).isNull();
        assertThat(saved.getDurationMs()).isEqualTo(3000);
    }

    @Test
    void controlsCanBeSwitchedOff() {
        properties.getAudio().getQc().setEnabled(false);
        byte[] silent = TestAudio.wav(TestAudio.constant(SR * 3, 0), SR, 1);

        Recording saved = service.upload(1L, 3000, silent);

        assertThat(saved.getQcStatus()).isEqualTo(QcStatus.PENDING);
        assertThat(saved.getQcReport()).isNull();
    }

    @Test
    void lightDefectsDoNotBlockTheUpload() {
        // Pauses bruyantes : signalees « noisy » dans le rapport, note abaissee, mais envoi accepte
        byte[] noisy = TestAudio.wav(TestAudio.speechLike(SR, 3, 0.4, 0.2), SR, 1);

        Recording saved = service.upload(1L, 3000, noisy);

        assertThat(saved.getQcStatus()).isEqualTo(QcStatus.PASSED);
        assertThat(saved.getQcReport()).contains("noisy");
        assertThat(saved.getQcScore()).isLessThan(90f);
    }
}
