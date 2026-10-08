package com.afrivox.app.service;

import com.afrivox.app.domain.Language;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.service.dto.LanguageDTO;
import com.afrivox.app.service.mapper.LanguageMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Language}.
 */
@Service
@Transactional
public class LanguageService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageService.class);

    private final LanguageRepository languageRepository;

    private final LanguageMapper languageMapper;

    private final LanguageActivationService languageActivationService;

    public LanguageService(
        LanguageRepository languageRepository,
        LanguageMapper languageMapper,
        LanguageActivationService languageActivationService
    ) {
        this.languageRepository = languageRepository;
        this.languageMapper = languageMapper;
        this.languageActivationService = languageActivationService;
    }

    /**
     * Save a language.
     *
     * @param languageDTO the entity to save.
     * @return the persisted entity.
     */
    public LanguageDTO save(LanguageDTO languageDTO) {
        LOG.debug("Request to save Language : {}", languageDTO);
        Language language = languageMapper.toEntity(languageDTO);
        language = languageRepository.save(language);
        return languageMapper.toDto(language);
    }

    /**
     * Update a language.
     *
     * @param languageDTO the entity to save.
     * @return the persisted entity.
     */
    public LanguageDTO update(LanguageDTO languageDTO) {
        LOG.debug("Request to update Language : {}", languageDTO);
        Language language = languageMapper.toEntity(languageDTO);
        language = languageRepository.save(language);
        return languageMapper.toDto(language);
    }

    /**
     * Partially update a language.
     *
     * @param languageDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<LanguageDTO> partialUpdate(LanguageDTO languageDTO) {
        LOG.debug("Request to partially update Language : {}", languageDTO);

        return languageRepository
            .findById(languageDTO.getId())
            .map(existingLanguage -> {
                languageMapper.partialUpdate(existingLanguage, languageDTO);

                return existingLanguage;
            })
            .map(languageRepository::save)
            .map(saved -> {
                // Si l'admin fixe le statut lui-meme (ex. remise en attente), on ne le contredit pas.
                if (languageDTO.getStatus() == null) {
                    languageActivationService.promoteIfReady(saved.getId());
                }
                return saved;
            })
            .map(languageMapper::toDto);
    }

    /**
     * Get all the languages.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<LanguageDTO> findAll() {
        LOG.debug("Request to get all Languages");
        return languageRepository.findAll().stream().map(languageMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one language by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LanguageDTO> findOne(Long id) {
        LOG.debug("Request to get Language : {}", id);
        return languageRepository.findById(id).map(languageMapper::toDto);
    }

    /**
     * Delete the language by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Language : {}", id);
        languageRepository.deleteById(id);
    }
}
