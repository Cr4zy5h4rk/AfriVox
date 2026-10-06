package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.DownloadLogTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DownloadLogTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DownloadLog.class);
        DownloadLog downloadLog1 = getDownloadLogSample1();
        DownloadLog downloadLog2 = new DownloadLog();
        assertThat(downloadLog1).isNotEqualTo(downloadLog2);

        downloadLog2.setId(downloadLog1.getId());
        assertThat(downloadLog1).isEqualTo(downloadLog2);

        downloadLog2 = getDownloadLogSample2();
        assertThat(downloadLog1).isNotEqualTo(downloadLog2);
    }

    @Test
    void datasetVersionTest() {
        DownloadLog downloadLog = getDownloadLogRandomSampleGenerator();
        DatasetVersion datasetVersionBack = getDatasetVersionRandomSampleGenerator();

        downloadLog.setDatasetVersion(datasetVersionBack);
        assertThat(downloadLog.getDatasetVersion()).isEqualTo(datasetVersionBack);

        downloadLog.datasetVersion(null);
        assertThat(downloadLog.getDatasetVersion()).isNull();
    }

    @Test
    void profileTest() {
        DownloadLog downloadLog = getDownloadLogRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        downloadLog.setProfile(userProfileBack);
        assertThat(downloadLog.getProfile()).isEqualTo(userProfileBack);

        downloadLog.profile(null);
        assertThat(downloadLog.getProfile()).isNull();
    }
}
