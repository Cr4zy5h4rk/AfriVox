package com.afrivox.app.service;

import com.afrivox.app.domain.LanguageQualification;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.service.dto.LanguageQualificationDTO;
import com.afrivox.app.service.mapper.LanguageQualificationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.LanguageQualification}.
 */
@Service
@Transactional
public class LanguageQualificationService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageQualificationService.class);

    private final LanguageQualificationRepository languageQualificationRepository;

    private final LanguageQualificationMapper languageQualificationMapper;

    public LanguageQualificationService(
        LanguageQualificationRepository languageQualificationRepository,
        LanguageQualificationMapper languageQualificationMapper
    ) {
        this.languageQualificationRepository = languageQualificationRepository;
        this.languageQualificationMapper = languageQualificationMapper;
    }

    /**
     * Save a languageQualification.
     *
     * @param languageQualificationDTO the entity to save.
     * @return the persisted entity.
     */
    public LanguageQualificationDTO save(LanguageQualificationDTO languageQualificationDTO) {
        LOG.debug("Request to save LanguageQualification : {}", languageQualificationDTO);
        LanguageQualification languageQualification = languageQualificationMapper.toEntity(languageQualificationDTO);
        languageQualification = languageQualificationRepository.save(languageQualification);
        return languageQualificationMapper.toDto(languageQualification);
    }

    /**
     * Update a languageQualification.
     *
     * @param languageQualificationDTO the entity to save.
     * @return the persisted entity.
     */
    public LanguageQualificationDTO update(LanguageQualificationDTO languageQualificationDTO) {
        LOG.debug("Request to update LanguageQualification : {}", languageQualificationDTO);
        LanguageQualification languageQualification = languageQualificationMapper.toEntity(languageQualificationDTO);
        languageQualification = languageQualificationRepository.save(languageQualification);
        return languageQualificationMapper.toDto(languageQualification);
    }

    /**
     * Partially update a languageQualification.
     *
     * @param languageQualificationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<LanguageQualificationDTO> partialUpdate(LanguageQualificationDTO languageQualificationDTO) {
        LOG.debug("Request to partially update LanguageQualification : {}", languageQualificationDTO);

        return languageQualificationRepository
            .findById(languageQualificationDTO.getId())
            .map(existingLanguageQualification -> {
                languageQualificationMapper.partialUpdate(existingLanguageQualification, languageQualificationDTO);

                return existingLanguageQualification;
            })
            .map(languageQualificationRepository::save)
            .map(languageQualificationMapper::toDto);
    }

    /**
     * Get all the languageQualifications.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<LanguageQualificationDTO> findAll() {
        LOG.debug("Request to get all LanguageQualifications");
        return languageQualificationRepository
            .findAll()
            .stream()
            .map(languageQualificationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the languageQualifications with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<LanguageQualificationDTO> findAllWithEagerRelationships() {
        return languageQualificationRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(languageQualificationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one languageQualification by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LanguageQualificationDTO> findOne(Long id) {
        LOG.debug("Request to get LanguageQualification : {}", id);
        return languageQualificationRepository.findOneWithEagerRelationships(id).map(languageQualificationMapper::toDto);
    }

    /**
     * Delete the languageQualification by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete LanguageQualification : {}", id);
        languageQualificationRepository.deleteById(id);
    }
}
