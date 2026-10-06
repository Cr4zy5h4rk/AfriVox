package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RecordingDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingDTO.class);
        RecordingDTO recordingDTO1 = new RecordingDTO();
        recordingDTO1.setId(1L);
        RecordingDTO recordingDTO2 = new RecordingDTO();
        assertThat(recordingDTO1).isNotEqualTo(recordingDTO2);
        recordingDTO2.setId(recordingDTO1.getId());
        assertThat(recordingDTO1).isEqualTo(recordingDTO2);
        recordingDTO2.setId(2L);
        assertThat(recordingDTO1).isNotEqualTo(recordingDTO2);
        recordingDTO1.setId(null);
        assertThat(recordingDTO1).isNotEqualTo(recordingDTO2);
    }
}
