package com.afrivox.app.domain;

import static com.afrivox.app.domain.ConsentTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConsentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Consent.class);
        Consent consent1 = getConsentSample1();
        Consent consent2 = new Consent();
        assertThat(consent1).isNotEqualTo(consent2);

        consent2.setId(consent1.getId());
        assertThat(consent1).isEqualTo(consent2);

        consent2 = getConsentSample2();
        assertThat(consent1).isNotEqualTo(consent2);
    }

    @Test
    void profileTest() {
        Consent consent = getConsentRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        consent.setProfile(userProfileBack);
        assertThat(consent.getProfile()).isEqualTo(userProfileBack);

        consent.profile(null);
        assertThat(consent.getProfile()).isNull();
    }
}
