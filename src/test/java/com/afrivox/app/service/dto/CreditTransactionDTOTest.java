package com.afrivox.app.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CreditTransactionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CreditTransactionDTO.class);
        CreditTransactionDTO creditTransactionDTO1 = new CreditTransactionDTO();
        creditTransactionDTO1.setId(1L);
        CreditTransactionDTO creditTransactionDTO2 = new CreditTransactionDTO();
        assertThat(creditTransactionDTO1).isNotEqualTo(creditTransactionDTO2);
        creditTransactionDTO2.setId(creditTransactionDTO1.getId());
        assertThat(creditTransactionDTO1).isEqualTo(creditTransactionDTO2);
        creditTransactionDTO2.setId(2L);
        assertThat(creditTransactionDTO1).isNotEqualTo(creditTransactionDTO2);
        creditTransactionDTO1.setId(null);
        assertThat(creditTransactionDTO1).isNotEqualTo(creditTransactionDTO2);
    }
}
