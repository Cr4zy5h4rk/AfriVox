package com.afrivox.app.domain;

import static com.afrivox.app.domain.LanguageQualificationTestSamples.*;
import static com.afrivox.app.domain.LanguageTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LanguageQualificationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(LanguageQualification.class);
        LanguageQualification languageQualification1 = getLanguageQualificationSample1();
        LanguageQualification languageQualification2 = new LanguageQualification();
        assertThat(languageQualification1).isNotEqualTo(languageQualification2);

        languageQualification2.setId(languageQualification1.getId());
        assertThat(languageQualification1).isEqualTo(languageQualification2);

        languageQualification2 = getLanguageQualificationSample2();
        assertThat(languageQualification1).isNotEqualTo(languageQualification2);
    }

    @Test
    void profileTest() {
        LanguageQualification languageQualification = getLanguageQualificationRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        languageQualification.setProfile(userProfileBack);
        assertThat(languageQualification.getProfile()).isEqualTo(userProfileBack);

        languageQualification.profile(null);
        assertThat(languageQualification.getProfile()).isNull();
    }

    @Test
    void languageTest() {
        LanguageQualification languageQualification = getLanguageQualificationRandomSampleGenerator();
        Language languageBack = getLanguageRandomSampleGenerator();

        languageQualification.setLanguage(languageBack);
        assertThat(languageQualification.getLanguage()).isEqualTo(languageBack);

        languageQualification.language(null);
        assertThat(languageQualification.getLanguage()).isNull();
    }
}
