package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.ConsentAsserts.*;
import static com.afrivox.app.domain.ConsentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentMapperTest {

    private ConsentMapper consentMapper;

    @BeforeEach
    void setUp() {
        consentMapper = new ConsentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConsentSample1();
        var actual = consentMapper.toEntity(consentMapper.toDto(expected));
        assertConsentAllPropertiesEquals(expected, actual);
    }
}
