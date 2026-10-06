package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.PromptAsserts.*;
import static com.afrivox.app.domain.PromptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptMapperTest {

    private PromptMapper promptMapper;

    @BeforeEach
    void setUp() {
        promptMapper = new PromptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPromptSample1();
        var actual = promptMapper.toEntity(promptMapper.toDto(expected));
        assertPromptAllPropertiesEquals(expected, actual);
    }
}
