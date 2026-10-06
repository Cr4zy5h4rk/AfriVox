package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.DatasetAsserts.*;
import static com.afrivox.app.domain.DatasetTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetMapperTest {

    private DatasetMapper datasetMapper;

    @BeforeEach
    void setUp() {
        datasetMapper = new DatasetMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDatasetSample1();
        var actual = datasetMapper.toEntity(datasetMapper.toDto(expected));
        assertDatasetAllPropertiesEquals(expected, actual);
    }
}
