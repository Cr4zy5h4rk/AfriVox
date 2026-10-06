package com.afrivox.app.service;

import com.afrivox.app.domain.MlModel;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.service.dto.MlModelDTO;
import com.afrivox.app.service.mapper.MlModelMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.MlModel}.
 */
@Service
@Transactional
public class MlModelService {

    private static final Logger LOG = LoggerFactory.getLogger(MlModelService.class);

    private final MlModelRepository mlModelRepository;

    private final MlModelMapper mlModelMapper;

    public MlModelService(MlModelRepository mlModelRepository, MlModelMapper mlModelMapper) {
        this.mlModelRepository = mlModelRepository;
        this.mlModelMapper = mlModelMapper;
    }

    /**
     * Save a mlModel.
     *
     * @param mlModelDTO the entity to save.
     * @return the persisted entity.
     */
    public MlModelDTO save(MlModelDTO mlModelDTO) {
        LOG.debug("Request to save MlModel : {}", mlModelDTO);
        MlModel mlModel = mlModelMapper.toEntity(mlModelDTO);
        mlModel = mlModelRepository.save(mlModel);
        return mlModelMapper.toDto(mlModel);
    }

    /**
     * Update a mlModel.
     *
     * @param mlModelDTO the entity to save.
     * @return the persisted entity.
     */
    public MlModelDTO update(MlModelDTO mlModelDTO) {
        LOG.debug("Request to update MlModel : {}", mlModelDTO);
        MlModel mlModel = mlModelMapper.toEntity(mlModelDTO);
        mlModel = mlModelRepository.save(mlModel);
        return mlModelMapper.toDto(mlModel);
    }

    /**
     * Partially update a mlModel.
     *
     * @param mlModelDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MlModelDTO> partialUpdate(MlModelDTO mlModelDTO) {
        LOG.debug("Request to partially update MlModel : {}", mlModelDTO);

        return mlModelRepository
            .findById(mlModelDTO.getId())
            .map(existingMlModel -> {
                mlModelMapper.partialUpdate(existingMlModel, mlModelDTO);

                return existingMlModel;
            })
            .map(mlModelRepository::save)
            .map(mlModelMapper::toDto);
    }

    /**
     * Get all the mlModels.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<MlModelDTO> findAll() {
        LOG.debug("Request to get all MlModels");
        return mlModelRepository.findAll().stream().map(mlModelMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the mlModels with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<MlModelDTO> findAllWithEagerRelationships() {
        return mlModelRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(mlModelMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one mlModel by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MlModelDTO> findOne(Long id) {
        LOG.debug("Request to get MlModel : {}", id);
        return mlModelRepository.findOneWithEagerRelationships(id).map(mlModelMapper::toDto);
    }

    /**
     * Delete the mlModel by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete MlModel : {}", id);
        mlModelRepository.deleteById(id);
    }
}
