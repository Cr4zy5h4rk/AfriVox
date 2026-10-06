package com.afrivox.app.web.rest;

import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.service.DownloadLogService;
import com.afrivox.app.service.dto.DownloadLogDTO;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.afrivox.app.domain.DownloadLog}.
 */
@RestController
@RequestMapping("/api/download-logs")
public class DownloadLogResource {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadLogResource.class);

    private static final String ENTITY_NAME = "downloadLog";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final DownloadLogService downloadLogService;

    private final DownloadLogRepository downloadLogRepository;

    public DownloadLogResource(DownloadLogService downloadLogService, DownloadLogRepository downloadLogRepository) {
        this.downloadLogService = downloadLogService;
        this.downloadLogRepository = downloadLogRepository;
    }

    /**
     * {@code POST  /download-logs} : Create a new downloadLog.
     *
     * @param downloadLogDTO the downloadLogDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new downloadLogDTO, or with status {@code 400 (Bad Request)} if the downloadLog has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DownloadLogDTO> createDownloadLog(@Valid @RequestBody DownloadLogDTO downloadLogDTO) throws URISyntaxException {
        LOG.debug("REST request to save DownloadLog : {}", downloadLogDTO);
        if (downloadLogDTO.getId() != null) {
            throw new BadRequestAlertException("A new downloadLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        downloadLogDTO = downloadLogService.save(downloadLogDTO);
        return ResponseEntity.created(new URI("/api/download-logs/" + downloadLogDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, downloadLogDTO.getId().toString()))
            .body(downloadLogDTO);
    }

    /**
     * {@code PUT  /download-logs/:id} : Updates an existing downloadLog.
     *
     * @param id the id of the downloadLogDTO to save.
     * @param downloadLogDTO the downloadLogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated downloadLogDTO,
     * or with status {@code 400 (Bad Request)} if the downloadLogDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the downloadLogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DownloadLogDTO> updateDownloadLog(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DownloadLogDTO downloadLogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DownloadLog : {}, {}", id, downloadLogDTO);
        if (downloadLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, downloadLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!downloadLogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        downloadLogDTO = downloadLogService.update(downloadLogDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, downloadLogDTO.getId().toString()))
            .body(downloadLogDTO);
    }

    /**
     * {@code PATCH  /download-logs/:id} : Partial updates given fields of an existing downloadLog, field will ignore if it is null
     *
     * @param id the id of the downloadLogDTO to save.
     * @param downloadLogDTO the downloadLogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated downloadLogDTO,
     * or with status {@code 400 (Bad Request)} if the downloadLogDTO is not valid,
     * or with status {@code 404 (Not Found)} if the downloadLogDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the downloadLogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DownloadLogDTO> partialUpdateDownloadLog(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DownloadLogDTO downloadLogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DownloadLog partially : {}, {}", id, downloadLogDTO);
        if (downloadLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, downloadLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!downloadLogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DownloadLogDTO> result = downloadLogService.partialUpdate(downloadLogDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, downloadLogDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /download-logs} : get all the Download Logs.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Download Logs in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DownloadLogDTO>> getAllDownloadLogs(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of DownloadLogs");
        Page<DownloadLogDTO> page;
        if (eagerload) {
            page = downloadLogService.findAllWithEagerRelationships(pageable);
        } else {
            page = downloadLogService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /download-logs/:id} : get the "id" downloadLog.
     *
     * @param id the id of the downloadLogDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the downloadLogDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DownloadLogDTO> getDownloadLog(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DownloadLog : {}", id);
        Optional<DownloadLogDTO> downloadLogDTO = downloadLogService.findOne(id);
        return ResponseUtil.wrapOrNotFound(downloadLogDTO);
    }

    /**
     * {@code DELETE  /download-logs/:id} : delete the "id" downloadLog.
     *
     * @param id the id of the downloadLogDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDownloadLog(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DownloadLog : {}", id);
        downloadLogService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
