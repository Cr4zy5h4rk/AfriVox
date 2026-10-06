package com.afrivox.app.domain;

import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.EvaluationTestSamples.*;
import static com.afrivox.app.domain.MlModelTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvaluationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Evaluation.class);
        Evaluation evaluation1 = getEvaluationSample1();
        Evaluation evaluation2 = new Evaluation();
        assertThat(evaluation1).isNotEqualTo(evaluation2);

        evaluation2.setId(evaluation1.getId());
        assertThat(evaluation1).isEqualTo(evaluation2);

        evaluation2 = getEvaluationSample2();
        assertThat(evaluation1).isNotEqualTo(evaluation2);
    }

    @Test
    void modelTest() {
        Evaluation evaluation = getEvaluationRandomSampleGenerator();
        MlModel mlModelBack = getMlModelRandomSampleGenerator();

        evaluation.setModel(mlModelBack);
        assertThat(evaluation.getModel()).isEqualTo(mlModelBack);

        evaluation.model(null);
        assertThat(evaluation.getModel()).isNull();
    }

    @Test
    void testSetTest() {
        Evaluation evaluation = getEvaluationRandomSampleGenerator();
        DatasetVersion datasetVersionBack = getDatasetVersionRandomSampleGenerator();

        evaluation.setTestSet(datasetVersionBack);
        assertThat(evaluation.getTestSet()).isEqualTo(datasetVersionBack);

        evaluation.testSet(null);
        assertThat(evaluation.getTestSet()).isNull();
    }
}
