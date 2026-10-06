package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DownloadLogDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DownloadLogDTO.class);
        DownloadLogDTO downloadLogDTO1 = new DownloadLogDTO();
        downloadLogDTO1.setId(1L);
        DownloadLogDTO downloadLogDTO2 = new DownloadLogDTO();
        assertThat(downloadLogDTO1).isNotEqualTo(downloadLogDTO2);
        downloadLogDTO2.setId(downloadLogDTO1.getId());
        assertThat(downloadLogDTO1).isEqualTo(downloadLogDTO2);
        downloadLogDTO2.setId(2L);
        assertThat(downloadLogDTO1).isNotEqualTo(downloadLogDTO2);
        downloadLogDTO1.setId(null);
        assertThat(downloadLogDTO1).isNotEqualTo(downloadLogDTO2);
    }
}
