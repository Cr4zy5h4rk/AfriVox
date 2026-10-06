package com.afrivox.app.domain;

import static com.afrivox.app.domain.CreditTransactionTestSamples.*;
import static com.afrivox.app.domain.DatasetVersionTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CreditTransactionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CreditTransaction.class);
        CreditTransaction creditTransaction1 = getCreditTransactionSample1();
        CreditTransaction creditTransaction2 = new CreditTransaction();
        assertThat(creditTransaction1).isNotEqualTo(creditTransaction2);

        creditTransaction2.setId(creditTransaction1.getId());
        assertThat(creditTransaction1).isEqualTo(creditTransaction2);

        creditTransaction2 = getCreditTransactionSample2();
        assertThat(creditTransaction1).isNotEqualTo(creditTransaction2);
    }

    @Test
    void profileTest() {
        CreditTransaction creditTransaction = getCreditTransactionRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        creditTransaction.setProfile(userProfileBack);
        assertThat(creditTransaction.getProfile()).isEqualTo(userProfileBack);

        creditTransaction.profile(null);
        assertThat(creditTransaction.getProfile()).isNull();
    }

    @Test
    void datasetVersionTest() {
        CreditTransaction creditTransaction = getCreditTransactionRandomSampleGenerator();
        DatasetVersion datasetVersionBack = getDatasetVersionRandomSampleGenerator();

        creditTransaction.setDatasetVersion(datasetVersionBack);
        assertThat(creditTransaction.getDatasetVersion()).isEqualTo(datasetVersionBack);

        creditTransaction.datasetVersion(null);
        assertThat(creditTransaction.getDatasetVersion()).isNull();
    }
}
