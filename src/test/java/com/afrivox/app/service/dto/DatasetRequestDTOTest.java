package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DatasetRequestDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DatasetRequestDTO.class);
        DatasetRequestDTO datasetRequestDTO1 = new DatasetRequestDTO();
        datasetRequestDTO1.setId(1L);
        DatasetRequestDTO datasetRequestDTO2 = new DatasetRequestDTO();
        assertThat(datasetRequestDTO1).isNotEqualTo(datasetRequestDTO2);
        datasetRequestDTO2.setId(datasetRequestDTO1.getId());
        assertThat(datasetRequestDTO1).isEqualTo(datasetRequestDTO2);
        datasetRequestDTO2.setId(2L);
        assertThat(datasetRequestDTO1).isNotEqualTo(datasetRequestDTO2);
        datasetRequestDTO1.setId(null);
        assertThat(datasetRequestDTO1).isNotEqualTo(datasetRequestDTO2);
    }
}
