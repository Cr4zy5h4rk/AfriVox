package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.repository.DatasetRequestRepository;
import com.afrivox.app.service.dto.DatasetRequestDTO;
import com.afrivox.app.service.mapper.DatasetRequestMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.DatasetRequest}.
 */
@Service
@Transactional
public class DatasetRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetRequestService.class);

    private final DatasetRequestRepository datasetRequestRepository;

    private final DatasetRequestMapper datasetRequestMapper;

    public DatasetRequestService(DatasetRequestRepository datasetRequestRepository, DatasetRequestMapper datasetRequestMapper) {
        this.datasetRequestRepository = datasetRequestRepository;
        this.datasetRequestMapper = datasetRequestMapper;
    }

    /**
     * Save a datasetRequest.
     *
     * @param datasetRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetRequestDTO save(DatasetRequestDTO datasetRequestDTO) {
        LOG.debug("Request to save DatasetRequest : {}", datasetRequestDTO);
        DatasetRequest datasetRequest = datasetRequestMapper.toEntity(datasetRequestDTO);
        datasetRequest = datasetRequestRepository.save(datasetRequest);
        return datasetRequestMapper.toDto(datasetRequest);
    }

    /**
     * Update a datasetRequest.
     *
     * @param datasetRequestDTO the entity to save.
     * @return the persisted entity.
     */
    public DatasetRequestDTO update(DatasetRequestDTO datasetRequestDTO) {
        LOG.debug("Request to update DatasetRequest : {}", datasetRequestDTO);
        DatasetRequest datasetRequest = datasetRequestMapper.toEntity(datasetRequestDTO);
        datasetRequest = datasetRequestRepository.save(datasetRequest);
        return datasetRequestMapper.toDto(datasetRequest);
    }

    /**
     * Partially update a datasetRequest.
     *
     * @param datasetRequestDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DatasetRequestDTO> partialUpdate(DatasetRequestDTO datasetRequestDTO) {
        LOG.debug("Request to partially update DatasetRequest : {}", datasetRequestDTO);

        return datasetRequestRepository
            .findById(datasetRequestDTO.getId())
            .map(existingDatasetRequest -> {
                datasetRequestMapper.partialUpdate(existingDatasetRequest, datasetRequestDTO);

                return existingDatasetRequest;
            })
            .map(datasetRequestRepository::save)
            .map(datasetRequestMapper::toDto);
    }

    /**
     * Get all the datasetRequests.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DatasetRequestDTO> findAll() {
        LOG.debug("Request to get all DatasetRequests");
        return datasetRequestRepository
            .findAll()
            .stream()
            .map(datasetRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the datasetRequests with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<DatasetRequestDTO> findAllWithEagerRelationships() {
        return datasetRequestRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(datasetRequestMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one datasetRequest by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DatasetRequestDTO> findOne(Long id) {
        LOG.debug("Request to get DatasetRequest : {}", id);
        return datasetRequestRepository.findOneWithEagerRelationships(id).map(datasetRequestMapper::toDto);
    }

    /**
     * Delete the datasetRequest by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DatasetRequest : {}", id);
        datasetRequestRepository.deleteById(id);
    }
}
