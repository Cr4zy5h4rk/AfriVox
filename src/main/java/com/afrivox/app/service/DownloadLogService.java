package com.afrivox.app.service;

import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.service.dto.DownloadLogDTO;
import com.afrivox.app.service.mapper.DownloadLogMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.DownloadLog}.
 */
@Service
@Transactional
public class DownloadLogService {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadLogService.class);

    private final DownloadLogRepository downloadLogRepository;

    private final DownloadLogMapper downloadLogMapper;

    public DownloadLogService(DownloadLogRepository downloadLogRepository, DownloadLogMapper downloadLogMapper) {
        this.downloadLogRepository = downloadLogRepository;
        this.downloadLogMapper = downloadLogMapper;
    }

    /**
     * Save a downloadLog.
     *
     * @param downloadLogDTO the entity to save.
     * @return the persisted entity.
     */
    public DownloadLogDTO save(DownloadLogDTO downloadLogDTO) {
        LOG.debug("Request to save DownloadLog : {}", downloadLogDTO);
        DownloadLog downloadLog = downloadLogMapper.toEntity(downloadLogDTO);
        downloadLog = downloadLogRepository.save(downloadLog);
        return downloadLogMapper.toDto(downloadLog);
    }

    /**
     * Update a downloadLog.
     *
     * @param downloadLogDTO the entity to save.
     * @return the persisted entity.
     */
    public DownloadLogDTO update(DownloadLogDTO downloadLogDTO) {
        LOG.debug("Request to update DownloadLog : {}", downloadLogDTO);
        DownloadLog downloadLog = downloadLogMapper.toEntity(downloadLogDTO);
        downloadLog = downloadLogRepository.save(downloadLog);
        return downloadLogMapper.toDto(downloadLog);
    }

    /**
     * Partially update a downloadLog.
     *
     * @param downloadLogDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DownloadLogDTO> partialUpdate(DownloadLogDTO downloadLogDTO) {
        LOG.debug("Request to partially update DownloadLog : {}", downloadLogDTO);

        return downloadLogRepository
            .findById(downloadLogDTO.getId())
            .map(existingDownloadLog -> {
                downloadLogMapper.partialUpdate(existingDownloadLog, downloadLogDTO);

                return existingDownloadLog;
            })
            .map(downloadLogRepository::save)
            .map(downloadLogMapper::toDto);
    }

    /**
     * Get all the downloadLogs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<DownloadLogDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all DownloadLogs");
        return downloadLogRepository.findAll(pageable).map(downloadLogMapper::toDto);
    }

    /**
     * Get all the downloadLogs with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DownloadLogDTO> findAllWithEagerRelationships(Pageable pageable) {
        return downloadLogRepository.findAllWithEagerRelationships(pageable).map(downloadLogMapper::toDto);
    }

    /**
     * Get one downloadLog by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DownloadLogDTO> findOne(Long id) {
        LOG.debug("Request to get DownloadLog : {}", id);
        return downloadLogRepository.findOneWithEagerRelationships(id).map(downloadLogMapper::toDto);
    }

    /**
     * Delete the downloadLog by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete DownloadLog : {}", id);
        downloadLogRepository.deleteById(id);
    }
}
