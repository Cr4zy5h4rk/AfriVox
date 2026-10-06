package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.PromptTestSamples.*;
import static com.afrivox.app.domain.RecordingTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecordingTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Recording.class);
        Recording recording1 = getRecordingSample1();
        Recording recording2 = new Recording();
        assertThat(recording1).isNotEqualTo(recording2);

        recording2.setId(recording1.getId());
        assertThat(recording1).isEqualTo(recording2);

        recording2 = getRecordingSample2();
        assertThat(recording1).isNotEqualTo(recording2);
    }

    @Test
    void promptTest() {
        Recording recording = getRecordingRandomSampleGenerator();
        Prompt promptBack = getPromptRandomSampleGenerator();

        recording.setPrompt(promptBack);
        assertThat(recording.getPrompt()).isEqualTo(promptBack);

        recording.prompt(null);
        assertThat(recording.getPrompt()).isNull();
    }

    @Test
    void contributorTest() {
        Recording recording = getRecordingRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        recording.setContributor(userProfileBack);
        assertThat(recording.getContributor()).isEqualTo(userProfileBack);

        recording.contributor(null);
        assertThat(recording.getContributor()).isNull();
    }

    @Test
    void datasetVersionTest() {
        Recording recording = getRecordingRandomSampleGenerator();
        DatasetVersion datasetVersionBack = getDatasetVersionRandomSampleGenerator();

        recording.addDatasetVersion(datasetVersionBack);
        assertThat(recording.getDatasetVersions()).containsOnly(datasetVersionBack);
        assertThat(datasetVersionBack.getRecordings()).containsOnly(recording);

        recording.removeDatasetVersion(datasetVersionBack);
        assertThat(recording.getDatasetVersions()).doesNotContain(datasetVersionBack);
        assertThat(datasetVersionBack.getRecordings()).doesNotContain(recording);

        recording.datasetVersions(new HashSet<>(Set.of(datasetVersionBack)));
        assertThat(recording.getDatasetVersions()).containsOnly(datasetVersionBack);
        assertThat(datasetVersionBack.getRecordings()).containsOnly(recording);

        recording.setDatasetVersions(new HashSet<>());
        assertThat(recording.getDatasetVersions()).doesNotContain(datasetVersionBack);
        assertThat(datasetVersionBack.getRecordings()).doesNotContain(recording);
    }
}
