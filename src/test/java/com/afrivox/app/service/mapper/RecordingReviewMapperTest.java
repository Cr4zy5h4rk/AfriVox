package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.RecordingReviewAsserts.*;
import static com.afrivox.app.domain.RecordingReviewTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordingReviewMapperTest {

    private RecordingReviewMapper recordingReviewMapper;

    @BeforeEach
    void setUp() {
        recordingReviewMapper = new RecordingReviewMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRecordingReviewSample1();
        var actual = recordingReviewMapper.toEntity(recordingReviewMapper.toDto(expected));
        assertRecordingReviewAllPropertiesEquals(expected, actual);
    }
}
