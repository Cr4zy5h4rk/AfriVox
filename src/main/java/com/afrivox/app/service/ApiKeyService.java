package com.afrivox.app.service;

import com.afrivox.app.domain.ApiKey;
import com.afrivox.app.repository.ApiKeyRepository;
import com.afrivox.app.service.dto.ApiKeyDTO;
import com.afrivox.app.service.mapper.ApiKeyMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.ApiKey}.
 */
@Service
@Transactional
public class ApiKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;

    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, ApiKeyMapper apiKeyMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyMapper = apiKeyMapper;
    }

    /**
     * Save a apiKey.
     *
     * @param apiKeyDTO the entity to save.
     * @return the persisted entity.
     */
    public ApiKeyDTO save(ApiKeyDTO apiKeyDTO) {
        LOG.debug("Request to save ApiKey : {}", apiKeyDTO);
        ApiKey apiKey = apiKeyMapper.toEntity(apiKeyDTO);
        apiKey = apiKeyRepository.save(apiKey);
        return apiKeyMapper.toDto(apiKey);
    }

    /**
     * Update a apiKey.
     *
     * @param apiKeyDTO the entity to save.
     * @return the persisted entity.
     */
    public ApiKeyDTO update(ApiKeyDTO apiKeyDTO) {
        LOG.debug("Request to update ApiKey : {}", apiKeyDTO);
        ApiKey apiKey = apiKeyMapper.toEntity(apiKeyDTO);
        apiKey = apiKeyRepository.save(apiKey);
        return apiKeyMapper.toDto(apiKey);
    }

    /**
     * Partially update a apiKey.
     *
     * @param apiKeyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ApiKeyDTO> partialUpdate(ApiKeyDTO apiKeyDTO) {
        LOG.debug("Request to partially update ApiKey : {}", apiKeyDTO);

        return apiKeyRepository
            .findById(apiKeyDTO.getId())
            .map(existingApiKey -> {
                apiKeyMapper.partialUpdate(existingApiKey, apiKeyDTO);

                return existingApiKey;
            })
            .map(apiKeyRepository::save)
            .map(apiKeyMapper::toDto);
    }

    /**
     * Get all the apiKeys.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyDTO> findAll() {
        LOG.debug("Request to get all ApiKeys");
        return apiKeyRepository.findAll().stream().map(apiKeyMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the apiKeys with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<ApiKeyDTO> findAllWithEagerRelationships() {
        return apiKeyRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(apiKeyMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one apiKey by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ApiKeyDTO> findOne(Long id) {
        LOG.debug("Request to get ApiKey : {}", id);
        return apiKeyRepository.findOneWithEagerRelationships(id).map(apiKeyMapper::toDto);
    }

    /**
     * Delete the apiKey by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete ApiKey : {}", id);
        apiKeyRepository.deleteById(id);
    }
}
