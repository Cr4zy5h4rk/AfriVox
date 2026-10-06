package com.afrivox.app.web.rest;

import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.service.MlModelService;
import com.afrivox.app.service.dto.MlModelDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.MlModel}.
 */
@RestController
@RequestMapping("/api/ml-models")
public class MlModelResource {

    private static final Logger LOG = LoggerFactory.getLogger(MlModelResource.class);

    private static final String ENTITY_NAME = "mlModel";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final MlModelService mlModelService;

    private final MlModelRepository mlModelRepository;

    public MlModelResource(MlModelService mlModelService, MlModelRepository mlModelRepository) {
        this.mlModelService = mlModelService;
        this.mlModelRepository = mlModelRepository;
    }

    /**
     * {@code POST  /ml-models} : Create a new mlModel.
     *
     * @param mlModelDTO the mlModelDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new mlModelDTO, or with status {@code 400 (Bad Request)} if the mlModel has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MlModelDTO> createMlModel(@Valid @RequestBody MlModelDTO mlModelDTO) throws URISyntaxException {
        LOG.debug("REST request to save MlModel : {}", mlModelDTO);
        if (mlModelDTO.getId() != null) {
            throw new BadRequestAlertException("A new mlModel cannot already have an ID", ENTITY_NAME, "idexists");
        }
        mlModelDTO = mlModelService.save(mlModelDTO);
        return ResponseEntity.created(new URI("/api/ml-models/" + mlModelDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, mlModelDTO.getId().toString()))
            .body(mlModelDTO);
    }

    /**
     * {@code PUT  /ml-models/:id} : Updates an existing mlModel.
     *
     * @param id the id of the mlModelDTO to save.
     * @param mlModelDTO the mlModelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated mlModelDTO,
     * or with status {@code 400 (Bad Request)} if the mlModelDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the mlModelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MlModelDTO> updateMlModel(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MlModelDTO mlModelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MlModel : {}, {}", id, mlModelDTO);
        if (mlModelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, mlModelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!mlModelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        mlModelDTO = mlModelService.update(mlModelDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, mlModelDTO.getId().toString()))
            .body(mlModelDTO);
    }

    /**
     * {@code PATCH  /ml-models/:id} : Partial updates given fields of an existing mlModel, field will ignore if it is null
     *
     * @param id the id of the mlModelDTO to save.
     * @param mlModelDTO the mlModelDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated mlModelDTO,
     * or with status {@code 400 (Bad Request)} if the mlModelDTO is not valid,
     * or with status {@code 404 (Not Found)} if the mlModelDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the mlModelDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MlModelDTO> partialUpdateMlModel(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MlModelDTO mlModelDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MlModel partially : {}, {}", id, mlModelDTO);
        if (mlModelDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, mlModelDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!mlModelRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MlModelDTO> result = mlModelService.partialUpdate(mlModelDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, mlModelDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /ml-models} : get all the Ml Models.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Ml Models in body.
     */
    @GetMapping("")
    public List<MlModelDTO> getAllMlModels(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all MlModels");
        if (eagerload) {
            return mlModelService.findAllWithEagerRelationships();
        } else {
            return mlModelService.findAll();
        }
    }

    /**
     * {@code GET  /ml-models/:id} : get the "id" mlModel.
     *
     * @param id the id of the mlModelDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the mlModelDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MlModelDTO> getMlModel(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MlModel : {}", id);
        Optional<MlModelDTO> mlModelDTO = mlModelService.findOne(id);
        return ResponseUtil.wrapOrNotFound(mlModelDTO);
    }

    /**
     * {@code DELETE  /ml-models/:id} : delete the "id" mlModel.
     *
     * @param id the id of the mlModelDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMlModel(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MlModel : {}", id);
        mlModelService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
