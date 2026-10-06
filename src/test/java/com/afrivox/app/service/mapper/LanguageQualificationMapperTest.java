package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.LanguageQualificationAsserts.*;
import static com.afrivox.app.domain.LanguageQualificationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageQualificationMapperTest {

    private LanguageQualificationMapper languageQualificationMapper;

    @BeforeEach
    void setUp() {
        languageQualificationMapper = new LanguageQualificationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getLanguageQualificationSample1();
        var actual = languageQualificationMapper.toEntity(languageQualificationMapper.toDto(expected));
        assertLanguageQualificationAllPropertiesEquals(expected, actual);
    }
}
