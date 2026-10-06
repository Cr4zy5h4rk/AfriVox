package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.RecordingReviewAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.RecordingReview;
import com.afrivox.app.domain.enumeration.Verdict;
import com.afrivox.app.repository.RecordingReviewRepository;
import com.afrivox.app.service.RecordingReviewService;
import com.afrivox.app.service.dto.RecordingReviewDTO;
import com.afrivox.app.service.mapper.RecordingReviewMapper;
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
 * Integration tests for the {@link RecordingReviewResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RecordingReviewResourceIT {

    private static final Verdict DEFAULT_VERDICT = Verdict.ACCEPT;
    private static final Verdict UPDATED_VERDICT = Verdict.REJECT;

    private static final Boolean DEFAULT_AUDIO_OK = false;
    private static final Boolean UPDATED_AUDIO_OK = true;

    private static final Boolean DEFAULT_PRONUNCIATION_OK = false;
    private static final Boolean UPDATED_PRONUNCIATION_OK = true;

    private static final Boolean DEFAULT_TRANSCRIPT_OK = false;
    private static final Boolean UPDATED_TRANSCRIPT_OK = true;

    private static final Boolean DEFAULT_GOLD = false;
    private static final Boolean UPDATED_GOLD = true;

    private static final String DEFAULT_NOTE = "AAAAAAAAAA";
    private static final String UPDATED_NOTE = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/recording-reviews";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RecordingReviewRepository recordingReviewRepository;

    @Mock
    private RecordingReviewRepository recordingReviewRepositoryMock;

    @Autowired
    private RecordingReviewMapper recordingReviewMapper;

    @Mock
    private RecordingReviewService recordingReviewServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRecordingReviewMockMvc;

    private RecordingReview recordingReview;

    private RecordingReview insertedRecordingReview;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingReview createEntity() {
        return new RecordingReview()
            .verdict(DEFAULT_VERDICT)
            .audioOk(DEFAULT_AUDIO_OK)
            .pronunciationOk(DEFAULT_PRONUNCIATION_OK)
            .transcriptOk(DEFAULT_TRANSCRIPT_OK)
            .gold(DEFAULT_GOLD)
            .note(DEFAULT_NOTE)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RecordingReview createUpdatedEntity() {
        return new RecordingReview()
            .verdict(UPDATED_VERDICT)
            .audioOk(UPDATED_AUDIO_OK)
            .pronunciationOk(UPDATED_PRONUNCIATION_OK)
            .transcriptOk(UPDATED_TRANSCRIPT_OK)
            .gold(UPDATED_GOLD)
            .note(UPDATED_NOTE)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        recordingReview = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRecordingReview != null) {
            recordingReviewRepository.delete(insertedRecordingReview);
            insertedRecordingReview = null;
        }
    }

    @Test
    @Transactional
    void createRecordingReview() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);
        var returnedRecordingReviewDTO = om.readValue(
            restRecordingReviewMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingReviewDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RecordingReviewDTO.class
        );

        // Validate the RecordingReview in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRecordingReview = recordingReviewMapper.toEntity(returnedRecordingReviewDTO);
        assertRecordingReviewUpdatableFieldsEquals(returnedRecordingReview, getPersistedRecordingReview(returnedRecordingReview));

        insertedRecordingReview = returnedRecordingReview;
    }

    @Test
    @Transactional
    void createRecordingReviewWithExistingId() throws Exception {
        // Create the RecordingReview with an existing ID
        recordingReview.setId(1L);
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRecordingReviewMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingReviewDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkVerdictIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recordingReview.setVerdict(null);

        // Create the RecordingReview, which fails.
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        restRecordingReviewMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingReviewDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recordingReview.setCreatedAt(null);

        // Create the RecordingReview, which fails.
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        restRecordingReviewMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingReviewDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRecordingReviews() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        // Get all the recordingReviewList
        restRecordingReviewMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(recordingReview.getId().intValue())))
            .andExpect(jsonPath("$.[*].verdict").value(hasItem(DEFAULT_VERDICT.toString())))
            .andExpect(jsonPath("$.[*].audioOk").value(hasItem(DEFAULT_AUDIO_OK)))
            .andExpect(jsonPath("$.[*].pronunciationOk").value(hasItem(DEFAULT_PRONUNCIATION_OK)))
            .andExpect(jsonPath("$.[*].transcriptOk").value(hasItem(DEFAULT_TRANSCRIPT_OK)))
            .andExpect(jsonPath("$.[*].gold").value(hasItem(DEFAULT_GOLD)))
            .andExpect(jsonPath("$.[*].note").value(hasItem(DEFAULT_NOTE)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingReviewsWithEagerRelationshipsIsEnabled() throws Exception {
        when(recordingReviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingReviewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(recordingReviewServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingReviewsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(recordingReviewServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingReviewMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(recordingReviewRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRecordingReview() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        // Get the recordingReview
        restRecordingReviewMockMvc
            .perform(get(ENTITY_API_URL_ID, recordingReview.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(recordingReview.getId().intValue()))
            .andExpect(jsonPath("$.verdict").value(DEFAULT_VERDICT.toString()))
            .andExpect(jsonPath("$.audioOk").value(DEFAULT_AUDIO_OK))
            .andExpect(jsonPath("$.pronunciationOk").value(DEFAULT_PRONUNCIATION_OK))
            .andExpect(jsonPath("$.transcriptOk").value(DEFAULT_TRANSCRIPT_OK))
            .andExpect(jsonPath("$.gold").value(DEFAULT_GOLD))
            .andExpect(jsonPath("$.note").value(DEFAULT_NOTE))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRecordingReview() throws Exception {
        // Get the recordingReview
        restRecordingReviewMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRecordingReview() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recordingReview
        RecordingReview updatedRecordingReview = recordingReviewRepository.findById(recordingReview.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRecordingReview are not directly saved in db
        em.detach(updatedRecordingReview);
        updatedRecordingReview
            .verdict(UPDATED_VERDICT)
            .audioOk(UPDATED_AUDIO_OK)
            .pronunciationOk(UPDATED_PRONUNCIATION_OK)
            .transcriptOk(UPDATED_TRANSCRIPT_OK)
            .gold(UPDATED_GOLD)
            .note(UPDATED_NOTE)
            .createdAt(UPDATED_CREATED_AT);
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(updatedRecordingReview);

        restRecordingReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, recordingReviewDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingReviewDTO))
            )
            .andExpect(status().isOk());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRecordingReviewToMatchAllProperties(updatedRecordingReview);
    }

    @Test
    @Transactional
    void putNonExistingRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, recordingReviewDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingReviewDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRecordingReviewWithPatch() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recordingReview using partial update
        RecordingReview partialUpdatedRecordingReview = new RecordingReview();
        partialUpdatedRecordingReview.setId(recordingReview.getId());

        partialUpdatedRecordingReview.verdict(UPDATED_VERDICT).createdAt(UPDATED_CREATED_AT);

        restRecordingReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRecordingReview.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRecordingReview))
            )
            .andExpect(status().isOk());

        // Validate the RecordingReview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRecordingReviewUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRecordingReview, recordingReview),
            getPersistedRecordingReview(recordingReview)
        );
    }

    @Test
    @Transactional
    void fullUpdateRecordingReviewWithPatch() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recordingReview using partial update
        RecordingReview partialUpdatedRecordingReview = new RecordingReview();
        partialUpdatedRecordingReview.setId(recordingReview.getId());

        partialUpdatedRecordingReview
            .verdict(UPDATED_VERDICT)
            .audioOk(UPDATED_AUDIO_OK)
            .pronunciationOk(UPDATED_PRONUNCIATION_OK)
            .transcriptOk(UPDATED_TRANSCRIPT_OK)
            .gold(UPDATED_GOLD)
            .note(UPDATED_NOTE)
            .createdAt(UPDATED_CREATED_AT);

        restRecordingReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRecordingReview.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRecordingReview))
            )
            .andExpect(status().isOk());

        // Validate the RecordingReview in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRecordingReviewUpdatableFieldsEquals(
            partialUpdatedRecordingReview,
            getPersistedRecordingReview(partialUpdatedRecordingReview)
        );
    }

    @Test
    @Transactional
    void patchNonExistingRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, recordingReviewDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(recordingReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(recordingReviewDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRecordingReview() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recordingReview.setId(longCount.incrementAndGet());

        // Create the RecordingReview
        RecordingReviewDTO recordingReviewDTO = recordingReviewMapper.toDto(recordingReview);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingReviewMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(recordingReviewDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RecordingReview in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRecordingReview() throws Exception {
        // Initialize the database
        insertedRecordingReview = recordingReviewRepository.saveAndFlush(recordingReview);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the recordingReview
        restRecordingReviewMockMvc
            .perform(delete(ENTITY_API_URL_ID, recordingReview.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return recordingReviewRepository.count();
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

    protected RecordingReview getPersistedRecordingReview(RecordingReview recordingReview) {
        return recordingReviewRepository.findById(recordingReview.getId()).orElseThrow();
    }

    protected void assertPersistedRecordingReviewToMatchAllProperties(RecordingReview expectedRecordingReview) {
        assertRecordingReviewAllPropertiesEquals(expectedRecordingReview, getPersistedRecordingReview(expectedRecordingReview));
    }

    protected void assertPersistedRecordingReviewToMatchUpdatableProperties(RecordingReview expectedRecordingReview) {
        assertRecordingReviewAllUpdatablePropertiesEquals(expectedRecordingReview, getPersistedRecordingReview(expectedRecordingReview));
    }
}
