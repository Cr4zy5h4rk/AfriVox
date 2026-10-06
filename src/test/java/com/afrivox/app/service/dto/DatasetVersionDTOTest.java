package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DatasetVersionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DatasetVersionDTO.class);
        DatasetVersionDTO datasetVersionDTO1 = new DatasetVersionDTO();
        datasetVersionDTO1.setId(1L);
        DatasetVersionDTO datasetVersionDTO2 = new DatasetVersionDTO();
        assertThat(datasetVersionDTO1).isNotEqualTo(datasetVersionDTO2);
        datasetVersionDTO2.setId(datasetVersionDTO1.getId());
        assertThat(datasetVersionDTO1).isEqualTo(datasetVersionDTO2);
        datasetVersionDTO2.setId(2L);
        assertThat(datasetVersionDTO1).isNotEqualTo(datasetVersionDTO2);
        datasetVersionDTO1.setId(null);
        assertThat(datasetVersionDTO1).isNotEqualTo(datasetVersionDTO2);
    }
}
