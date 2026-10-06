package com.afrivox.app.service;

import com.afrivox.app.domain.Prompt;
import com.afrivox.app.repository.PromptRepository;
import com.afrivox.app.service.dto.PromptDTO;
import com.afrivox.app.service.mapper.PromptMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Prompt}.
 */
@Service
@Transactional
public class PromptService {

    private static final Logger LOG = LoggerFactory.getLogger(PromptService.class);

    private final PromptRepository promptRepository;

    private final PromptMapper promptMapper;

    public PromptService(PromptRepository promptRepository, PromptMapper promptMapper) {
        this.promptRepository = promptRepository;
        this.promptMapper = promptMapper;
    }

    /**
     * Save a prompt.
     *
     * @param promptDTO the entity to save.
     * @return the persisted entity.
     */
    public PromptDTO save(PromptDTO promptDTO) {
        LOG.debug("Request to save Prompt : {}", promptDTO);
        Prompt prompt = promptMapper.toEntity(promptDTO);
        prompt = promptRepository.save(prompt);
        return promptMapper.toDto(prompt);
    }

    /**
     * Update a prompt.
     *
     * @param promptDTO the entity to save.
     * @return the persisted entity.
     */
    public PromptDTO update(PromptDTO promptDTO) {
        LOG.debug("Request to update Prompt : {}", promptDTO);
        Prompt prompt = promptMapper.toEntity(promptDTO);
        prompt = promptRepository.save(prompt);
        return promptMapper.toDto(prompt);
    }

    /**
     * Partially update a prompt.
     *
     * @param promptDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PromptDTO> partialUpdate(PromptDTO promptDTO) {
        LOG.debug("Request to partially update Prompt : {}", promptDTO);

        return promptRepository
            .findById(promptDTO.getId())
            .map(existingPrompt -> {
                promptMapper.partialUpdate(existingPrompt, promptDTO);

                return existingPrompt;
            })
            .map(promptRepository::save)
            .map(promptMapper::toDto);
    }

    /**
     * Get all the prompts.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<PromptDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Prompts");
        return promptRepository.findAll(pageable).map(promptMapper::toDto);
    }

    /**
     * Get all the prompts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PromptDTO> findAllWithEagerRelationships(Pageable pageable) {
        return promptRepository.findAllWithEagerRelationships(pageable).map(promptMapper::toDto);
    }

    /**
     * Get one prompt by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PromptDTO> findOne(Long id) {
        LOG.debug("Request to get Prompt : {}", id);
        return promptRepository.findOneWithEagerRelationships(id).map(promptMapper::toDto);
    }

    /**
     * Delete the prompt by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Prompt : {}", id);
        promptRepository.deleteById(id);
    }
}
