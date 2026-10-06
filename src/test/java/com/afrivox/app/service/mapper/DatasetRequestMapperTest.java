package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.DatasetRequestAsserts.*;
import static com.afrivox.app.domain.DatasetRequestTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetRequestMapperTest {

    private DatasetRequestMapper datasetRequestMapper;

    @BeforeEach
    void setUp() {
        datasetRequestMapper = new DatasetRequestMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDatasetRequestSample1();
        var actual = datasetRequestMapper.toEntity(datasetRequestMapper.toDto(expected));
        assertDatasetRequestAllPropertiesEquals(expected, actual);
    }
}
