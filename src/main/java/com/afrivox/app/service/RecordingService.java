package com.afrivox.app.service;

import com.afrivox.app.domain.Recording;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.service.dto.RecordingDTO;
import com.afrivox.app.service.mapper.RecordingMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Recording}.
 */
@Service
@Transactional
public class RecordingService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingService.class);

    private final RecordingRepository recordingRepository;

    private final RecordingMapper recordingMapper;

    public RecordingService(RecordingRepository recordingRepository, RecordingMapper recordingMapper) {
        this.recordingRepository = recordingRepository;
        this.recordingMapper = recordingMapper;
    }

    /**
     * Save a recording.
     *
     * @param recordingDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingDTO save(RecordingDTO recordingDTO) {
        LOG.debug("Request to save Recording : {}", recordingDTO);
        Recording recording = recordingMapper.toEntity(recordingDTO);
        recording = recordingRepository.save(recording);
        return recordingMapper.toDto(recording);
    }

    /**
     * Update a recording.
     *
     * @param recordingDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingDTO update(RecordingDTO recordingDTO) {
        LOG.debug("Request to update Recording : {}", recordingDTO);
        Recording recording = recordingMapper.toEntity(recordingDTO);
        recording = recordingRepository.save(recording);
        return recordingMapper.toDto(recording);
    }

    /**
     * Partially update a recording.
     *
     * @param recordingDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecordingDTO> partialUpdate(RecordingDTO recordingDTO) {
        LOG.debug("Request to partially update Recording : {}", recordingDTO);

        return recordingRepository
            .findById(recordingDTO.getId())
            .map(existingRecording -> {
                recordingMapper.partialUpdate(existingRecording, recordingDTO);

                return existingRecording;
            })
            .map(recordingRepository::save)
            .map(recordingMapper::toDto);
    }

    /**
     * Get all the recordings.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<RecordingDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Recordings");
        return recordingRepository.findAll(pageable).map(recordingMapper::toDto);
    }

    /**
     * Get all the recordings with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecordingDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recordingRepository.findAllWithEagerRelationships(pageable).map(recordingMapper::toDto);
    }

    /**
     * Get one recording by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecordingDTO> findOne(Long id) {
        LOG.debug("Request to get Recording : {}", id);
        return recordingRepository.findOneWithEagerRelationships(id).map(recordingMapper::toDto);
    }

    /**
     * Delete the recording by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Recording : {}", id);
        recordingRepository.deleteById(id);
    }
}
