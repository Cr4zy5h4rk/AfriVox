package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.RecordingAsserts.*;
import static com.afrivox.app.domain.RecordingTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordingMapperTest {

    private RecordingMapper recordingMapper;

    @BeforeEach
    void setUp() {
        recordingMapper = new RecordingMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRecordingSample1();
        var actual = recordingMapper.toEntity(recordingMapper.toDto(expected));
        assertRecordingAllPropertiesEquals(expected, actual);
    }
}
