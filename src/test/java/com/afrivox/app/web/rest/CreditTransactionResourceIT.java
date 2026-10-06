package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.CreditTransactionAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.domain.enumeration.CreditReason;
import com.afrivox.app.repository.CreditTransactionRepository;
import com.afrivox.app.service.CreditTransactionService;
import com.afrivox.app.service.dto.CreditTransactionDTO;
import com.afrivox.app.service.mapper.CreditTransactionMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link CreditTransactionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CreditTransactionResourceIT {

    private static final Integer DEFAULT_AMOUNT = 1;
    private static final Integer UPDATED_AMOUNT = 2;

    private static final CreditReason DEFAULT_REASON = CreditReason.RECORDING_VALIDATED;
    private static final CreditReason UPDATED_REASON = CreditReason.REVIEW_DONE;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/credit-transactions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepositoryMock;

    @Autowired
    private CreditTransactionMapper creditTransactionMapper;

    @Mock
    private CreditTransactionService creditTransactionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCreditTransactionMockMvc;

    private CreditTransaction creditTransaction;

    private CreditTransaction insertedCreditTransaction;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CreditTransaction createEntity() {
        return new CreditTransaction().amount(DEFAULT_AMOUNT).reason(DEFAULT_REASON).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CreditTransaction createUpdatedEntity() {
        return new CreditTransaction().amount(UPDATED_AMOUNT).reason(UPDATED_REASON).createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        creditTransaction = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCreditTransaction != null) {
            creditTransactionRepository.delete(insertedCreditTransaction);
            insertedCreditTransaction = null;
        }
    }

    @Test
    @Transactional
    void createCreditTransaction() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);
        var returnedCreditTransactionDTO = om.readValue(
            restCreditTransactionMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CreditTransactionDTO.class
        );

        // Validate the CreditTransaction in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCreditTransaction = creditTransactionMapper.toEntity(returnedCreditTransactionDTO);
        assertCreditTransactionUpdatableFieldsEquals(returnedCreditTransaction, getPersistedCreditTransaction(returnedCreditTransaction));

        insertedCreditTransaction = returnedCreditTransaction;
    }

    @Test
    @Transactional
    void createCreditTransactionWithExistingId() throws Exception {
        // Create the CreditTransaction with an existing ID
        creditTransaction.setId(1L);
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCreditTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAmountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        creditTransaction.setAmount(null);

        // Create the CreditTransaction, which fails.
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        restCreditTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        creditTransaction.setReason(null);

        // Create the CreditTransaction, which fails.
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        restCreditTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        creditTransaction.setCreatedAt(null);

        // Create the CreditTransaction, which fails.
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        restCreditTransactionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCreditTransactions() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        // Get all the creditTransactionList
        restCreditTransactionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(creditTransaction.getId().intValue())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT)))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCreditTransactionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(creditTransactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCreditTransactionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(creditTransactionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCreditTransactionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(creditTransactionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCreditTransactionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(creditTransactionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCreditTransaction() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        // Get the creditTransaction
        restCreditTransactionMockMvc
            .perform(get(ENTITY_API_URL_ID, creditTransaction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(creditTransaction.getId().intValue()))
            .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCreditTransaction() throws Exception {
        // Get the creditTransaction
        restCreditTransactionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCreditTransaction() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the creditTransaction
        CreditTransaction updatedCreditTransaction = creditTransactionRepository.findById(creditTransaction.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCreditTransaction are not directly saved in db
        em.detach(updatedCreditTransaction);
        updatedCreditTransaction.amount(UPDATED_AMOUNT).reason(UPDATED_REASON).createdAt(UPDATED_CREATED_AT);
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(updatedCreditTransaction);

        restCreditTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, creditTransactionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(creditTransactionDTO))
            )
            .andExpect(status().isOk());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCreditTransactionToMatchAllProperties(updatedCreditTransaction);
    }

    @Test
    @Transactional
    void putNonExistingCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, creditTransactionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(creditTransactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(creditTransactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCreditTransactionWithPatch() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the creditTransaction using partial update
        CreditTransaction partialUpdatedCreditTransaction = new CreditTransaction();
        partialUpdatedCreditTransaction.setId(creditTransaction.getId());

        partialUpdatedCreditTransaction.amount(UPDATED_AMOUNT).createdAt(UPDATED_CREATED_AT);

        restCreditTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCreditTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCreditTransaction))
            )
            .andExpect(status().isOk());

        // Validate the CreditTransaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCreditTransactionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCreditTransaction, creditTransaction),
            getPersistedCreditTransaction(creditTransaction)
        );
    }

    @Test
    @Transactional
    void fullUpdateCreditTransactionWithPatch() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the creditTransaction using partial update
        CreditTransaction partialUpdatedCreditTransaction = new CreditTransaction();
        partialUpdatedCreditTransaction.setId(creditTransaction.getId());

        partialUpdatedCreditTransaction.amount(UPDATED_AMOUNT).reason(UPDATED_REASON).createdAt(UPDATED_CREATED_AT);

        restCreditTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCreditTransaction.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCreditTransaction))
            )
            .andExpect(status().isOk());

        // Validate the CreditTransaction in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCreditTransactionUpdatableFieldsEquals(
            partialUpdatedCreditTransaction,
            getPersistedCreditTransaction(partialUpdatedCreditTransaction)
        );
    }

    @Test
    @Transactional
    void patchNonExistingCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, creditTransactionDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(creditTransactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(creditTransactionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCreditTransaction() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        creditTransaction.setId(longCount.incrementAndGet());

        // Create the CreditTransaction
        CreditTransactionDTO creditTransactionDTO = creditTransactionMapper.toDto(creditTransaction);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCreditTransactionMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(creditTransactionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CreditTransaction in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCreditTransaction() throws Exception {
        // Initialize the database
        insertedCreditTransaction = creditTransactionRepository.saveAndFlush(creditTransaction);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the creditTransaction
        restCreditTransactionMockMvc
            .perform(delete(ENTITY_API_URL_ID, creditTransaction.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return creditTransactionRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected CreditTransaction getPersistedCreditTransaction(CreditTransaction creditTransaction) {
        return creditTransactionRepository.findById(creditTransaction.getId()).orElseThrow();
    }

    protected void assertPersistedCreditTransactionToMatchAllProperties(CreditTransaction expectedCreditTransaction) {
        assertCreditTransactionAllPropertiesEquals(expectedCreditTransaction, getPersistedCreditTransaction(expectedCreditTransaction));
    }

    protected void assertPersistedCreditTransactionToMatchUpdatableProperties(CreditTransaction expectedCreditTransaction) {
        assertCreditTransactionAllUpdatablePropertiesEquals(
            expectedCreditTransaction,
            getPersistedCreditTransaction(expectedCreditTransaction)
        );
    }
}
