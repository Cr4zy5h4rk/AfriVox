package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.LanguageTestSamples.*;
import static com.afrivox.app.domain.MlModelTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MlModelTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MlModel.class);
        MlModel mlModel1 = getMlModelSample1();
        MlModel mlModel2 = new MlModel();
        assertThat(mlModel1).isNotEqualTo(mlModel2);

        mlModel2.setId(mlModel1.getId());
        assertThat(mlModel1).isEqualTo(mlModel2);

        mlModel2 = getMlModelSample2();
        assertThat(mlModel1).isNotEqualTo(mlModel2);
    }

    @Test
    void ownerTest() {
        MlModel mlModel = getMlModelRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        mlModel.setOwner(userProfileBack);
        assertThat(mlModel.getOwner()).isEqualTo(userProfileBack);

        mlModel.owner(null);
        assertThat(mlModel.getOwner()).isNull();
    }

    @Test
    void languageTest() {
        MlModel mlModel = getMlModelRandomSampleGenerator();
        Language languageBack = getLanguageRandomSampleGenerator();

        mlModel.setLanguage(languageBack);
        assertThat(mlModel.getLanguage()).isEqualTo(languageBack);

        mlModel.language(null);
        assertThat(mlModel.getLanguage()).isNull();
    }

    @Test
    void trainedOnTest() {
        MlModel mlModel = getMlModelRandomSampleGenerator();
        DatasetVersion datasetVersionBack = getDatasetVersionRandomSampleGenerator();

        mlModel.setTrainedOn(datasetVersionBack);
        assertThat(mlModel.getTrainedOn()).isEqualTo(datasetVersionBack);

        mlModel.trainedOn(null);
        assertThat(mlModel.getTrainedOn()).isNull();
    }
}
