package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.DatasetAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.service.DatasetService;
import com.afrivox.app.service.dto.DatasetDTO;
import com.afrivox.app.service.mapper.DatasetMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link DatasetResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DatasetResourceIT {

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_DOMAIN = "AAAAAAAAAA";
    private static final String UPDATED_DOMAIN = "BBBBBBBBBB";

    private static final DatasetPlan DEFAULT_PLAN = DatasetPlan.STARTER;
    private static final DatasetPlan UPDATED_PLAN = DatasetPlan.PLUS;

    private static final String ENTITY_API_URL = "/api/datasets";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DatasetRepository datasetRepository;

    @Mock
    private DatasetRepository datasetRepositoryMock;

    @Autowired
    private DatasetMapper datasetMapper;

    @Mock
    private DatasetService datasetServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDatasetMockMvc;

    private Dataset dataset;

    private Dataset insertedDataset;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dataset createEntity() {
        return new Dataset()
            .slug(DEFAULT_SLUG)
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .domain(DEFAULT_DOMAIN)
            .plan(DEFAULT_PLAN);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dataset createUpdatedEntity() {
        return new Dataset()
            .slug(UPDATED_SLUG)
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .plan(UPDATED_PLAN);
    }

    @BeforeEach
    void initTest() {
        dataset = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDataset != null) {
            datasetRepository.delete(insertedDataset);
            insertedDataset = null;
        }
    }

    @Test
    @Transactional
    void createDataset() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);
        var returnedDatasetDTO = om.readValue(
            restDatasetMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DatasetDTO.class
        );

        // Validate the Dataset in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDataset = datasetMapper.toEntity(returnedDatasetDTO);
        assertDatasetUpdatableFieldsEquals(returnedDataset, getPersistedDataset(returnedDataset));

        insertedDataset = returnedDataset;
    }

    @Test
    @Transactional
    void createDatasetWithExistingId() throws Exception {
        // Create the Dataset with an existing ID
        dataset.setId(1L);
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDatasetMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dataset.setSlug(null);

        // Create the Dataset, which fails.
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        restDatasetMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dataset.setTitle(null);

        // Create the Dataset, which fails.
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        restDatasetMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPlanIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        dataset.setPlan(null);

        // Create the Dataset, which fails.
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        restDatasetMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDatasets() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        // Get all the datasetList
        restDatasetMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(dataset.getId().intValue())))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN)))
            .andExpect(jsonPath("$.[*].plan").value(hasItem(DEFAULT_PLAN.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetsWithEagerRelationshipsIsEnabled() throws Exception {
        when(datasetServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(datasetServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDatasetsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(datasetServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restDatasetMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(datasetRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDataset() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        // Get the dataset
        restDatasetMockMvc
            .perform(get(ENTITY_API_URL_ID, dataset.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dataset.getId().intValue()))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.domain").value(DEFAULT_DOMAIN))
            .andExpect(jsonPath("$.plan").value(DEFAULT_PLAN.toString()));
    }

    @Test
    @Transactional
    void getNonExistingDataset() throws Exception {
        // Get the dataset
        restDatasetMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDataset() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dataset
        Dataset updatedDataset = datasetRepository.findById(dataset.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDataset are not directly saved in db
        em.detach(updatedDataset);
        updatedDataset.slug(UPDATED_SLUG).title(UPDATED_TITLE).description(UPDATED_DESCRIPTION).domain(UPDATED_DOMAIN).plan(UPDATED_PLAN);
        DatasetDTO datasetDTO = datasetMapper.toDto(updatedDataset);

        restDatasetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO))
            )
            .andExpect(status().isOk());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDatasetToMatchAllProperties(updatedDataset);
    }

    @Test
    @Transactional
    void putNonExistingDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, datasetDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(datasetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDatasetWithPatch() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dataset using partial update
        Dataset partialUpdatedDataset = new Dataset();
        partialUpdatedDataset.setId(dataset.getId());

        partialUpdatedDataset.title(UPDATED_TITLE);

        restDatasetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDataset.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDataset))
            )
            .andExpect(status().isOk());

        // Validate the Dataset in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedDataset, dataset), getPersistedDataset(dataset));
    }

    @Test
    @Transactional
    void fullUpdateDatasetWithPatch() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the dataset using partial update
        Dataset partialUpdatedDataset = new Dataset();
        partialUpdatedDataset.setId(dataset.getId());

        partialUpdatedDataset
            .slug(UPDATED_SLUG)
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .plan(UPDATED_PLAN);

        restDatasetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDataset.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDataset))
            )
            .andExpect(status().isOk());

        // Validate the Dataset in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDatasetUpdatableFieldsEquals(partialUpdatedDataset, getPersistedDataset(partialUpdatedDataset));
    }

    @Test
    @Transactional
    void patchNonExistingDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, datasetDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(datasetDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDataset() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        dataset.setId(longCount.incrementAndGet());

        // Create the Dataset
        DatasetDTO datasetDTO = datasetMapper.toDto(dataset);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDatasetMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(datasetDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Dataset in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDataset() throws Exception {
        // Initialize the database
        insertedDataset = datasetRepository.saveAndFlush(dataset);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the dataset
        restDatasetMockMvc
            .perform(delete(ENTITY_API_URL_ID, dataset.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return datasetRepository.count();
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

    protected Dataset getPersistedDataset(Dataset dataset) {
        return datasetRepository.findById(dataset.getId()).orElseThrow();
    }

    protected void assertPersistedDatasetToMatchAllProperties(Dataset expectedDataset) {
        assertDatasetAllPropertiesEquals(expectedDataset, getPersistedDataset(expectedDataset));
    }

    protected void assertPersistedDatasetToMatchUpdatableProperties(Dataset expectedDataset) {
        assertDatasetAllUpdatablePropertiesEquals(expectedDataset, getPersistedDataset(expectedDataset));
    }
}
