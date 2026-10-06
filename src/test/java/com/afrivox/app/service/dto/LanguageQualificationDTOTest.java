package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LanguageQualificationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LanguageQualificationDTO.class);
        LanguageQualificationDTO languageQualificationDTO1 = new LanguageQualificationDTO();
        languageQualificationDTO1.setId(1L);
        LanguageQualificationDTO languageQualificationDTO2 = new LanguageQualificationDTO();
        assertThat(languageQualificationDTO1).isNotEqualTo(languageQualificationDTO2);
        languageQualificationDTO2.setId(languageQualificationDTO1.getId());
        assertThat(languageQualificationDTO1).isEqualTo(languageQualificationDTO2);
        languageQualificationDTO2.setId(2L);
        assertThat(languageQualificationDTO1).isNotEqualTo(languageQualificationDTO2);
        languageQualificationDTO1.setId(null);
        assertThat(languageQualificationDTO1).isNotEqualTo(languageQualificationDTO2);
    }
}
