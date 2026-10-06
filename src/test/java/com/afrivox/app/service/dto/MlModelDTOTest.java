package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MlModelDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MlModelDTO.class);
        MlModelDTO mlModelDTO1 = new MlModelDTO();
        mlModelDTO1.setId(1L);
        MlModelDTO mlModelDTO2 = new MlModelDTO();
        assertThat(mlModelDTO1).isNotEqualTo(mlModelDTO2);
        mlModelDTO2.setId(mlModelDTO1.getId());
        assertThat(mlModelDTO1).isEqualTo(mlModelDTO2);
        mlModelDTO2.setId(2L);
        assertThat(mlModelDTO1).isNotEqualTo(mlModelDTO2);
        mlModelDTO1.setId(null);
        assertThat(mlModelDTO1).isNotEqualTo(mlModelDTO2);
    }
}
