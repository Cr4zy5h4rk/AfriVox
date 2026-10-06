package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.service.dto.DatasetVersionDTO;
import com.afrivox.app.service.mapper.DatasetVersionMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.DatasetVersion}.
 */
@Service
@Transactional
public class DatasetVersionService {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetVersionService.class);

    private final DatasetVersionRepository datasetVersionRepository;

    private final DatasetVersionMapper datasetVersionMapper;

    public DatasetVersionService(DatasetVersionRepository datasetVersionRepository, DatasetVersionMapper datasetVersionMapper) {
        this.datasetVersionRepository = datasetVersionRepository;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    /**
     * Save a datasetVersion.
     *
     * @param datasetVersionDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetVersionDTO save(DatasetVersionDTO datasetVersionDTO) {
        LOG.debug("Request to save DatasetVersion : {}", datasetVersionDTO);
        DatasetVersion datasetVersion = datasetVersionMapper.toEntity(datasetVersionDTO);
        datasetVersion = datasetVersionRepository.save(datasetVersion);
        return datasetVersionMapper.toDto(datasetVersion);
    }

    /**
     * Update a datasetVersion.
     *
     * @param datasetVersionDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetVersionDTO update(DatasetVersionDTO datasetVersionDTO) {
        LOG.debug("Request to update DatasetVersion : {}", datasetVersionDTO);
        DatasetVersion datasetVersion = datasetVersionMapper.toEntity(datasetVersionDTO);
        datasetVersion = datasetVersionRepository.save(datasetVersion);
        return datasetVersionMapper.toDto(datasetVersion);
    }

    /**
     * Partially update a datasetVersion.
     *
     * @param datasetVersionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DatasetVersionDTO> partialUpdate(DatasetVersionDTO datasetVersionDTO) {
        LOG.debug("Request to partially update DatasetVersion : {}", datasetVersionDTO);

        return datasetVersionRepository
            .findById(datasetVersionDTO.getId())
            .map(existingDatasetVersion -> {
                datasetVersionMapper.partialUpdate(existingDatasetVersion, datasetVersionDTO);

                return existingDatasetVersion;
            })
            .map(datasetVersionRepository::save)
            .map(datasetVersionMapper::toDto);
    }

    /**
     * Get all the datasetVersions.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DatasetVersionDTO> findAll() {
        LOG.debug("Request to get all DatasetVersions");
        return datasetVersionRepository
            .findAll()
            .stream()
            .map(datasetVersionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the datasetVersions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<DatasetVersionDTO> findAllWithEagerRelationships() {
        return datasetVersionRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(datasetVersionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one datasetVersion by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DatasetVersionDTO> findOne(Long id) {
        LOG.debug("Request to get DatasetVersion : {}", id);
        return datasetVersionRepository.findOneWithEagerRelationships(id).map(datasetVersionMapper::toDto);
    }

    /**
     * Delete the datasetVersion by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DatasetVersion : {}", id);
        datasetVersionRepository.deleteById(id);
    }
}
