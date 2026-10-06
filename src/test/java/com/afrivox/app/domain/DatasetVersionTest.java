package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetTestSamples.*;
import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.RecordingTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatasetVersionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DatasetVersion.class);
        DatasetVersion datasetVersion1 = getDatasetVersionSample1();
        DatasetVersion datasetVersion2 = new DatasetVersion();
        assertThat(datasetVersion1).isNotEqualTo(datasetVersion2);

        datasetVersion2.setId(datasetVersion1.getId());
        assertThat(datasetVersion1).isEqualTo(datasetVersion2);

        datasetVersion2 = getDatasetVersionSample2();
        assertThat(datasetVersion1).isNotEqualTo(datasetVersion2);
    }

    @Test
    void datasetTest() {
        DatasetVersion datasetVersion = getDatasetVersionRandomSampleGenerator();
        Dataset datasetBack = getDatasetRandomSampleGenerator();

        datasetVersion.setDataset(datasetBack);
        assertThat(datasetVersion.getDataset()).isEqualTo(datasetBack);

        datasetVersion.dataset(null);
        assertThat(datasetVersion.getDataset()).isNull();
    }

    @Test
    void recordingTest() {
        DatasetVersion datasetVersion = getDatasetVersionRandomSampleGenerator();
        Recording recordingBack = getRecordingRandomSampleGenerator();

        datasetVersion.addRecording(recordingBack);
        assertThat(datasetVersion.getRecordings()).containsOnly(recordingBack);

        datasetVersion.removeRecording(recordingBack);
        assertThat(datasetVersion.getRecordings()).doesNotContain(recordingBack);

        datasetVersion.recordings(new HashSet<>(Set.of(recordingBack)));
        assertThat(datasetVersion.getRecordings()).containsOnly(recordingBack);

        datasetVersion.setRecordings(new HashSet<>());
        assertThat(datasetVersion.getRecordings()).doesNotContain(recordingBack);
    }
}
