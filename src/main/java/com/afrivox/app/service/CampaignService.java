package com.afrivox.app.service;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.service.dto.CampaignDTO;
import com.afrivox.app.service.mapper.CampaignMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Campaign}.
 */
@Service
@Transactional
public class CampaignService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;

    private final CampaignMapper campaignMapper;

    public CampaignService(CampaignRepository campaignRepository, CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.campaignMapper = campaignMapper;
    }

    /**
     * Save a campaign.
     *
     * @param campaignDTO the entity to save.
     * @return the persisted entity.
     */
    public CampaignDTO save(CampaignDTO campaignDTO) {
        LOG.debug("Request to save Campaign : {}", campaignDTO);
        Campaign campaign = campaignMapper.toEntity(campaignDTO);
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toDto(campaign);
    }

    /**
     * Update a campaign.
     *
     * @param campaignDTO the entity to save.
     * @return the persisted entity.
     */
    public CampaignDTO update(CampaignDTO campaignDTO) {
        LOG.debug("Request to update Campaign : {}", campaignDTO);
        Campaign campaign = campaignMapper.toEntity(campaignDTO);
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toDto(campaign);
    }

    /**
     * Partially update a campaign.
     *
     * @param campaignDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CampaignDTO> partialUpdate(CampaignDTO campaignDTO) {
        LOG.debug("Request to partially update Campaign : {}", campaignDTO);

        return campaignRepository
            .findById(campaignDTO.getId())
            .map(existingCampaign -> {
                campaignMapper.partialUpdate(existingCampaign, campaignDTO);

                return existingCampaign;
            })
            .map(campaignRepository::save)
            .map(campaignMapper::toDto);
    }

    /**
     * Get all the campaigns.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CampaignDTO> findAll() {
        LOG.debug("Request to get all Campaigns");
        return campaignRepository.findAll().stream().map(campaignMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the campaigns with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<CampaignDTO> findAllWithEagerRelationships() {
        return campaignRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(campaignMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one campaign by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CampaignDTO> findOne(Long id) {
        LOG.debug("Request to get Campaign : {}", id);
        return campaignRepository.findOneWithEagerRelationships(id).map(campaignMapper::toDto);
    }

    /**
     * Delete the campaign by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Campaign : {}", id);
        campaignRepository.deleteById(id);
    }
}
