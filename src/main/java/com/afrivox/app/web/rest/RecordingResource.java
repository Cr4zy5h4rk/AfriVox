package com.afrivox.app.web.rest;

import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.service.RecordingService;
import com.afrivox.app.service.dto.RecordingDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.Recording}.
 */
@RestController
@RequestMapping("/api/recordings")
public class RecordingResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingResource.class);

    private static final String ENTITY_NAME = "recording";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final RecordingService recordingService;

    private final RecordingRepository recordingRepository;

    public RecordingResource(RecordingService recordingService, RecordingRepository recordingRepository) {
        this.recordingService = recordingService;
        this.recordingRepository = recordingRepository;
    }

    /**
     * {@code POST  /recordings} : Create a new recording.
     *
     * @param recordingDTO the recordingDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recordingDTO, or with status {@code 400 (Bad Request)} if the recording has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecordingDTO> createRecording(@Valid @RequestBody RecordingDTO recordingDTO) throws URISyntaxException {
        LOG.debug("REST request to save Recording : {}", recordingDTO);
        if (recordingDTO.getId() != null) {
            throw new BadRequestAlertException("A new recording cannot already have an ID", ENTITY_NAME, "idexists");
        }
        recordingDTO = recordingService.save(recordingDTO);
        return ResponseEntity.created(new URI("/api/recordings/" + recordingDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, recordingDTO.getId().toString()))
            .body(recordingDTO);
    }

    /**
     * {@code PUT  /recordings/:id} : Updates an existing recording.
     *
     * @param id the id of the recordingDTO to save.
     * @param recordingDTO the recordingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recordingDTO,
     * or with status {@code 400 (Bad Request)} if the recordingDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the recordingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecordingDTO> updateRecording(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RecordingDTO recordingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Recording : {}, {}", id, recordingDTO);
        if (recordingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recordingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recordingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        recordingDTO = recordingService.update(recordingDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, recordingDTO.getId().toString()))
            .body(recordingDTO);
    }

    /**
     * {@code PATCH  /recordings/:id} : Partial updates given fields of an existing recording, field will ignore if it is null
     *
     * @param id the id of the recordingDTO to save.
     * @param recordingDTO the recordingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recordingDTO,
     * or with status {@code 400 (Bad Request)} if the recordingDTO is not valid,
     * or with status {@code 404 (Not Found)} if the recordingDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the recordingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RecordingDTO> partialUpdateRecording(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RecordingDTO recordingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Recording partially : {}, {}", id, recordingDTO);
        if (recordingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recordingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recordingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RecordingDTO> result = recordingService.partialUpdate(recordingDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, recordingDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /recordings} : get all the Recordings.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recordings in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RecordingDTO>> getAllRecordings(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Recordings");
        Page<RecordingDTO> page;
        if (eagerload) {
            page = recordingService.findAllWithEagerRelationships(pageable);
        } else {
            page = recordingService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /recordings/:id} : get the "id" recording.
     *
     * @param id the id of the recordingDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recordingDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordingDTO> getRecording(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Recording : {}", id);
        Optional<RecordingDTO> recordingDTO = recordingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recordingDTO);
    }

    /**
     * {@code DELETE  /recordings/:id} : delete the "id" recording.
     *
     * @param id the id of the recordingDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecording(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Recording : {}", id);
        recordingService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
