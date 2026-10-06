package com.afrivox.app.service.mapper;

import static com.afrivox.app.domain.CreditTransactionAsserts.*;
import static com.afrivox.app.domain.CreditTransactionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreditTransactionMapperTest {

    private CreditTransactionMapper creditTransactionMapper;

    @BeforeEach
    void setUp() {
        creditTransactionMapper = new CreditTransactionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCreditTransactionSample1();
        var actual = creditTransactionMapper.toEntity(creditTransactionMapper.toDto(expected));
        assertCreditTransactionAllPropertiesEquals(expected, actual);
    }
}
