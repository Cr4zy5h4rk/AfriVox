package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.DownloadLogAsserts.*;
import static com.afrivox.app.domain.DownloadLogTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DownloadLogMapperTest {

    private DownloadLogMapper downloadLogMapper;

    @BeforeEach
    void setUp() {
        downloadLogMapper = new DownloadLogMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDownloadLogSample1();
        var actual = downloadLogMapper.toEntity(downloadLogMapper.toDto(expected));
        assertDownloadLogAllPropertiesEquals(expected, actual);
    }
}
