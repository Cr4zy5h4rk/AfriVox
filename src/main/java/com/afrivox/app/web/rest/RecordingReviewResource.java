package com.afrivox.app.web.rest;

import com.afrivox.app.repository.RecordingReviewRepository;
import com.afrivox.app.service.RecordingReviewService;
import com.afrivox.app.service.dto.RecordingReviewDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.RecordingReview}.
 */
@RestController
@RequestMapping("/api/recording-reviews")
public class RecordingReviewResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingReviewResource.class);

    private static final String ENTITY_NAME = "recordingReview";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final RecordingReviewService recordingReviewService;

    private final RecordingReviewRepository recordingReviewRepository;

    public RecordingReviewResource(RecordingReviewService recordingReviewService, RecordingReviewRepository recordingReviewRepository) {
        this.recordingReviewService = recordingReviewService;
        this.recordingReviewRepository = recordingReviewRepository;
    }

    /**
     * {@code POST  /recording-reviews} : Create a new recordingReview.
     *
     * @param recordingReviewDTO the recordingReviewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new recordingReviewDTO, or with status {@code 400 (Bad Request)} if the recordingReview has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RecordingReviewDTO> createRecordingReview(@Valid @RequestBody RecordingReviewDTO recordingReviewDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RecordingReview : {}", recordingReviewDTO);
        if (recordingReviewDTO.getId() != null) {
            throw new BadRequestAlertException("A new recordingReview cannot already have an ID", ENTITY_NAME, "idexists");
        }
        recordingReviewDTO = recordingReviewService.save(recordingReviewDTO);
        return ResponseEntity.created(new URI("/api/recording-reviews/" + recordingReviewDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, recordingReviewDTO.getId().toString()))
            .body(recordingReviewDTO);
    }

    /**
     * {@code PUT  /recording-reviews/:id} : Updates an existing recordingReview.
     *
     * @param id the id of the recordingReviewDTO to save.
     * @param recordingReviewDTO the recordingReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recordingReviewDTO,
     * or with status {@code 400 (Bad Request)} if the recordingReviewDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the recordingReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecordingReviewDTO> updateRecordingReview(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RecordingReviewDTO recordingReviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RecordingReview : {}, {}", id, recordingReviewDTO);
        if (recordingReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recordingReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recordingReviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        recordingReviewDTO = recordingReviewService.update(recordingReviewDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, recordingReviewDTO.getId().toString()))
            .body(recordingReviewDTO);
    }

    /**
     * {@code PATCH  /recording-reviews/:id} : Partial updates given fields of an existing recordingReview, field will ignore if it is null
     *
     * @param id the id of the recordingReviewDTO to save.
     * @param recordingReviewDTO the recordingReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated recordingReviewDTO,
     * or with status {@code 400 (Bad Request)} if the recordingReviewDTO is not valid,
     * or with status {@code 404 (Not Found)} if the recordingReviewDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the recordingReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RecordingReviewDTO> partialUpdateRecordingReview(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RecordingReviewDTO recordingReviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RecordingReview partially : {}, {}", id, recordingReviewDTO);
        if (recordingReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, recordingReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!recordingReviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RecordingReviewDTO> result = recordingReviewService.partialUpdate(recordingReviewDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, recordingReviewDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /recording-reviews} : get all the Recording Reviews.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recording Reviews in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RecordingReviewDTO>> getAllRecordingReviews(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of RecordingReviews");
        Page<RecordingReviewDTO> page;
        if (eagerload) {
            page = recordingReviewService.findAllWithEagerRelationships(pageable);
        } else {
            page = recordingReviewService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /recording-reviews/:id} : get the "id" recordingReview.
     *
     * @param id the id of the recordingReviewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recordingReviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordingReviewDTO> getRecordingReview(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RecordingReview : {}", id);
        Optional<RecordingReviewDTO> recordingReviewDTO = recordingReviewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recordingReviewDTO);
    }

    /**
     * {@code DELETE  /recording-reviews/:id} : delete the "id" recordingReview.
     *
     * @param id the id of the recordingReviewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecordingReview(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RecordingReview : {}", id);
        recordingReviewService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
