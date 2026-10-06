package com.afrivox.app.web.rest;

import static com.afrivox.app.domain.CampaignAsserts.*;
import static com.afrivox.app.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.service.CampaignService;
import com.afrivox.app.service.dto.CampaignDTO;
import com.afrivox.app.service.mapper.CampaignMapper;
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
 * Integration tests for the {@link CampaignResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CampaignResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_DOMAIN = "AAAAAAAAAA";
    private static final String UPDATED_DOMAIN = "BBBBBBBBBB";

    private static final CampaignKind DEFAULT_KIND = CampaignKind.OPEN;
    private static final CampaignKind UPDATED_KIND = CampaignKind.SPONSORED;

    private static final CampaignStatus DEFAULT_STATUS = CampaignStatus.DRAFT;
    private static final CampaignStatus UPDATED_STATUS = CampaignStatus.ACTIVE;

    private static final Integer DEFAULT_TARGET_SENTENCES = 1;
    private static final Integer UPDATED_TARGET_SENTENCES = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1791208131505L);

    private static final String ENTITY_API_URL = "/api/campaigns";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignRepository campaignRepositoryMock;

    @Autowired
    private CampaignMapper campaignMapper;

    @Mock
    private CampaignService campaignServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCampaignMockMvc;

    private Campaign campaign;

    private Campaign insertedCampaign;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Campaign createEntity() {
        return new Campaign()
            .title(DEFAULT_TITLE)
            .description(DEFAULT_DESCRIPTION)
            .domain(DEFAULT_DOMAIN)
            .kind(DEFAULT_KIND)
            .status(DEFAULT_STATUS)
            .targetSentences(DEFAULT_TARGET_SENTENCES)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Campaign createUpdatedEntity() {
        return new Campaign()
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .kind(UPDATED_KIND)
            .status(UPDATED_STATUS)
            .targetSentences(UPDATED_TARGET_SENTENCES)
            .createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        campaign = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCampaign != null) {
            campaignRepository.delete(insertedCampaign);
            insertedCampaign = null;
        }
    }

    @Test
    @Transactional
    void createCampaign() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);
        var returnedCampaignDTO = om.readValue(
            restCampaignMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CampaignDTO.class
        );

        // Validate the Campaign in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCampaign = campaignMapper.toEntity(returnedCampaignDTO);
        assertCampaignUpdatableFieldsEquals(returnedCampaign, getPersistedCampaign(returnedCampaign));

        insertedCampaign = returnedCampaign;
    }

    @Test
    @Transactional
    void createCampaignWithExistingId() throws Exception {
        // Create the Campaign with an existing ID
        campaign.setId(1L);
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCampaignMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setTitle(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        restCampaignMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkKindIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setKind(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        restCampaignMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setStatus(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        restCampaignMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setCreatedAt(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        restCampaignMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCampaigns() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        // Get all the campaignList
        restCampaignMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(campaign.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].domain").value(hasItem(DEFAULT_DOMAIN)))
            .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].targetSentences").value(hasItem(DEFAULT_TARGET_SENTENCES)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCampaignsWithEagerRelationshipsIsEnabled() throws Exception {
        when(campaignServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restCampaignMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(campaignServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCampaignsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(campaignServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restCampaignMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(campaignRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCampaign() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        // Get the campaign
        restCampaignMockMvc
            .perform(get(ENTITY_API_URL_ID, campaign.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(campaign.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.domain").value(DEFAULT_DOMAIN))
            .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.targetSentences").value(DEFAULT_TARGET_SENTENCES))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCampaign() throws Exception {
        // Get the campaign
        restCampaignMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCampaign() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign
        Campaign updatedCampaign = campaignRepository.findById(campaign.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCampaign are not directly saved in db
        em.detach(updatedCampaign);
        updatedCampaign
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .kind(UPDATED_KIND)
            .status(UPDATED_STATUS)
            .targetSentences(UPDATED_TARGET_SENTENCES)
            .createdAt(UPDATED_CREATED_AT);
        CampaignDTO campaignDTO = campaignMapper.toDto(updatedCampaign);

        restCampaignMockMvc
            .perform(
                put(ENTITY_API_URL_ID, campaignDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignDTO))
            )
            .andExpect(status().isOk());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCampaignToMatchAllProperties(updatedCampaign);
    }

    @Test
    @Transactional
    void putNonExistingCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(
                put(ENTITY_API_URL_ID, campaignDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCampaignWithPatch() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign using partial update
        Campaign partialUpdatedCampaign = new Campaign();
        partialUpdatedCampaign.setId(campaign.getId());

        partialUpdatedCampaign
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .kind(UPDATED_KIND)
            .status(UPDATED_STATUS)
            .targetSentences(UPDATED_TARGET_SENTENCES)
            .createdAt(UPDATED_CREATED_AT);

        restCampaignMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCampaign.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCampaign))
            )
            .andExpect(status().isOk());

        // Validate the Campaign in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCampaign, campaign), getPersistedCampaign(campaign));
    }

    @Test
    @Transactional
    void fullUpdateCampaignWithPatch() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign using partial update
        Campaign partialUpdatedCampaign = new Campaign();
        partialUpdatedCampaign.setId(campaign.getId());

        partialUpdatedCampaign
            .title(UPDATED_TITLE)
            .description(UPDATED_DESCRIPTION)
            .domain(UPDATED_DOMAIN)
            .kind(UPDATED_KIND)
            .status(UPDATED_STATUS)
            .targetSentences(UPDATED_TARGET_SENTENCES)
            .createdAt(UPDATED_CREATED_AT);

        restCampaignMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCampaign.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCampaign))
            )
            .andExpect(status().isOk());

        // Validate the Campaign in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignUpdatableFieldsEquals(partialUpdatedCampaign, getPersistedCampaign(partialUpdatedCampaign));
    }

    @Test
    @Transactional
    void patchNonExistingCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, campaignDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(campaignDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(campaignDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(campaignDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCampaign() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.saveAndFlush(campaign);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the campaign
        restCampaignMockMvc
            .perform(delete(ENTITY_API_URL_ID, campaign.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return campaignRepository.count();
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

    protected Campaign getPersistedCampaign(Campaign campaign) {
        return campaignRepository.findById(campaign.getId()).orElseThrow();
    }

    protected void assertPersistedCampaignToMatchAllProperties(Campaign expectedCampaign) {
        assertCampaignAllPropertiesEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
    }

    protected void assertPersistedCampaignToMatchUpdatableProperties(Campaign expectedCampaign) {
        assertCampaignAllUpdatablePropertiesEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
    }
}
