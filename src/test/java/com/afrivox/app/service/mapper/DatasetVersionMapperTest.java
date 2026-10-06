package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.DatasetVersionAsserts.*;
import static com.afrivox.app.domain.DatasetVersionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetVersionMapperTest {

    private DatasetVersionMapper datasetVersionMapper;

    @BeforeEach
    void setUp() {
        datasetVersionMapper = new DatasetVersionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDatasetVersionSample1();
        var actual = datasetVersionMapper.toEntity(datasetVersionMapper.toDto(expected));
        assertDatasetVersionAllPropertiesEquals(expected, actual);
    }
}
