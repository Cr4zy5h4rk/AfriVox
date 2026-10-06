package com.afrivox.app.service;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.service.dto.DatasetDTO;
import com.afrivox.app.service.mapper.DatasetMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Dataset}.
 */
@Service
@Transactional
public class DatasetService {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetService.class);

    private final DatasetRepository datasetRepository;

    private final DatasetMapper datasetMapper;

    public DatasetService(DatasetRepository datasetRepository, DatasetMapper datasetMapper) {
        this.datasetRepository = datasetRepository;
        this.datasetMapper = datasetMapper;
    }

    /**
     * Save a dataset.
     *
     * @param datasetDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetDTO save(DatasetDTO datasetDTO) {
        LOG.debug("Request to save Dataset : {}", datasetDTO);
        Dataset dataset = datasetMapper.toEntity(datasetDTO);
        dataset = datasetRepository.save(dataset);
        return datasetMapper.toDto(dataset);
    }

    /**
     * Update a dataset.
     *
     * @param datasetDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetDTO update(DatasetDTO datasetDTO) {
        LOG.debug("Request to update Dataset : {}", datasetDTO);
        Dataset dataset = datasetMapper.toEntity(datasetDTO);
        dataset = datasetRepository.save(dataset);
        return datasetMapper.toDto(dataset);
    }

    /**
     * Partially update a dataset.
     *
     * @param datasetDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DatasetDTO> partialUpdate(DatasetDTO datasetDTO) {
        LOG.debug("Request to partially update Dataset : {}", datasetDTO);

        return datasetRepository
            .findById(datasetDTO.getId())
            .map(existingDataset -> {
                datasetMapper.partialUpdate(existingDataset, datasetDTO);

                return existingDataset;
            })
            .map(datasetRepository::save)
            .map(datasetMapper::toDto);
    }

    /**
     * Get all the datasets.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DatasetDTO> findAll() {
        LOG.debug("Request to get all Datasets");
        return datasetRepository.findAll().stream().map(datasetMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the datasets with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<DatasetDTO> findAllWithEagerRelationships() {
        return datasetRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(datasetMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one dataset by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DatasetDTO> findOne(Long id) {
        LOG.debug("Request to get Dataset : {}", id);
        return datasetRepository.findOneWithEagerRelationships(id).map(datasetMapper::toDto);
    }

    /**
     * Delete the dataset by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Dataset : {}", id);
        datasetRepository.deleteById(id);
    }
}
