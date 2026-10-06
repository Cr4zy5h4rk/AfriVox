package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RecordingReviewDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingReviewDTO.class);
        RecordingReviewDTO recordingReviewDTO1 = new RecordingReviewDTO();
        recordingReviewDTO1.setId(1L);
        RecordingReviewDTO recordingReviewDTO2 = new RecordingReviewDTO();
        assertThat(recordingReviewDTO1).isNotEqualTo(recordingReviewDTO2);
        recordingReviewDTO2.setId(recordingReviewDTO1.getId());
        assertThat(recordingReviewDTO1).isEqualTo(recordingReviewDTO2);
        recordingReviewDTO2.setId(2L);
        assertThat(recordingReviewDTO1).isNotEqualTo(recordingReviewDTO2);
        recordingReviewDTO1.setId(null);
        assertThat(recordingReviewDTO1).isNotEqualTo(recordingReviewDTO2);
    }
}
