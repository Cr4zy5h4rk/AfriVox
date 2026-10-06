package com.afrivox.app.web.rest;

import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.service.DatasetService;
import com.afrivox.app.service.dto.DatasetDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.Dataset}.
 */
@RestController
@RequestMapping("/api/datasets")
public class DatasetResource {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetResource.class);

    private static final String ENTITY_NAME = "dataset";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final DatasetService datasetService;

    private final DatasetRepository datasetRepository;

    public DatasetResource(DatasetService datasetService, DatasetRepository datasetRepository) {
        this.datasetService = datasetService;
        this.datasetRepository = datasetRepository;
    }

    /**
     * {@code POST  /datasets} : Create a new dataset.
     *
     * @param datasetDTO the datasetDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new datasetDTO, or with status {@code 400 (Bad Request)} if the dataset has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DatasetDTO> createDataset(@Valid @RequestBody DatasetDTO datasetDTO) throws URISyntaxException {
        LOG.debug("REST request to save Dataset : {}", datasetDTO);
        if (datasetDTO.getId() != null) {
            throw new BadRequestAlertException("A new dataset cannot already have an ID", ENTITY_NAME, "idexists");
        }
        datasetDTO = datasetService.save(datasetDTO);
        return ResponseEntity.created(new URI("/api/datasets/" + datasetDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, datasetDTO.getId().toString()))
            .body(datasetDTO);
    }

    /**
     * {@code PUT  /datasets/:id} : Updates an existing dataset.
     *
     * @param id the id of the datasetDTO to save.
     * @param datasetDTO the datasetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetDTO,
     * or with status {@code 400 (Bad Request)} if the datasetDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the datasetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DatasetDTO> updateDataset(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody DatasetDTO datasetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Dataset : {}, {}", id, datasetDTO);
        if (datasetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        datasetDTO = datasetService.update(datasetDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetDTO.getId().toString()))
            .body(datasetDTO);
    }

    /**
     * {@code PATCH  /datasets/:id} : Partial updates given fields of an existing dataset, field will ignore if it is null
     *
     * @param id the id of the datasetDTO to save.
     * @param datasetDTO the datasetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated datasetDTO,
     * or with status {@code 400 (Bad Request)} if the datasetDTO is not valid,
     * or with status {@code 404 (Not Found)} if the datasetDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the datasetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DatasetDTO> partialUpdateDataset(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody DatasetDTO datasetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Dataset partially : {}, {}", id, datasetDTO);
        if (datasetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, datasetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!datasetRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DatasetDTO> result = datasetService.partialUpdate(datasetDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, datasetDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /datasets} : get all the Datasets.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Datasets in body.
     */
    @GetMapping("")
    public List<DatasetDTO> getAllDatasets(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all Datasets");
        if (eagerload) {
            return datasetService.findAllWithEagerRelationships();
        } else {
            return datasetService.findAll();
        }
    }

    /**
     * {@code GET  /datasets/:id} : get the "id" dataset.
     *
     * @param id the id of the datasetDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the datasetDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatasetDTO> getDataset(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Dataset : {}", id);
        Optional<DatasetDTO> datasetDTO = datasetService.findOne(id);
        return ResponseUtil.wrapOrNotFound(datasetDTO);
    }

    /**
     * {@code DELETE  /datasets/:id} : delete the "id" dataset.
     *
     * @param id the id of the datasetDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Dataset : {}", id);
        datasetService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
