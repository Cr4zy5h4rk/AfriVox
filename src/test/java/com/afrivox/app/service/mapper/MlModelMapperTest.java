package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.MlModelAsserts.*;
import static com.afrivox.app.domain.MlModelTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MlModelMapperTest {

    private MlModelMapper mlModelMapper;

    @BeforeEach
    void setUp() {
        mlModelMapper = new MlModelMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMlModelSample1();
        var actual = mlModelMapper.toEntity(mlModelMapper.toDto(expected));
        assertMlModelAllPropertiesEquals(expected, actual);
    }
}
