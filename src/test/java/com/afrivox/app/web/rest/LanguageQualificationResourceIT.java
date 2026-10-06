package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.LanguageQualificationAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.LanguageQualification;
import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.service.LanguageQualificationService;
import com.afrivox.app.service.dto.LanguageQualificationDTO;
import com.afrivox.app.service.mapper.LanguageQualificationMapper;
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
 * Integration tests for the {@link LanguageQualificationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class LanguageQualificationResourceIT {

    private static final Float DEFAULT_SCORE = 0F;
    private static final Float UPDATED_SCORE = 1F;

    private static final QualificationStatus DEFAULT_STATUS = QualificationStatus.PENDING;
    private static final QualificationStatus UPDATED_STATUS = QualificationStatus.QUALIFIED;

    private static final Instant DEFAULT_QUALIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_QUALIFIED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/language-qualifications";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LanguageQualificationRepository languageQualificationRepository;

    @Mock
    private LanguageQualificationRepository languageQualificationRepositoryMock;

    @Autowired
    private LanguageQualificationMapper languageQualificationMapper;

    @Mock
    private LanguageQualificationService languageQualificationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLanguageQualificationMockMvc;

    private LanguageQualification languageQualification;

    private LanguageQualification insertedLanguageQualification;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LanguageQualification createEntity() {
        return new LanguageQualification().score(DEFAULT_SCORE).status(DEFAULT_STATUS).qualifiedAt(DEFAULT_QUALIFIED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LanguageQualification createUpdatedEntity() {
        return new LanguageQualification().score(UPDATED_SCORE).status(UPDATED_STATUS).qualifiedAt(UPDATED_QUALIFIED_AT);
    }

    @BeforeEach
    void initTest() {
        languageQualification = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLanguageQualification != null) {
            languageQualificationRepository.delete(insertedLanguageQualification);
            insertedLanguageQualification = null;
        }
    }

    @Test
    @Transactional
    void createLanguageQualification() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);
        var returnedLanguageQualificationDTO = om.readValue(
            restLanguageQualificationMockMvc
                .perform(
                    post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(languageQualificationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LanguageQualificationDTO.class
        );

        // Validate the LanguageQualification in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLanguageQualification = languageQualificationMapper.toEntity(returnedLanguageQualificationDTO);
        assertLanguageQualificationUpdatableFieldsEquals(
            returnedLanguageQualification,
            getPersistedLanguageQualification(returnedLanguageQualification)
        );

        insertedLanguageQualification = returnedLanguageQualification;
    }

    @Test
    @Transactional
    void createLanguageQualificationWithExistingId() throws Exception {
        // Create the LanguageQualification with an existing ID
        languageQualification.setId(1L);
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restLanguageQualificationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(languageQualificationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        languageQualification.setStatus(null);

        // Create the LanguageQualification, which fails.
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        restLanguageQualificationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(languageQualificationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllLanguageQualifications() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        // Get all the languageQualificationList
        restLanguageQualificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(languageQualification.getId().intValue())))
            .andExpect(jsonPath("$.[*].score").value(hasItem(DEFAULT_SCORE.doubleValue())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].qualifiedAt").value(hasItem(DEFAULT_QUALIFIED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLanguageQualificationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(languageQualificationServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restLanguageQualificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(languageQualificationServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLanguageQualificationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(languageQualificationServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restLanguageQualificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(languageQualificationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getLanguageQualification() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        // Get the languageQualification
        restLanguageQualificationMockMvc
            .perform(get(ENTITY_API_URL_ID, languageQualification.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(languageQualification.getId().intValue()))
            .andExpect(jsonPath("$.score").value(DEFAULT_SCORE.doubleValue()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.qualifiedAt").value(DEFAULT_QUALIFIED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingLanguageQualification() throws Exception {
        // Get the languageQualification
        restLanguageQualificationMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLanguageQualification() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the languageQualification
        LanguageQualification updatedLanguageQualification = languageQualificationRepository
            .findById(languageQualification.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedLanguageQualification are not directly saved in db
        em.detach(updatedLanguageQualification);
        updatedLanguageQualification.score(UPDATED_SCORE).status(UPDATED_STATUS).qualifiedAt(UPDATED_QUALIFIED_AT);
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(updatedLanguageQualification);

        restLanguageQualificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, languageQualificationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isOk());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLanguageQualificationToMatchAllProperties(updatedLanguageQualification);
    }

    @Test
    @Transactional
    void putNonExistingLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, languageQualificationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(languageQualificationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateLanguageQualificationWithPatch() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the languageQualification using partial update
        LanguageQualification partialUpdatedLanguageQualification = new LanguageQualification();
        partialUpdatedLanguageQualification.setId(languageQualification.getId());

        partialUpdatedLanguageQualification.score(UPDATED_SCORE).status(UPDATED_STATUS).qualifiedAt(UPDATED_QUALIFIED_AT);

        restLanguageQualificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLanguageQualification.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLanguageQualification))
            )
            .andExpect(status().isOk());

        // Validate the LanguageQualification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLanguageQualificationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedLanguageQualification, languageQualification),
            getPersistedLanguageQualification(languageQualification)
        );
    }

    @Test
    @Transactional
    void fullUpdateLanguageQualificationWithPatch() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the languageQualification using partial update
        LanguageQualification partialUpdatedLanguageQualification = new LanguageQualification();
        partialUpdatedLanguageQualification.setId(languageQualification.getId());

        partialUpdatedLanguageQualification.score(UPDATED_SCORE).status(UPDATED_STATUS).qualifiedAt(UPDATED_QUALIFIED_AT);

        restLanguageQualificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLanguageQualification.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLanguageQualification))
            )
            .andExpect(status().isOk());

        // Validate the LanguageQualification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLanguageQualificationUpdatableFieldsEquals(
            partialUpdatedLanguageQualification,
            getPersistedLanguageQualification(partialUpdatedLanguageQualification)
        );
    }

    @Test
    @Transactional
    void patchNonExistingLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, languageQualificationDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLanguageQualification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        languageQualification.setId(longCount.incrementAndGet());

        // Create the LanguageQualification
        LanguageQualificationDTO languageQualificationDTO = languageQualificationMapper.toDto(languageQualification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLanguageQualificationMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(languageQualificationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the LanguageQualification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteLanguageQualification() throws Exception {
        // Initialize the database
        insertedLanguageQualification = languageQualificationRepository.saveAndFlush(languageQualification);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the languageQualification
        restLanguageQualificationMockMvc
            .perform(delete(ENTITY_API_URL_ID, languageQualification.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return languageQualificationRepository.count();
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

    protected LanguageQualification getPersistedLanguageQualification(LanguageQualification languageQualification) {
        return languageQualificationRepository.findById(languageQualification.getId()).orElseThrow();
    }

    protected void assertPersistedLanguageQualificationToMatchAllProperties(LanguageQualification expectedLanguageQualification) {
        assertLanguageQualificationAllPropertiesEquals(
            expectedLanguageQualification,
            getPersistedLanguageQualification(expectedLanguageQualification)
        );
    }

    protected void assertPersistedLanguageQualificationToMatchUpdatableProperties(LanguageQualification expectedLanguageQualification) {
        assertLanguageQualificationAllUpdatablePropertiesEquals(
            expectedLanguageQualification,
            getPersistedLanguageQualification(expectedLanguageQualification)
        );
    }
}
