package com.afrivox.app.service;

import com.afrivox.app.domain.Evaluation;
import com.afrivox.app.repository.EvaluationRepository;
import com.afrivox.app.service.dto.EvaluationDTO;
import com.afrivox.app.service.mapper.EvaluationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Evaluation}.
 */
@Service
@Transactional
public class EvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationRepository evaluationRepository;

    private final EvaluationMapper evaluationMapper;

    public EvaluationService(EvaluationRepository evaluationRepository, EvaluationMapper evaluationMapper) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationMapper = evaluationMapper;
    }

    /**
     * Save a evaluation.
     *
     * @param evaluationDTO the entity to save.
     * @return the persisted entity.
     */
    public EvaluationDTO save(EvaluationDTO evaluationDTO) {
        LOG.debug("Request to save Evaluation : {}", evaluationDTO);
        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        evaluation = evaluationRepository.save(evaluation);
        return evaluationMapper.toDto(evaluation);
    }

    /**
     * Update a evaluation.
     *
     * @param evaluationDTO the entity to save.
     * @return the persisted entity.
     */
    public EvaluationDTO update(EvaluationDTO evaluationDTO) {
        LOG.debug("Request to update Evaluation : {}", evaluationDTO);
        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        evaluation = evaluationRepository.save(evaluation);
        return evaluationMapper.toDto(evaluation);
    }

    /**
     * Partially update a evaluation.
     *
     * @param evaluationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EvaluationDTO> partialUpdate(EvaluationDTO evaluationDTO) {
        LOG.debug("Request to partially update Evaluation : {}", evaluationDTO);

        return evaluationRepository
            .findById(evaluationDTO.getId())
            .map(existingEvaluation -> {
                evaluationMapper.partialUpdate(existingEvaluation, evaluationDTO);

                return existingEvaluation;
            })
            .map(evaluationRepository::save)
            .map(evaluationMapper::toDto);
    }

    /**
     * Get all the evaluations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<EvaluationDTO> findAll() {
        LOG.debug("Request to get all Evaluations");
        return evaluationRepository.findAll().stream().map(evaluationMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the evaluations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<EvaluationDTO> findAllWithEagerRelationships() {
        return evaluationRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(evaluationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one evaluation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EvaluationDTO> findOne(Long id) {
        LOG.debug("Request to get Evaluation : {}", id);
        return evaluationRepository.findOneWithEagerRelationships(id).map(evaluationMapper::toDto);
    }

    /**
     * Delete the evaluation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Evaluation : {}", id);
        evaluationRepository.deleteById(id);
    }
}
