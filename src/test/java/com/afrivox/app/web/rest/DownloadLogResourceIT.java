package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.DownloadLogAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.service.DownloadLogService;
import com.afrivox.app.service.dto.DownloadLogDTO;
import com.afrivox.app.service.mapper.DownloadLogMapper;
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
 * Integration tests for the {@link DownloadLogResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class DownloadLogResourceIT {

    private static final Instant DEFAULT_DOWNLOADED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DOWNLOADED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String DEFAULT_IP = "AAAAAAAAAA";
    private static final String UPDATED_IP = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/download-logs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private DownloadLogRepository downloadLogRepository;

    @Mock
    private DownloadLogRepository downloadLogRepositoryMock;

    @Autowired
    private DownloadLogMapper downloadLogMapper;

    @Mock
    private DownloadLogService downloadLogServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restDownloadLogMockMvc;

    private DownloadLog downloadLog;

    private DownloadLog insertedDownloadLog;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DownloadLog createEntity() {
        return new DownloadLog().downloadedAt(DEFAULT_DOWNLOADED_AT).ip(DEFAULT_IP);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static DownloadLog createUpdatedEntity() {
        return new DownloadLog().downloadedAt(UPDATED_DOWNLOADED_AT).ip(UPDATED_IP);
    }

    @BeforeEach
    void initTest() {
        downloadLog = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedDownloadLog != null) {
            downloadLogRepository.delete(insertedDownloadLog);
            insertedDownloadLog = null;
        }
    }

    @Test
    @Transactional
    void createDownloadLog() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);
        var returnedDownloadLogDTO = om.readValue(
            restDownloadLogMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(downloadLogDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            DownloadLogDTO.class
        );

        // Validate the DownloadLog in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedDownloadLog = downloadLogMapper.toEntity(returnedDownloadLogDTO);
        assertDownloadLogUpdatableFieldsEquals(returnedDownloadLog, getPersistedDownloadLog(returnedDownloadLog));

        insertedDownloadLog = returnedDownloadLog;
    }

    @Test
    @Transactional
    void createDownloadLogWithExistingId() throws Exception {
        // Create the DownloadLog with an existing ID
        downloadLog.setId(1L);
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restDownloadLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(downloadLogDTO)))
            .andExpect(status().isBadRequest());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDownloadedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        downloadLog.setDownloadedAt(null);

        // Create the DownloadLog, which fails.
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        restDownloadLogMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(downloadLogDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllDownloadLogs() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        // Get all the downloadLogList
        restDownloadLogMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(downloadLog.getId().intValue())))
            .andExpect(jsonPath("$.[*].downloadedAt").value(hasItem(DEFAULT_DOWNLOADED_AT.toString())))
            .andExpect(jsonPath("$.[*].ip").value(hasItem(DEFAULT_IP)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDownloadLogsWithEagerRelationshipsIsEnabled() throws Exception {
        when(downloadLogServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDownloadLogMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(downloadLogServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllDownloadLogsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(downloadLogServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restDownloadLogMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(downloadLogRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getDownloadLog() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        // Get the downloadLog
        restDownloadLogMockMvc
            .perform(get(ENTITY_API_URL_ID, downloadLog.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(downloadLog.getId().intValue()))
            .andExpect(jsonPath("$.downloadedAt").value(DEFAULT_DOWNLOADED_AT.toString()))
            .andExpect(jsonPath("$.ip").value(DEFAULT_IP));
    }

    @Test
    @Transactional
    void getNonExistingDownloadLog() throws Exception {
        // Get the downloadLog
        restDownloadLogMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingDownloadLog() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the downloadLog
        DownloadLog updatedDownloadLog = downloadLogRepository.findById(downloadLog.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedDownloadLog are not directly saved in db
        em.detach(updatedDownloadLog);
        updatedDownloadLog.downloadedAt(UPDATED_DOWNLOADED_AT).ip(UPDATED_IP);
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(updatedDownloadLog);

        restDownloadLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, downloadLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(downloadLogDTO))
            )
            .andExpect(status().isOk());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedDownloadLogToMatchAllProperties(updatedDownloadLog);
    }

    @Test
    @Transactional
    void putNonExistingDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, downloadLogDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(downloadLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(downloadLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(downloadLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateDownloadLogWithPatch() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the downloadLog using partial update
        DownloadLog partialUpdatedDownloadLog = new DownloadLog();
        partialUpdatedDownloadLog.setId(downloadLog.getId());

        partialUpdatedDownloadLog.downloadedAt(UPDATED_DOWNLOADED_AT);

        restDownloadLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDownloadLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDownloadLog))
            )
            .andExpect(status().isOk());

        // Validate the DownloadLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDownloadLogUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedDownloadLog, downloadLog),
            getPersistedDownloadLog(downloadLog)
        );
    }

    @Test
    @Transactional
    void fullUpdateDownloadLogWithPatch() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the downloadLog using partial update
        DownloadLog partialUpdatedDownloadLog = new DownloadLog();
        partialUpdatedDownloadLog.setId(downloadLog.getId());

        partialUpdatedDownloadLog.downloadedAt(UPDATED_DOWNLOADED_AT).ip(UPDATED_IP);

        restDownloadLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedDownloadLog.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedDownloadLog))
            )
            .andExpect(status().isOk());

        // Validate the DownloadLog in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertDownloadLogUpdatableFieldsEquals(partialUpdatedDownloadLog, getPersistedDownloadLog(partialUpdatedDownloadLog));
    }

    @Test
    @Transactional
    void patchNonExistingDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, downloadLogDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(downloadLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(downloadLogDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamDownloadLog() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        downloadLog.setId(longCount.incrementAndGet());

        // Create the DownloadLog
        DownloadLogDTO downloadLogDTO = downloadLogMapper.toDto(downloadLog);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restDownloadLogMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(downloadLogDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the DownloadLog in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteDownloadLog() throws Exception {
        // Initialize the database
        insertedDownloadLog = downloadLogRepository.saveAndFlush(downloadLog);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the downloadLog
        restDownloadLogMockMvc
            .perform(delete(ENTITY_API_URL_ID, downloadLog.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return downloadLogRepository.count();
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

    protected DownloadLog getPersistedDownloadLog(DownloadLog downloadLog) {
        return downloadLogRepository.findById(downloadLog.getId()).orElseThrow();
    }

    protected void assertPersistedDownloadLogToMatchAllProperties(DownloadLog expectedDownloadLog) {
        assertDownloadLogAllPropertiesEquals(expectedDownloadLog, getPersistedDownloadLog(expectedDownloadLog));
    }

    protected void assertPersistedDownloadLogToMatchUpdatableProperties(DownloadLog expectedDownloadLog) {
        assertDownloadLogAllUpdatablePropertiesEquals(expectedDownloadLog, getPersistedDownloadLog(expectedDownloadLog));
    }
}
