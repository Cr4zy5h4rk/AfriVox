package com.afrivox.app.domain;

import static com.afrivox.app.domain.ApiKeyTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ApiKeyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ApiKey.class);
        ApiKey apiKey1 = getApiKeySample1();
        ApiKey apiKey2 = new ApiKey();
        assertThat(apiKey1).isNotEqualTo(apiKey2);

        apiKey2.setId(apiKey1.getId());
        assertThat(apiKey1).isEqualTo(apiKey2);

        apiKey2 = getApiKeySample2();
        assertThat(apiKey1).isNotEqualTo(apiKey2);
    }

    @Test
    void ownerTest() {
        ApiKey apiKey = getApiKeyRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        apiKey.setOwner(userProfileBack);
        assertThat(apiKey.getOwner()).isEqualTo(userProfileBack);

        apiKey.owner(null);
        assertThat(apiKey.getOwner()).isNull();
    }
}
