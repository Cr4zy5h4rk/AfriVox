package com.afrivox.app.web.rest;

import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.service.DatasetVersionService;
import com.afrivox.app.service.dto.DatasetVersionDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.DatasetVersion}.
 */
@RestController
@RequestMapping("/api/dataset-versions")
public class DatasetVersionResource {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetVersionResource.class);

    private static final String ENTITY_NAME = "datasetVersion";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final DatasetVersionService datasetVersionService;

    private final DatasetVersionRepository datasetVersionRepository;

    public DatasetVersionResource(DatasetVersionService datasetVersionService, DatasetVersionRepository datasetVersionRepository) {
        this.datasetVersionService = datasetVersionService;
        this.datasetVersionRepository = datasetVersionRepository;
    }

    /**
     * {@code POST  /dataset-versions} : Create a new datasetVersion.
     *
     * @param datasetVersionDTO the datasetVersionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new datasetVersionDTO, or with status {@code 400 (Bad Request)} if the datasetVersion has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DatasetVersionDTO> createDatasetVersion(@Valid @RequestBody DatasetVersionDTO datasetVersionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save DatasetVersion : {}", datasetVersionDTO);
        if (datasetVersionDTO.getId() != null) {
            throw new BadRequestAlertException("A new datasetVersion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        datasetVersionDTO = datasetVersionService.save(datasetVersionDTO);
        return ResponseEntity.created(new URI("/api/dataset-versions/" + datasetVersionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, datasetVersionDTO.getId().toString()))
            .body(datasetVersionDTO);
    }

    /**
     * {@code PUT  /dataset-versions/:id} : Updates an existing datasetVersion.
     *
     * @param id the id of the datasetVersionDTO to save.
     * @param datasetVersionDTO the datasetVersionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetVersionDTO,
     * or with status {@code 400 (Bad Request)} if the datasetVersionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the datasetVersionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DatasetVersionDTO> updateDatasetVersion(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DatasetVersionDTO datasetVersionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DatasetVersion : {}, {}", id, datasetVersionDTO);
        if (datasetVersionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetVersionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetVersionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        datasetVersionDTO = datasetVersionService.update(datasetVersionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetVersionDTO.getId().toString()))
            .body(datasetVersionDTO);
    }

    /**
     * {@code PATCH  /dataset-versions/:id} : Partial updates given fields of an existing datasetVersion, field will ignore if it is null
     *
     * @param id the id of the datasetVersionDTO to save.
     * @param datasetVersionDTO the datasetVersionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetVersionDTO,
     * or with status {@code 400 (Bad Request)} if the datasetVersionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the datasetVersionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the datasetVersionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DatasetVersionDTO> partialUpdateDatasetVersion(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DatasetVersionDTO datasetVersionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DatasetVersion partially : {}, {}", id, datasetVersionDTO);
        if (datasetVersionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetVersionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetVersionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DatasetVersionDTO> result = datasetVersionService.partialUpdate(datasetVersionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetVersionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /dataset-versions} : get all the Dataset Versions.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Dataset Versions in body.
     */
    @GetMapping("")
    public List<DatasetVersionDTO> getAllDatasetVersions(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all DatasetVersions");
        if (eagerload) {
            return datasetVersionService.findAllWithEagerRelationships();
        } else {
            return datasetVersionService.findAll();
        }
    }

    /**
     * {@code GET  /dataset-versions/:id} : get the "id" datasetVersion.
     *
     * @param id the id of the datasetVersionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the datasetVersionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatasetVersionDTO> getDatasetVersion(@PathVariable("id") Long id) {
        LOG.debug("REST request to get DatasetVersion : {}", id);
        Optional<DatasetVersionDTO> datasetVersionDTO = datasetVersionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(datasetVersionDTO);
    }

    /**
     * {@code DELETE  /dataset-versions/:id} : delete the "id" datasetVersion.
     *
     * @param id the id of the datasetVersionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatasetVersion(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete DatasetVersion : {}", id);
        datasetVersionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
