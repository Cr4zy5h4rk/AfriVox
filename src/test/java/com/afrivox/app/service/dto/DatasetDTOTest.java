package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DatasetDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DatasetDTO.class);
        DatasetDTO datasetDTO1 = new DatasetDTO();
        datasetDTO1.setId(1L);
        DatasetDTO datasetDTO2 = new DatasetDTO();
        assertThat(datasetDTO1).isNotEqualTo(datasetDTO2);
        datasetDTO2.setId(datasetDTO1.getId());
        assertThat(datasetDTO1).isEqualTo(datasetDTO2);
        datasetDTO2.setId(2L);
        assertThat(datasetDTO1).isNotEqualTo(datasetDTO2);
        datasetDTO1.setId(null);
        assertThat(datasetDTO1).isNotEqualTo(datasetDTO2);
    }
}
