package com.afrivox.app.web.rest;

import com.afrivox.app.repository.DatasetRequestRepository;
import com.afrivox.app.service.DatasetRequestService;
import com.afrivox.app.service.dto.DatasetRequestDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.afrivox.app.domain.DatasetRequest}.
 */
@RestController
@RequestMapping("/api/dataset-requests")
public class DatasetRequestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetRequestResource.class);

    private static final String ENTITY_NAME = "datasetRequest";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final DatasetRequestService datasetRequestService;

    private final DatasetRequestRepository datasetRequestRepository;

    public DatasetRequestResource(DatasetRequestService datasetRequestService, DatasetRequestRepository datasetRequestRepository) {
        this.datasetRequestService = datasetRequestService;
        this.datasetRequestRepository = datasetRequestRepository;
    }

    /**
     * {@code POST  /dataset-requests} : Create a new datasetRequest.
     *
     * @param datasetRequestDTO the datasetRequestDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new datasetRequestDTO, or with status {@code 400 (Bad Request)} if the datasetRequest has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DatasetRequestDTO> createDatasetRequest(@Valid @RequestBody DatasetRequestDTO datasetRequestDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save DatasetRequest : {}", datasetRequestDTO);
        if (datasetRequestDTO.getId() != null) {
            throw new BadRequestAlertException("A new datasetRequest cannot already have an ID", ENTITY_NAME, "idexists");
        }
        datasetRequestDTO = datasetRequestService.save(datasetRequestDTO);
        return ResponseEntity.created(new URI("/api/dataset-requests/" + datasetRequestDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, datasetRequestDTO.getId().toString()))
            .body(datasetRequestDTO);
    }

    /**
     * {@code PUT  /dataset-requests/:id} : Updates an existing datasetRequest.
     *
     * @param id the id of the datasetRequestDTO to save.
     * @param datasetRequestDTO the datasetRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetRequestDTO,
     * or with status {@code 400 (Bad Request)} if the datasetRequestDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the datasetRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DatasetRequestDTO> updateDatasetRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DatasetRequestDTO datasetRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DatasetRequest : {}, {}", id, datasetRequestDTO);
        if (datasetRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        datasetRequestDTO = datasetRequestService.update(datasetRequestDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetRequestDTO.getId().toString()))
            .body(datasetRequestDTO);
    }

    /**
     * {@code PATCH  /dataset-requests/:id} : Partial updates given fields of an existing datasetRequest, field will ignore if it is null
     *
     * @param id the id of the datasetRequestDTO to save.
     * @param datasetRequestDTO the datasetRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetRequestDTO,
     * or with status {@code 400 (Bad Request)} if the datasetRequestDTO is not valid,
     * or with status {@code 404 (Not Found)} if the datasetRequestDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the datasetRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DatasetRequestDTO> partialUpdateDatasetRequest(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DatasetRequestDTO datasetRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DatasetRequest partially : {}, {}", id, datasetRequestDTO);
        if (datasetRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DatasetRequestDTO> result = datasetRequestService.partialUpdate(datasetRequestDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /dataset-requests} : get all the Dataset Requests.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Dataset Requests in body.
     */
    @GetMapping("")
    public List<DatasetRequestDTO> getAllDatasetRequests(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all DatasetRequests");
        if (eagerload) {
            return datasetRequestService.findAllWithEagerRelationships();
        } else {
            return datasetRequestService.findAll();
        }
    }

    /**
     * {@code GET  /dataset-requests/:id} : get the "id" datasetRequest.
     *
     * @param id the id of the datasetRequestDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the datasetRequestDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatasetRequestDTO> getDatasetRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DatasetRequest : {}", id);
        Optional<DatasetRequestDTO> datasetRequestDTO = datasetRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(datasetRequestDTO);
    }

    /**
     * {@code DELETE  /dataset-requests/:id} : delete the "id" datasetRequest.
     *
     * @param id the id of the datasetRequestDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatasetRequest(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DatasetRequest : {}", id);
        datasetRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
