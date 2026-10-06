package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetTestSamples.*;
import static com.afrivox.app.domain.LanguageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DatasetTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Dataset.class);
        Dataset dataset1 = getDatasetSample1();
        Dataset dataset2 = new Dataset();
        assertThat(dataset1).isNotEqualTo(dataset2);

        dataset2.setId(dataset1.getId());
        assertThat(dataset1).isEqualTo(dataset2);

        dataset2 = getDatasetSample2();
        assertThat(dataset1).isNotEqualTo(dataset2);
    }

    @Test
    void languageTest() {
        Dataset dataset = getDatasetRandomSampleGenerator();
        Language languageBack = getLanguageRandomSampleGenerator();

        dataset.setLanguage(languageBack);
        assertThat(dataset.getLanguage()).isEqualTo(languageBack);

        dataset.language(null);
        assertThat(dataset.getLanguage()).isNull();
    }
}
