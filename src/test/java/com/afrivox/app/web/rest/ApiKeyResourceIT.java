package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.ApiKeyAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.ApiKey;
import com.afrivox.app.repository.ApiKeyRepository;
import com.afrivox.app.service.ApiKeyService;
import com.afrivox.app.service.dto.ApiKeyDTO;
import com.afrivox.app.service.mapper.ApiKeyMapper;
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
 * Integration tests for the {@link ApiKeyResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ApiKeyResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_PREFIX = "AAAAAAAAAA";
    private static final String UPDATED_PREFIX = "BBBBBBBBBB";

    private static final String DEFAULT_KEY_HASH = "AAAAAAAAAA";
    private static final String UPDATED_KEY_HASH = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final Instant DEFAULT_REVOKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOKED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/api-keys";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyRepository apiKeyRepositoryMock;

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    @Mock
    private ApiKeyService apiKeyServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restApiKeyMockMvc;

    private ApiKey apiKey;

    private ApiKey insertedApiKey;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ApiKey createEntity() {
        return new ApiKey()
            .name(DEFAULT_NAME)
            .prefix(DEFAULT_PREFIX)
            .keyHash(DEFAULT_KEY_HASH)
            .createdAt(DEFAULT_CREATED_AT)
            .revokedAt(DEFAULT_REVOKED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ApiKey createUpdatedEntity() {
        return new ApiKey()
            .name(UPDATED_NAME)
            .prefix(UPDATED_PREFIX)
            .keyHash(UPDATED_KEY_HASH)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT);
    }

    @BeforeEach
    void initTest() {
        apiKey = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedApiKey != null) {
            apiKeyRepository.delete(insertedApiKey);
            insertedApiKey = null;
        }
    }

    @Test
    @Transactional
    void createApiKey() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);
        var returnedApiKeyDTO = om.readValue(
            restApiKeyMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ApiKeyDTO.class
        );

        // Validate the ApiKey in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedApiKey = apiKeyMapper.toEntity(returnedApiKeyDTO);
        assertApiKeyUpdatableFieldsEquals(returnedApiKey, getPersistedApiKey(returnedApiKey));

        insertedApiKey = returnedApiKey;
    }

    @Test
    @Transactional
    void createApiKeyWithExistingId() throws Exception {
        // Create the ApiKey with an existing ID
        apiKey.setId(1L);
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restApiKeyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        apiKey.setName(null);

        // Create the ApiKey, which fails.
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        restApiKeyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPrefixIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        apiKey.setPrefix(null);

        // Create the ApiKey, which fails.
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        restApiKeyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkKeyHashIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        apiKey.setKeyHash(null);

        // Create the ApiKey, which fails.
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        restApiKeyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        apiKey.setCreatedAt(null);

        // Create the ApiKey, which fails.
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        restApiKeyMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllApiKeys() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        // Get all the apiKeyList
        restApiKeyMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(apiKey.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].prefix").value(hasItem(DEFAULT_PREFIX)))
            .andExpect(jsonPath("$.[*].keyHash").value(hasItem(DEFAULT_KEY_HASH)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllApiKeysWithEagerRelationshipsIsEnabled() throws Exception {
        when(apiKeyServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restApiKeyMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(apiKeyServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllApiKeysWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(apiKeyServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restApiKeyMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(apiKeyRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getApiKey() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        // Get the apiKey
        restApiKeyMockMvc
            .perform(get(ENTITY_API_URL_ID, apiKey.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(apiKey.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.prefix").value(DEFAULT_PREFIX))
            .andExpect(jsonPath("$.keyHash").value(DEFAULT_KEY_HASH))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.revokedAt").value(DEFAULT_REVOKED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingApiKey() throws Exception {
        // Get the apiKey
        restApiKeyMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingApiKey() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the apiKey
        ApiKey updatedApiKey = apiKeyRepository.findById(apiKey.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedApiKey are not directly saved in db
        em.detach(updatedApiKey);
        updatedApiKey
            .name(UPDATED_NAME)
            .prefix(UPDATED_PREFIX)
            .keyHash(UPDATED_KEY_HASH)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT);
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(updatedApiKey);

        restApiKeyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, apiKeyDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO))
            )
            .andExpect(status().isOk());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedApiKeyToMatchAllProperties(updatedApiKey);
    }

    @Test
    @Transactional
    void putNonExistingApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, apiKeyDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(apiKeyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateApiKeyWithPatch() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the apiKey using partial update
        ApiKey partialUpdatedApiKey = new ApiKey();
        partialUpdatedApiKey.setId(apiKey.getId());

        partialUpdatedApiKey.revokedAt(UPDATED_REVOKED_AT);

        restApiKeyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApiKey.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApiKey))
            )
            .andExpect(status().isOk());

        // Validate the ApiKey in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApiKeyUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedApiKey, apiKey), getPersistedApiKey(apiKey));
    }

    @Test
    @Transactional
    void fullUpdateApiKeyWithPatch() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the apiKey using partial update
        ApiKey partialUpdatedApiKey = new ApiKey();
        partialUpdatedApiKey.setId(apiKey.getId());

        partialUpdatedApiKey
            .name(UPDATED_NAME)
            .prefix(UPDATED_PREFIX)
            .keyHash(UPDATED_KEY_HASH)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT);

        restApiKeyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedApiKey.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedApiKey))
            )
            .andExpect(status().isOk());

        // Validate the ApiKey in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertApiKeyUpdatableFieldsEquals(partialUpdatedApiKey, getPersistedApiKey(partialUpdatedApiKey));
    }

    @Test
    @Transactional
    void patchNonExistingApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, apiKeyDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(apiKeyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(apiKeyDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamApiKey() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        apiKey.setId(longCount.incrementAndGet());

        // Create the ApiKey
        ApiKeyDTO apiKeyDTO = apiKeyMapper.toDto(apiKey);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restApiKeyMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(apiKeyDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ApiKey in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteApiKey() throws Exception {
        // Initialize the database
        insertedApiKey = apiKeyRepository.saveAndFlush(apiKey);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the apiKey
        restApiKeyMockMvc
            .perform(delete(ENTITY_API_URL_ID, apiKey.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return apiKeyRepository.count();
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

    protected ApiKey getPersistedApiKey(ApiKey apiKey) {
        return apiKeyRepository.findById(apiKey.getId()).orElseThrow();
    }

    protected void assertPersistedApiKeyToMatchAllProperties(ApiKey expectedApiKey) {
        assertApiKeyAllPropertiesEquals(expectedApiKey, getPersistedApiKey(expectedApiKey));
    }

    protected void assertPersistedApiKeyToMatchUpdatableProperties(ApiKey expectedApiKey) {
        assertApiKeyAllUpdatablePropertiesEquals(expectedApiKey, getPersistedApiKey(expectedApiKey));
    }
}
