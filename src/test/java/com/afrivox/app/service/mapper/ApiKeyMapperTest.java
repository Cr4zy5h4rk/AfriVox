package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.ApiKeyAsserts.*;
import static com.afrivox.app.domain.ApiKeyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiKeyMapperTest {

    private ApiKeyMapper apiKeyMapper;

    @BeforeEach
    void setUp() {
        apiKeyMapper = new ApiKeyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getApiKeySample1();
        var actual = apiKeyMapper.toEntity(apiKeyMapper.toDto(expected));
        assertApiKeyAllPropertiesEquals(expected, actual);
    }
}
