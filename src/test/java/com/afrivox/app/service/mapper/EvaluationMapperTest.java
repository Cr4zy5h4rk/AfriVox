package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.EvaluationAsserts.*;
import static com.afrivox.app.domain.EvaluationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluationMapperTest {

    private EvaluationMapper evaluationMapper;

    @BeforeEach
    void setUp() {
        evaluationMapper = new EvaluationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvaluationSample1();
        var actual = evaluationMapper.toEntity(evaluationMapper.toDto(expected));
        assertEvaluationAllPropertiesEquals(expected, actual);
    }
}
