package com.afrivox.app.domain;

import static com.afrivox.app.domain.CampaignTestSamples.*;
import static com.afrivox.app.domain.DatasetRequestTestSamples.*;
import static com.afrivox.app.domain.LanguageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CampaignTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Campaign.class);
        Campaign campaign1 = getCampaignSample1();
        Campaign campaign2 = new Campaign();
        assertThat(campaign1).isNotEqualTo(campaign2);

        campaign2.setId(campaign1.getId());
        assertThat(campaign1).isEqualTo(campaign2);

        campaign2 = getCampaignSample2();
        assertThat(campaign1).isNotEqualTo(campaign2);
    }

    @Test
    void languageTest() {
        Campaign campaign = getCampaignRandomSampleGenerator();
        Language languageBack = getLanguageRandomSampleGenerator();

        campaign.setLanguage(languageBack);
        assertThat(campaign.getLanguage()).isEqualTo(languageBack);

        campaign.language(null);
        assertThat(campaign.getLanguage()).isNull();
    }

    @Test
    void requestTest() {
        Campaign campaign = getCampaignRandomSampleGenerator();
        DatasetRequest datasetRequestBack = getDatasetRequestRandomSampleGenerator();

        campaign.setRequest(datasetRequestBack);
        assertThat(campaign.getRequest()).isEqualTo(datasetRequestBack);

        campaign.request(null);
        assertThat(campaign.getRequest()).isNull();
    }
}
