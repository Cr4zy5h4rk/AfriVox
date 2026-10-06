package com.afrivox.app.domain;

import static com.afrivox.app.domain.CampaignTestSamples.*;
import static com.afrivox.app.domain.PromptTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PromptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Prompt.class);
        Prompt prompt1 = getPromptSample1();
        Prompt prompt2 = new Prompt();
        assertThat(prompt1).isNotEqualTo(prompt2);

        prompt2.setId(prompt1.getId());
        assertThat(prompt1).isEqualTo(prompt2);

        prompt2 = getPromptSample2();
        assertThat(prompt1).isNotEqualTo(prompt2);
    }

    @Test
    void campaignTest() {
        Prompt prompt = getPromptRandomSampleGenerator();
        Campaign campaignBack = getCampaignRandomSampleGenerator();

        prompt.setCampaign(campaignBack);
        assertThat(prompt.getCampaign()).isEqualTo(campaignBack);

        prompt.campaign(null);
        assertThat(prompt.getCampaign()).isNull();
    }
}
