package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.DatasetVersionAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.service.DatasetVersionService;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.mapper.DatasetVersionMapper;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link DatasetVersionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DatasetVersionResourceIT {

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final Float DEFAULT_HOURS = 0F;
    private static final Float UPDATED_HOURS = 1F;

    private static final Integer DEFAULT_SPEAKERS = 0;
    private static final Integer UPDATED_SPEAKERS = 1;

    private static final Float DEFAULT_QUALITY_SCORE = 0F;
    private static final Float UPDATED_QUALITY_SCORE = 1F;

    private static final Integer DEFAULT_PRICE_PER_HOUR_FCFA = 0;
    private static final Integer UPDATED_PRICE_PER_HOUR_FCFA = 1;

    private static final LicenseKind DEFAULT_LICENSE = LicenseKind.PROTOTYPE;
    private static final LicenseKind UPDATED_LICENSE = LicenseKind.RESEARCH;

    private static final VersionStatus DEFAULT_STATUS = VersionStatus.DRAFT;
    private static final VersionStatus UPDATED_STATUS = VersionStatus.PUBLISHED;

    private static final String DEFAULT_PACKAGE_PATH = "AAAAAAAAAA";
    private static final String UPDATED_PACKAGE_PATH = "BBBBBBBBBB";

    private static final String DEFAULT_SHA_256 = "AAAAAAAAAA";
    private static final String UPDATED_SHA_256 = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/dataset-versions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DatasetVersionRepository datasetVersionRepository;

    @Mock
    private DatasetVersionRepository datasetVersionRepositoryMock;

    @Autowired
    private DatasetVersionMapper datasetVersionMapper;

    @Mock
    private DatasetVersionService datasetVersionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDatasetVersionMockMvc;

    private DatasetVersion datasetVersion;

    private DatasetVersion insertedDatasetVersion;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DatasetVersion createEntity() {
        return new DatasetVersion()
            .label(DEFAULT_LABEL)
            .hours(DEFAULT_HOURS)
            .speakers(DEFAULT_SPEAKERS)
            .qualityScore(DEFAULT_QUALITY_SCORE)
            .pricePerHourFcfa(DEFAULT_PRICE_PER_HOUR_FCFA)
            .license(DEFAULT_LICENSE)
            .status(DEFAULT_STATUS)
            .packagePath(DEFAULT_PACKAGE_PATH)
            .sha256(DEFAULT_SHA_256)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DatasetVersion createUpdatedEntity() {
        return new DatasetVersion()
            .label(UPDATED_LABEL)
            .hours(UPDATED_HOURS)
            .speakers(UPDATED_SPEAKERS)
            .qualityScore(UPDATED_QUALITY_SCORE)
            .pricePerHourFcfa(UPDATED_PRICE_PER_HOUR_FCFA)
            .license(UPDATED_LICENSE)
            .status(UPDATED_STATUS)
            .packagePath(UPDATED_PACKAGE_PATH)
            .sha256(UPDATED_SHA_256)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        datasetVersion = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDatasetVersion != null) {
            datasetVersionRepository.delete(insertedDatasetVersion);
            insertedDatasetVersion = null;
        }
    }

    @Test
    @Transactional
    void createDatasetVersion() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);
        var returnedDatasetVersionDTO = om.readValue(
            restDatasetVersionMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DatasetVersionDTO.class
        );

        // Validate the DatasetVersion in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDatasetVersion = datasetVersionMapper.toEntity(returnedDatasetVersionDTO);
        assertDatasetVersionUpdatableFieldsEquals(returnedDatasetVersion, getPersistedDatasetVersion(returnedDatasetVersion));

        insertedDatasetVersion = returnedDatasetVersion;
    }

    @Test
    @Transactional
    void createDatasetVersionWithExistingId() throws Exception {
        // Create the DatasetVersion with an existing ID
        datasetVersion.setId(1L);
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDatasetVersionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetVersion.setLabel(null);

        // Create the DatasetVersion, which fails.
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        restDatasetVersionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLicenseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetVersion.setLicense(null);

        // Create the DatasetVersion, which fails.
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        restDatasetVersionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetVersion.setStatus(null);

        // Create the DatasetVersion, which fails.
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        restDatasetVersionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        datasetVersion.setCreatedAt(null);

        // Create the DatasetVersion, which fails.
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        restDatasetVersionMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDatasetVersions() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        // Get all the datasetVersionList
        restDatasetVersionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(datasetVersion.getId().intValue())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].hours").value(hasItem(DEFAULT_HOURS.doubleValue())))
            .andExpect(jsonPath("$.[*].speakers").value(hasItem(DEFAULT_SPEAKERS)))
            .andExpect(jsonPath("$.[*].qualityScore").value(hasItem(DEFAULT_QUALITY_SCORE.doubleValue())))
            .andExpect(jsonPath("$.[*].pricePerHourFcfa").value(hasItem(DEFAULT_PRICE_PER_HOUR_FCFA)))
            .andExpect(jsonPath("$.[*].license").value(hasItem(DEFAULT_LICENSE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].packagePath").value(hasItem(DEFAULT_PACKAGE_PATH)))
            .andExpect(jsonPath("$.[*].sha256").value(hasItem(DEFAULT_SHA_256)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetVersionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(datasetVersionServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetVersionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(datasetVersionServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetVersionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(datasetVersionServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetVersionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(datasetVersionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDatasetVersion() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        // Get the datasetVersion
        restDatasetVersionMockMvc
            .perform(get(ENTITY_API_URL_ID, datasetVersion.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(datasetVersion.getId().intValue()))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.hours").value(DEFAULT_HOURS.doubleValue()))
            .andExpect(jsonPath("$.speakers").value(DEFAULT_SPEAKERS))
            .andExpect(jsonPath("$.qualityScore").value(DEFAULT_QUALITY_SCORE.doubleValue()))
            .andExpect(jsonPath("$.pricePerHourFcfa").value(DEFAULT_PRICE_PER_HOUR_FCFA))
            .andExpect(jsonPath("$.license").value(DEFAULT_LICENSE.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.packagePath").value(DEFAULT_PACKAGE_PATH))
            .andExpect(jsonPath("$.sha256").value(DEFAULT_SHA_256))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingDatasetVersion() throws Exception {
        // Get the datasetVersion
        restDatasetVersionMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDatasetVersion() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetVersion
        DatasetVersion updatedDatasetVersion = datasetVersionRepository.findById(datasetVersion.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDatasetVersion are not directly saved in db
        em.detach(updatedDatasetVersion);
        updatedDatasetVersion
            .label(UPDATED_LABEL)
            .hours(UPDATED_HOURS)
            .speakers(UPDATED_SPEAKERS)
            .qualityScore(UPDATED_QUALITY_SCORE)
            .pricePerHourFcfa(UPDATED_PRICE_PER_HOUR_FCFA)
            .license(UPDATED_LICENSE)
            .status(UPDATED_STATUS)
            .packagePath(UPDATED_PACKAGE_PATH)
            .sha256(UPDATED_SHA_256)
            .createdAt(UPDATED_CREATED_AT);
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(updatedDatasetVersion);

        restDatasetVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetVersionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetVersionDTO))
            )
            .andExpect(status().isOk());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDatasetVersionToMatchAllProperties(updatedDatasetVersion);
    }

    @Test
    @Transactional
    void putNonExistingDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetVersionDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDatasetVersionWithPatch() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetVersion using partial update
        DatasetVersion partialUpdatedDatasetVersion = new DatasetVersion();
        partialUpdatedDatasetVersion.setId(datasetVersion.getId());

        partialUpdatedDatasetVersion
            .hours(UPDATED_HOURS)
            .qualityScore(UPDATED_QUALITY_SCORE)
            .license(UPDATED_LICENSE)
            .status(UPDATED_STATUS)
            .sha256(UPDATED_SHA_256);

        restDatasetVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDatasetVersion.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDatasetVersion))
            )
            .andExpect(status().isOk());

        // Validate the DatasetVersion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetVersionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDatasetVersion, datasetVersion),
            getPersistedDatasetVersion(datasetVersion)
        );
    }

    @Test
    @Transactional
    void fullUpdateDatasetVersionWithPatch() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the datasetVersion using partial update
        DatasetVersion partialUpdatedDatasetVersion = new DatasetVersion();
        partialUpdatedDatasetVersion.setId(datasetVersion.getId());

        partialUpdatedDatasetVersion
            .label(UPDATED_LABEL)
            .hours(UPDATED_HOURS)
            .speakers(UPDATED_SPEAKERS)
            .qualityScore(UPDATED_QUALITY_SCORE)
            .pricePerHourFcfa(UPDATED_PRICE_PER_HOUR_FCFA)
            .license(UPDATED_LICENSE)
            .status(UPDATED_STATUS)
            .packagePath(UPDATED_PACKAGE_PATH)
            .sha256(UPDATED_SHA_256)
            .createdAt(UPDATED_CREATED_AT);

        restDatasetVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDatasetVersion.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDatasetVersion))
            )
            .andExpect(status().isOk());

        // Validate the DatasetVersion in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetVersionUpdatableFieldsEquals(partialUpdatedDatasetVersion, getPersistedDatasetVersion(partialUpdatedDatasetVersion));
    }

    @Test
    @Transactional
    void patchNonExistingDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, datasetVersionDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetVersionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDatasetVersion() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        datasetVersion.setId(longCount.incrementAndGet());

        // Create the DatasetVersion
        DatasetVersionDTO datasetVersionDTO = datasetVersionMapper.toDto(datasetVersion);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetVersionMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(datasetVersionDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DatasetVersion in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDatasetVersion() throws Exception {
        // Initialize the database
        insertedDatasetVersion = datasetVersionRepository.saveAndFlush(datasetVersion);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the datasetVersion
        restDatasetVersionMockMvc
            .perform(delete(ENTITY_API_URL_ID, datasetVersion.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return datasetVersionRepository.count();
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

    protected DatasetVersion getPersistedDatasetVersion(DatasetVersion datasetVersion) {
        return datasetVersionRepository.findById(datasetVersion.getId()).orElseThrow();
    }

    protected void assertPersistedDatasetVersionToMatchAllProperties(DatasetVersion expectedDatasetVersion) {
        assertDatasetVersionAllPropertiesEquals(expectedDatasetVersion, getPersistedDatasetVersion(expectedDatasetVersion));
    }

    protected void assertPersistedDatasetVersionToMatchUpdatableProperties(DatasetVersion expectedDatasetVersion) {
        assertDatasetVersionAllUpdatablePropertiesEquals(expectedDatasetVersion, getPersistedDatasetVersion(expectedDatasetVersion));
    }
}
