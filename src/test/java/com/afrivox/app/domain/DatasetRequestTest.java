package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetRequestTestSamples.*;
import static com.afrivox.app.domain.LanguageTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DatasetRequestTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DatasetRequest.class);
        DatasetRequest datasetRequest1 = getDatasetRequestSample1();
        DatasetRequest datasetRequest2 = new DatasetRequest();
        assertThat(datasetRequest1).isNotEqualTo(datasetRequest2);

        datasetRequest2.setId(datasetRequest1.getId());
        assertThat(datasetRequest1).isEqualTo(datasetRequest2);

        datasetRequest2 = getDatasetRequestSample2();
        assertThat(datasetRequest1).isNotEqualTo(datasetRequest2);
    }

    @Test
    void languageTest() {
        DatasetRequest datasetRequest = getDatasetRequestRandomSampleGenerator();
        Language languageBack = getLanguageRandomSampleGenerator();

        datasetRequest.setLanguage(languageBack);
        assertThat(datasetRequest.getLanguage()).isEqualTo(languageBack);

        datasetRequest.language(null);
        assertThat(datasetRequest.getLanguage()).isNull();
    }

    @Test
    void requesterTest() {
        DatasetRequest datasetRequest = getDatasetRequestRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        datasetRequest.setRequester(userProfileBack);
        assertThat(datasetRequest.getRequester()).isEqualTo(userProfileBack);

        datasetRequest.requester(null);
        assertThat(datasetRequest.getRequester()).isNull();
    }
}
