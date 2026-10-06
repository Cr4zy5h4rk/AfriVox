package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.MlModelAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.service.MlModelService;
import com.afrivox.app.service.dto.MlModelDTO;
import com.afrivox.app.service.mapper.MlModelMapper;
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
 * Integration tests for the {@link MlModelResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class MlModelResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final MlTask DEFAULT_TASK = MlTask.ASR;
    private static final MlTask UPDATED_TASK = MlTask.TTS;

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_LICENSE = "AAAAAAAAAA";
    private static final String UPDATED_LICENSE = "BBBBBBBBBB";

    private static final String DEFAULT_URL = "AAAAAAAAAA";
    private static final String UPDATED_URL = "BBBBBBBBBB";

    private static final Integer DEFAULT_DOWNLOADS = 0;
    private static final Integer UPDATED_DOWNLOADS = 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/ml-models";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MlModelRepository mlModelRepository;

    @Mock
    private MlModelRepository mlModelRepositoryMock;

    @Autowired
    private MlModelMapper mlModelMapper;

    @Mock
    private MlModelService mlModelServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMlModelMockMvc;

    private MlModel mlModel;

    private MlModel insertedMlModel;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MlModel createEntity() {
        return new MlModel()
            .name(DEFAULT_NAME)
            .task(DEFAULT_TASK)
            .description(DEFAULT_DESCRIPTION)
            .license(DEFAULT_LICENSE)
            .url(DEFAULT_URL)
            .downloads(DEFAULT_DOWNLOADS)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MlModel createUpdatedEntity() {
        return new MlModel()
            .name(UPDATED_NAME)
            .task(UPDATED_TASK)
            .description(UPDATED_DESCRIPTION)
            .license(UPDATED_LICENSE)
            .url(UPDATED_URL)
            .downloads(UPDATED_DOWNLOADS)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        mlModel = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMlModel != null) {
            mlModelRepository.delete(insertedMlModel);
            insertedMlModel = null;
        }
    }

    @Test
    @Transactional
    void createMlModel() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);
        var returnedMlModelDTO = om.readValue(
            restMlModelMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MlModelDTO.class
        );

        // Validate the MlModel in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMlModel = mlModelMapper.toEntity(returnedMlModelDTO);
        assertMlModelUpdatableFieldsEquals(returnedMlModel, getPersistedMlModel(returnedMlModel));

        insertedMlModel = returnedMlModel;
    }

    @Test
    @Transactional
    void createMlModelWithExistingId() throws Exception {
        // Create the MlModel with an existing ID
        mlModel.setId(1L);
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMlModelMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isBadRequest());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        mlModel.setName(null);

        // Create the MlModel, which fails.
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        restMlModelMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTaskIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        mlModel.setTask(null);

        // Create the MlModel, which fails.
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        restMlModelMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        mlModel.setCreatedAt(null);

        // Create the MlModel, which fails.
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        restMlModelMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMlModels() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        // Get all the mlModelList
        restMlModelMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(mlModel.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].task").value(hasItem(DEFAULT_TASK.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].license").value(hasItem(DEFAULT_LICENSE)))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].downloads").value(hasItem(DEFAULT_DOWNLOADS)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMlModelsWithEagerRelationshipsIsEnabled() throws Exception {
        when(mlModelServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restMlModelMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(mlModelServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllMlModelsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(mlModelServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restMlModelMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(mlModelRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getMlModel() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        // Get the mlModel
        restMlModelMockMvc
            .perform(get(ENTITY_API_URL_ID, mlModel.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(mlModel.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.task").value(DEFAULT_TASK.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.license").value(DEFAULT_LICENSE))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.downloads").value(DEFAULT_DOWNLOADS))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingMlModel() throws Exception {
        // Get the mlModel
        restMlModelMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMlModel() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the mlModel
        MlModel updatedMlModel = mlModelRepository.findById(mlModel.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMlModel are not directly saved in db
        em.detach(updatedMlModel);
        updatedMlModel
            .name(UPDATED_NAME)
            .task(UPDATED_TASK)
            .description(UPDATED_DESCRIPTION)
            .license(UPDATED_LICENSE)
            .url(UPDATED_URL)
            .downloads(UPDATED_DOWNLOADS)
            .createdAt(UPDATED_CREATED_AT);
        MlModelDTO mlModelDTO = mlModelMapper.toDto(updatedMlModel);

        restMlModelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, mlModelDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO))
            )
            .andExpect(status().isOk());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMlModelToMatchAllProperties(updatedMlModel);
    }

    @Test
    @Transactional
    void putNonExistingMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, mlModelDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(mlModelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMlModelWithPatch() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the mlModel using partial update
        MlModel partialUpdatedMlModel = new MlModel();
        partialUpdatedMlModel.setId(mlModel.getId());

        partialUpdatedMlModel.name(UPDATED_NAME).task(UPDATED_TASK).description(UPDATED_DESCRIPTION).createdAt(UPDATED_CREATED_AT);

        restMlModelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMlModel.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMlModel))
            )
            .andExpect(status().isOk());

        // Validate the MlModel in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMlModelUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedMlModel, mlModel), getPersistedMlModel(mlModel));
    }

    @Test
    @Transactional
    void fullUpdateMlModelWithPatch() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the mlModel using partial update
        MlModel partialUpdatedMlModel = new MlModel();
        partialUpdatedMlModel.setId(mlModel.getId());

        partialUpdatedMlModel
            .name(UPDATED_NAME)
            .task(UPDATED_TASK)
            .description(UPDATED_DESCRIPTION)
            .license(UPDATED_LICENSE)
            .url(UPDATED_URL)
            .downloads(UPDATED_DOWNLOADS)
            .createdAt(UPDATED_CREATED_AT);

        restMlModelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMlModel.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMlModel))
            )
            .andExpect(status().isOk());

        // Validate the MlModel in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMlModelUpdatableFieldsEquals(partialUpdatedMlModel, getPersistedMlModel(partialUpdatedMlModel));
    }

    @Test
    @Transactional
    void patchNonExistingMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, mlModelDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(mlModelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(mlModelDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMlModel() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        mlModel.setId(longCount.incrementAndGet());

        // Create the MlModel
        MlModelDTO mlModelDTO = mlModelMapper.toDto(mlModel);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMlModelMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(mlModelDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the MlModel in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMlModel() throws Exception {
        // Initialize the database
        insertedMlModel = mlModelRepository.saveAndFlush(mlModel);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the mlModel
        restMlModelMockMvc
            .perform(delete(ENTITY_API_URL_ID, mlModel.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return mlModelRepository.count();
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

    protected MlModel getPersistedMlModel(MlModel mlModel) {
        return mlModelRepository.findById(mlModel.getId()).orElseThrow();
    }

    protected void assertPersistedMlModelToMatchAllProperties(MlModel expectedMlModel) {
        assertMlModelAllPropertiesEquals(expectedMlModel, getPersistedMlModel(expectedMlModel));
    }

    protected void assertPersistedMlModelToMatchUpdatableProperties(MlModel expectedMlModel) {
        assertMlModelAllUpdatablePropertiesEquals(expectedMlModel, getPersistedMlModel(expectedMlModel));
    }
}
