package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.DatasetRequestAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.afrivox.app.repository.DatasetRequestRepository;
import com.afrivox.app.service.DatasetRequestService;
import com.afrivox.app.service.dto.DatasetRequestDTO;
import com.afrivox.app.service.mapper.DatasetRequestMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link DatasetRequestResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DatasetRequestResourceIT {

    private static final String DEFAULT_ORG_NAME = "AAAAAAAAAA";
    private static final String UPDATED_ORG_NAME = "BBBBBBBBBB";

    private static final Sector DEFAULT_SECTOR = Sector.BANK;
    private static final Sector UPDATED_SECTOR = Sector.TELECOM;

    private static final Integer DEFAULT_HOURS = 1;
    private static final Integer UPDATED_HOURS = 2;

    private static final Integer DEFAULT_SPEAKERS_TARGET = 1;
    private static final Integer UPDATED_SPEAKERS_TARGET = 2;

    private static final String DEFAULT_CONVERSATION_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_CONVERSATION_TYPE = "BBBBBBBBBB";

    private static final UsageKind DEFAULT_USAGE_KIND = UsageKind.COMMERCIAL;
    private static final UsageKind UPDATED_USAGE_KIND = UsageKind.RESEARCH;

    private static final LocalDate DEFAULT_DEADLINE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DEADLINE = LocalDate.parse("2026-10-05");

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final RequestStatus DEFAULT_STATUS = RequestStatus.RECEIVED;
    private static final RequestStatus UPDATED_STATUS = RequestStatus.IN_REVIEW;

    private static final Integer DEFAULT_QUOTE_AMOUNT_FCFA = 0;
    private static final Integer UPDATED_QUOTE_AMOUNT_FCFA = 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/dataset-requests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DatasetRequestRepository datasetRequestRepository;

    @Mock
    private DatasetRequestRepository datasetRequestRepositoryMock;

    @Autowired
    private DatasetRequestMapper datasetRequestMapper;

    @Mock
    private DatasetRequestService datasetRequestServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDatasetRequestMockMvc;

    private DatasetRequest datasetRequest;

    private DatasetRequest insertedDatasetRequest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DatasetRequest createEntity() {
        return new DatasetRequest()
            .orgName(DEFAULT_ORG_NAME)
            .sector(DEFAULT_SECTOR)
            .hours(DEFAULT_HOURS)
            .speakersTarget(DEFAULT_SPEAKERS_TARGET)
            .conversationType(DEFAULT_CONVERSATION_TYPE)
            .usageKind(DEFAULT_USAGE_KIND)
            .deadline(DEFAULT_DEADLINE)
            .description(DEFAULT_DESCRIPTION)
            .status(DEFAULT_STATUS)
            .quoteAmountFcfa(DEFAULT_QUOTE_AMOUNT_FCFA)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DatasetRequest createUpdatedEntity() {
        return new DatasetRequest()
            .orgName(UPDATED_ORG_NAME)
            .sector(UPDATED_SECTOR)
            .hours(UPDATED_HOURS)
            .speakersTarget(UPDATED_SPEAKERS_TARGET)
            .conversationType(UPDATED_CONVERSATION_TYPE)
            .usageKind(UPDATED_USAGE_KIND)
            .deadline(UPDATED_DEADLINE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .quoteAmountFcfa(UPDATED_QUOTE_AMOUNT_FCFA)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        datasetRequest = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDatasetRequest != null) {
            datasetRequestRepository.delete(insertedDatasetRequest);
            insertedDatasetRequest = null;
        }
    }

    @Test
    @Transactional
    void createDatasetRequest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);
        var returnedDatasetRequestDTO = om.readValue(
            restDatasetRequestMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DatasetRequestDTO.class
        );

        // Validate the DatasetRequest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDatasetRequest = datasetRequestMapper.toEntity(returnedDatasetRequestDTO);
        assertDatasetRequestUpdatableFieldsEquals(returnedDatasetRequest, getPersistedDatasetRequest(returnedDatasetRequest));

        insertedDatasetRequest = returnedDatasetRequest;
    }

    @Test
    @Transactional
    void createDatasetRequestWithExistingId() throws Exception {
        // Create the DatasetRequest with an existing ID
        datasetRequest.setId(1L);
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkOrgNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setOrgName(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSectorIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setSector(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHoursIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setHours(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUsageKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setUsageKind(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setStatus(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetRequest.setCreatedAt(null);

        // Create the DatasetRequest, which fails.
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        restDatasetRequestMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDatasetRequests() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        // Get all the datasetRequestList
        restDatasetRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(datasetRequest.getId().intValue())))
            .andExpect(jsonPath("$.[*].orgName").value(hasItem(DEFAULT_ORG_NAME)))
            .andExpect(jsonPath("$.[*].sector").value(hasItem(DEFAULT_SECTOR.toString())))
            .andExpect(jsonPath("$.[*].hours").value(hasItem(DEFAULT_HOURS)))
            .andExpect(jsonPath("$.[*].speakersTarget").value(hasItem(DEFAULT_SPEAKERS_TARGET)))
            .andExpect(jsonPath("$.[*].conversationType").value(hasItem(DEFAULT_CONVERSATION_TYPE)))
            .andExpect(jsonPath("$.[*].usageKind").value(hasItem(DEFAULT_USAGE_KIND.toString())))
            .andExpect(jsonPath("$.[*].deadline").value(hasItem(DEFAULT_DEADLINE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].quoteAmountFcfa").value(hasItem(DEFAULT_QUOTE_AMOUNT_FCFA)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetRequestsWithEagerRelationshipsIsEnabled() throws Exception {
        when(datasetRequestServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetRequestMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(datasetRequestServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetRequestsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(datasetRequestServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetRequestMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(datasetRequestRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDatasetRequest() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        // Get the datasetRequest
        restDatasetRequestMockMvc
            .perform(get(ENTITY_API_URL_ID, datasetRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(datasetRequest.getId().intValue()))
            .andExpect(jsonPath("$.orgName").value(DEFAULT_ORG_NAME))
            .andExpect(jsonPath("$.sector").value(DEFAULT_SECTOR.toString()))
            .andExpect(jsonPath("$.hours").value(DEFAULT_HOURS))
            .andExpect(jsonPath("$.speakersTarget").value(DEFAULT_SPEAKERS_TARGET))
            .andExpect(jsonPath("$.conversationType").value(DEFAULT_CONVERSATION_TYPE))
            .andExpect(jsonPath("$.usageKind").value(DEFAULT_USAGE_KIND.toString()))
            .andExpect(jsonPath("$.deadline").value(DEFAULT_DEADLINE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.quoteAmountFcfa").value(DEFAULT_QUOTE_AMOUNT_FCFA))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingDatasetRequest() throws Exception {
        // Get the datasetRequest
        restDatasetRequestMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDatasetRequest() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetRequest
        DatasetRequest updatedDatasetRequest = datasetRequestRepository.findById(datasetRequest.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDatasetRequest are not directly saved in db
        em.detach(updatedDatasetRequest);
        updatedDatasetRequest
            .orgName(UPDATED_ORG_NAME)
            .sector(UPDATED_SECTOR)
            .hours(UPDATED_HOURS)
            .speakersTarget(UPDATED_SPEAKERS_TARGET)
            .conversationType(UPDATED_CONVERSATION_TYPE)
            .usageKind(UPDATED_USAGE_KIND)
            .deadline(UPDATED_DEADLINE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .quoteAmountFcfa(UPDATED_QUOTE_AMOUNT_FCFA)
            .createdAt(UPDATED_CREATED_AT);
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(updatedDatasetRequest);

        restDatasetRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetRequestDTO))
            )
            .andExpect(status().isOk());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDatasetRequestToMatchAllProperties(updatedDatasetRequest);
    }

    @Test
    @Transactional
    void putNonExistingDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetRequestDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDatasetRequestWithPatch() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetRequest using partial update
        DatasetRequest partialUpdatedDatasetRequest = new DatasetRequest();
        partialUpdatedDatasetRequest.setId(datasetRequest.getId());

        partialUpdatedDatasetRequest
            .sector(UPDATED_SECTOR)
            .hours(UPDATED_HOURS)
            .speakersTarget(UPDATED_SPEAKERS_TARGET)
            .createdAt(UPDATED_CREATED_AT);

        restDatasetRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDatasetRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDatasetRequest))
            )
            .andExpect(status().isOk());

        // Validate the DatasetRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetRequestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDatasetRequest, datasetRequest),
            getPersistedDatasetRequest(datasetRequest)
        );
    }

    @Test
    @Transactional
    void fullUpdateDatasetRequestWithPatch() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetRequest using partial update
        DatasetRequest partialUpdatedDatasetRequest = new DatasetRequest();
        partialUpdatedDatasetRequest.setId(datasetRequest.getId());

        partialUpdatedDatasetRequest
            .orgName(UPDATED_ORG_NAME)
            .sector(UPDATED_SECTOR)
            .hours(UPDATED_HOURS)
            .speakersTarget(UPDATED_SPEAKERS_TARGET)
            .conversationType(UPDATED_CONVERSATION_TYPE)
            .usageKind(UPDATED_USAGE_KIND)
            .deadline(UPDATED_DEADLINE)
            .description(UPDATED_DESCRIPTION)
            .status(UPDATED_STATUS)
            .quoteAmountFcfa(UPDATED_QUOTE_AMOUNT_FCFA)
            .createdAt(UPDATED_CREATED_AT);

        restDatasetRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDatasetRequest.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDatasetRequest))
            )
            .andExpect(status().isOk());

        // Validate the DatasetRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetRequestUpdatableFieldsEquals(partialUpdatedDatasetRequest, getPersistedDatasetRequest(partialUpdatedDatasetRequest));
    }

    @Test
    @Transactional
    void patchNonExistingDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, datasetRequestDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDatasetRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetRequest.setId(longCount.incrementAndGet());

        // Create the DatasetRequest
        DatasetRequestDTO datasetRequestDTO = datasetRequestMapper.toDto(datasetRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetRequestMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(datasetRequestDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DatasetRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDatasetRequest() throws Exception {
        // Initialize the database
        insertedDatasetRequest = datasetRequestRepository.saveAndFlush(datasetRequest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the datasetRequest
        restDatasetRequestMockMvc
            .perform(delete(ENTITY_API_URL_ID, datasetRequest.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return datasetRequestRepository.count();
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

    protected DatasetRequest getPersistedDatasetRequest(DatasetRequest datasetRequest) {
        return datasetRequestRepository.findById(datasetRequest.getId()).orElseThrow();
    }

    protected void assertPersistedDatasetRequestToMatchAllProperties(DatasetRequest expectedDatasetRequest) {
        assertDatasetRequestAllPropertiesEquals(expectedDatasetRequest, getPersistedDatasetRequest(expectedDatasetRequest));
    }

    protected void assertPersistedDatasetRequestToMatchUpdatableProperties(DatasetRequest expectedDatasetRequest) {
        assertDatasetRequestAllUpdatablePropertiesEquals(expectedDatasetRequest, getPersistedDatasetRequest(expectedDatasetRequest));
    }
}
