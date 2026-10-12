package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.repository.RecordingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Analyse apres coup des WAV sans note : valide, refuse (FAILED, non supprime) ou illisible (reste en attente). */
class QualityBackfillServiceTest {

    private static final int SR = 16_000;

    @TempDir
    Path temp;

    private Path storage;
    private RecordingRepository repository;
    private QualityBackfillService service;

    @BeforeEach
    void setUp() throws IOException {
        storage = Files.createDirectory(temp.resolve("audio"));
        ApplicationProperties properties = new ApplicationProperties();
        properties.getAudio().setStorageDir(storage.toString());
        repository = mock(RecordingRepository.class);
        service = new QualityBackfillService(repository, properties);
    }

    private Recording recording(long id, String path, int durationMs) throws IOException {
        Recording r = new Recording().id(id);
        r.setAudioPath(path);
        r.setDurationMs(durationMs);
        r.setQcStatus(QcStatus.PENDING);
        return r;
    }

    private void write(String name, byte[] content) throws IOException {
        Files.write(storage.resolve(name), content);
    }

    @Test
    void goodAndBadFilesAreSortedAndNothingIsDeleted() throws IOException {
        write("good.wav", TestAudio.wav(TestAudio.speechLike(SR, 2, 0.5, 0.002), SR, 1));
        write("silent.wav", TestAudio.wav(TestAudio.constant(SR * 2, 0), SR, 1));
        Recording good = recording(1, "good.wav", 2000);
        Recording silent = recording(2, "silent.wav", 2000);
        when(repository.findPendingWav(any())).thenReturn(List.of(good, silent));

        QualityBackfillService.Result result = service.run(100);

        assertThat(result.analyzed()).isEqualTo(2);
        assertThat(result.passed()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(good.getQcStatus()).isEqualTo(QcStatus.PASSED);
        assertThat(good.getQcScore()).isGreaterThan(95f);
        assertThat(silent.getQcStatus()).isEqualTo(QcStatus.FAILED);
        assertThat(silent.getQcReport()).contains("qcsilent");
        // Le fichier du refuse reste sur le disque : on le marque, on ne le supprime pas
        assertThat(Files.exists(storage.resolve("silent.wav"))).isTrue();
    }

    @Test
    void unreadableFilesStayPendingAndGetAMarkerSoTheyAreNotRetriedForever() throws IOException {
        byte[] floatWav = TestAudio.pcm16Wav(2000, 1);
        floatWav[20] = 3; // WAV flottant : non analysable
        write("float.wav", floatWav);
        Recording notPcm = recording(1, "float.wav", 125);
        Recording missing = recording(2, "absent.wav", 2000);
        when(repository.findPendingWav(any())).thenReturn(List.of(notPcm, missing));

        QualityBackfillService.Result result = service.run(100);

        assertThat(result.unanalyzable()).isEqualTo(2);
        assertThat(notPcm.getQcStatus()).isEqualTo(QcStatus.PENDING);
        assertThat(notPcm.getQcReport()).contains("unanalyzable");
        assertThat(missing.getQcReport()).contains("unanalyzable");
    }

    @Test
    void pathsOutsideTheStorageFolderAreNeverRead() throws IOException {
        // Un vrai WAV valide, hors du dossier de stockage : atteignable par « ../ » si le chemin n'etait pas controle
        Files.write(temp.resolve("outside.wav"), TestAudio.wav(TestAudio.speechLike(SR, 2, 0.5, 0.002), SR, 1));
        Recording escaping = recording(1, "../outside.wav", 2000);
        when(repository.findPendingWav(any())).thenReturn(List.of(escaping));

        QualityBackfillService.Result result = service.run(100);

        assertThat(result.unanalyzable()).isEqualTo(1);
        assertThat(escaping.getQcStatus()).isEqualTo(QcStatus.PENDING);
        assertThat(escaping.getQcScore()).isNull();
    }

    @Test
    void emptyBatchIsANoOp() {
        when(repository.findPendingWav(any())).thenReturn(List.of());

        QualityBackfillService.Result result = service.run(100);

        assertThat(result).isEqualTo(new QualityBackfillService.Result(0, 0, 0, 0, 0));
    }

    @Test
    void summaryCountsEachStatus() {
        when(repository.countByQcStatus(QcStatus.PENDING)).thenReturn(5L);
        when(repository.countByQcStatus(QcStatus.PASSED)).thenReturn(90L);
        when(repository.countByQcStatus(QcStatus.FAILED)).thenReturn(2L);

        assertThat(service.summary()).isEqualTo(new QualityBackfillService.Summary(5, 90, 2));
    }
}
