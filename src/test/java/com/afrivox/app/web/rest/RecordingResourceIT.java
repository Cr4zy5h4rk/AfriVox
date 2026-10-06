package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.RecordingAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.service.RecordingService;
import com.afrivox.app.service.dto.RecordingDTO;
import com.afrivox.app.service.mapper.RecordingMapper;
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
 * Integration tests for the {@link RecordingResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RecordingResourceIT {

    private static final String DEFAULT_AUDIO_PATH = "AAAAAAAAAA";
    private static final String UPDATED_AUDIO_PATH = "BBBBBBBBBB";

    private static final Integer DEFAULT_DURATION_MS = 0;
    private static final Integer UPDATED_DURATION_MS = 1;

    private static final String DEFAULT_SHA_256 = "AAAAAAAAAA";
    private static final String UPDATED_SHA_256 = "BBBBBBBBBB";

    private static final QcStatus DEFAULT_QC_STATUS = QcStatus.PENDING;
    private static final QcStatus UPDATED_QC_STATUS = QcStatus.PASSED;

    private static final Float DEFAULT_QC_SCORE = 1F;
    private static final Float UPDATED_QC_SCORE = 2F;

    private static final String DEFAULT_TRANSCRIPT = "AAAAAAAAAA";
    private static final String UPDATED_TRANSCRIPT = "BBBBBBBBBB";

    private static final RecordingStatus DEFAULT_STATUS = RecordingStatus.SUBMITTED;
    private static final RecordingStatus UPDATED_STATUS = RecordingStatus.VALIDATED;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/recordings";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RecordingRepository recordingRepository;

    @Mock
    private RecordingRepository recordingRepositoryMock;

    @Autowired
    private RecordingMapper recordingMapper;

    @Mock
    private RecordingService recordingServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRecordingMockMvc;

    private Recording recording;

    private Recording insertedRecording;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Recording createEntity() {
        return new Recording()
            .audioPath(DEFAULT_AUDIO_PATH)
            .durationMs(DEFAULT_DURATION_MS)
            .sha256(DEFAULT_SHA_256)
            .qcStatus(DEFAULT_QC_STATUS)
            .qcScore(DEFAULT_QC_SCORE)
            .transcript(DEFAULT_TRANSCRIPT)
            .status(DEFAULT_STATUS)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Recording createUpdatedEntity() {
        return new Recording()
            .audioPath(UPDATED_AUDIO_PATH)
            .durationMs(UPDATED_DURATION_MS)
            .sha256(UPDATED_SHA_256)
            .qcStatus(UPDATED_QC_STATUS)
            .qcScore(UPDATED_QC_SCORE)
            .transcript(UPDATED_TRANSCRIPT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        recording = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRecording != null) {
            recordingRepository.delete(insertedRecording);
            insertedRecording = null;
        }
    }

    @Test
    @Transactional
    void createRecording() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);
        var returnedRecordingDTO = om.readValue(
            restRecordingMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RecordingDTO.class
        );

        // Validate the Recording in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRecording = recordingMapper.toEntity(returnedRecordingDTO);
        assertRecordingUpdatableFieldsEquals(returnedRecording, getPersistedRecording(returnedRecording));

        insertedRecording = returnedRecording;
    }

    @Test
    @Transactional
    void createRecordingWithExistingId() throws Exception {
        // Create the Recording with an existing ID
        recording.setId(1L);
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAudioPathIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recording.setAudioPath(null);

        // Create the Recording, which fails.
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDurationMsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recording.setDurationMs(null);

        // Create the Recording, which fails.
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkQcStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recording.setQcStatus(null);

        // Create the Recording, which fails.
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recording.setStatus(null);

        // Create the Recording, which fails.
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        recording.setCreatedAt(null);

        // Create the Recording, which fails.
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        restRecordingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRecordings() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        // Get all the recordingList
        restRecordingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(recording.getId().intValue())))
            .andExpect(jsonPath("$.[*].audioPath").value(hasItem(DEFAULT_AUDIO_PATH)))
            .andExpect(jsonPath("$.[*].durationMs").value(hasItem(DEFAULT_DURATION_MS)))
            .andExpect(jsonPath("$.[*].sha256").value(hasItem(DEFAULT_SHA_256)))
            .andExpect(jsonPath("$.[*].qcStatus").value(hasItem(DEFAULT_QC_STATUS.toString())))
            .andExpect(jsonPath("$.[*].qcScore").value(hasItem(DEFAULT_QC_SCORE.doubleValue())))
            .andExpect(jsonPath("$.[*].transcript").value(hasItem(DEFAULT_TRANSCRIPT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingsWithEagerRelationshipsIsEnabled() throws Exception {
        when(recordingServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(recordingServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRecordingsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(recordingServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRecordingMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(recordingRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRecording() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        // Get the recording
        restRecordingMockMvc
            .perform(get(ENTITY_API_URL_ID, recording.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(recording.getId().intValue()))
            .andExpect(jsonPath("$.audioPath").value(DEFAULT_AUDIO_PATH))
            .andExpect(jsonPath("$.durationMs").value(DEFAULT_DURATION_MS))
            .andExpect(jsonPath("$.sha256").value(DEFAULT_SHA_256))
            .andExpect(jsonPath("$.qcStatus").value(DEFAULT_QC_STATUS.toString()))
            .andExpect(jsonPath("$.qcScore").value(DEFAULT_QC_SCORE.doubleValue()))
            .andExpect(jsonPath("$.transcript").value(DEFAULT_TRANSCRIPT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRecording() throws Exception {
        // Get the recording
        restRecordingMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRecording() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recording
        Recording updatedRecording = recordingRepository.findById(recording.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRecording are not directly saved in db
        em.detach(updatedRecording);
        updatedRecording
            .audioPath(UPDATED_AUDIO_PATH)
            .durationMs(UPDATED_DURATION_MS)
            .sha256(UPDATED_SHA_256)
            .qcStatus(UPDATED_QC_STATUS)
            .qcScore(UPDATED_QC_SCORE)
            .transcript(UPDATED_TRANSCRIPT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT);
        RecordingDTO recordingDTO = recordingMapper.toDto(updatedRecording);

        restRecordingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, recordingDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingDTO))
            )
            .andExpect(status().isOk());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRecordingToMatchAllProperties(updatedRecording);
    }

    @Test
    @Transactional
    void putNonExistingRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, recordingDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(recordingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRecordingWithPatch() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recording using partial update
        Recording partialUpdatedRecording = new Recording();
        partialUpdatedRecording.setId(recording.getId());

        partialUpdatedRecording
            .audioPath(UPDATED_AUDIO_PATH)
            .durationMs(UPDATED_DURATION_MS)
            .qcScore(UPDATED_QC_SCORE)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT);

        restRecordingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRecording.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRecording))
            )
            .andExpect(status().isOk());

        // Validate the Recording in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRecordingUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRecording, recording),
            getPersistedRecording(recording)
        );
    }

    @Test
    @Transactional
    void fullUpdateRecordingWithPatch() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the recording using partial update
        Recording partialUpdatedRecording = new Recording();
        partialUpdatedRecording.setId(recording.getId());

        partialUpdatedRecording
            .audioPath(UPDATED_AUDIO_PATH)
            .durationMs(UPDATED_DURATION_MS)
            .sha256(UPDATED_SHA_256)
            .qcStatus(UPDATED_QC_STATUS)
            .qcScore(UPDATED_QC_SCORE)
            .transcript(UPDATED_TRANSCRIPT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT);

        restRecordingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRecording.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRecording))
            )
            .andExpect(status().isOk());

        // Validate the Recording in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRecordingUpdatableFieldsEquals(partialUpdatedRecording, getPersistedRecording(partialUpdatedRecording));
    }

    @Test
    @Transactional
    void patchNonExistingRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, recordingDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(recordingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(recordingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRecording() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        recording.setId(longCount.incrementAndGet());

        // Create the Recording
        RecordingDTO recordingDTO = recordingMapper.toDto(recording);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRecordingMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(recordingDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Recording in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRecording() throws Exception {
        // Initialize the database
        insertedRecording = recordingRepository.saveAndFlush(recording);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the recording
        restRecordingMockMvc
            .perform(delete(ENTITY_API_URL_ID, recording.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return recordingRepository.count();
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

    protected Recording getPersistedRecording(Recording recording) {
        return recordingRepository.findById(recording.getId()).orElseThrow();
    }

    protected void assertPersistedRecordingToMatchAllProperties(Recording expectedRecording) {
        assertRecordingAllPropertiesEquals(expectedRecording, getPersistedRecording(expectedRecording));
    }

    protected void assertPersistedRecordingToMatchUpdatableProperties(Recording expectedRecording) {
        assertRecordingAllUpdatablePropertiesEquals(expectedRecording, getPersistedRecording(expectedRecording));
    }
}
