package com.afrivox.app.service;

import com.afrivox.app.domain.RecordingReview;
import com.afrivox.app.repository.RecordingReviewRepository;
import com.afrivox.app.service.dto.RecordingReviewDTO;
import com.afrivox.app.service.mapper.RecordingReviewMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.RecordingReview}.
 */
@Service
@Transactional
public class RecordingReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingReviewService.class);

    private final RecordingReviewRepository recordingReviewRepository;

    private final RecordingReviewMapper recordingReviewMapper;

    public RecordingReviewService(RecordingReviewRepository recordingReviewRepository, RecordingReviewMapper recordingReviewMapper) {
        this.recordingReviewRepository = recordingReviewRepository;
        this.recordingReviewMapper = recordingReviewMapper;
    }

    /**
     * Save a recordingReview.
     *
     * @param recordingReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingReviewDTO save(RecordingReviewDTO recordingReviewDTO) {
        LOG.debug("Request to save RecordingReview : {}", recordingReviewDTO);
        RecordingReview recordingReview = recordingReviewMapper.toEntity(recordingReviewDTO);
        recordingReview = recordingReviewRepository.save(recordingReview);
        return recordingReviewMapper.toDto(recordingReview);
    }

    /**
     * Update a recordingReview.
     *
     * @param recordingReviewDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingReviewDTO update(RecordingReviewDTO recordingReviewDTO) {
        LOG.debug("Request to update RecordingReview : {}", recordingReviewDTO);
        RecordingReview recordingReview = recordingReviewMapper.toEntity(recordingReviewDTO);
        recordingReview = recordingReviewRepository.save(recordingReview);
        return recordingReviewMapper.toDto(recordingReview);
    }

    /**
     * Partially update a recordingReview.
     *
     * @param recordingReviewDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecordingReviewDTO> partialUpdate(RecordingReviewDTO recordingReviewDTO) {
        LOG.debug("Request to partially update RecordingReview : {}", recordingReviewDTO);

        return recordingReviewRepository
            .findById(recordingReviewDTO.getId())
            .map(existingRecordingReview -> {
                recordingReviewMapper.partialUpdate(existingRecordingReview, recordingReviewDTO);

                return existingRecordingReview;
            })
            .map(recordingReviewRepository::save)
            .map(recordingReviewMapper::toDto);
    }

    /**
     * Get all the recordingReviews.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<RecordingReviewDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all RecordingReviews");
        return recordingReviewRepository.findAll(pageable).map(recordingReviewMapper::toDto);
    }

    /**
     * Get all the recordingReviews with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecordingReviewDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recordingReviewRepository.findAllWithEagerRelationships(pageable).map(recordingReviewMapper::toDto);
    }

    /**
     * Get one recordingReview by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecordingReviewDTO> findOne(Long id) {
        LOG.debug("Request to get RecordingReview : {}", id);
        return recordingReviewRepository.findOneWithEagerRelationships(id).map(recordingReviewMapper::toDto);
    }

    /**
     * Delete the recordingReview by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RecordingReview : {}", id);
        recordingReviewRepository.deleteById(id);
    }
}
