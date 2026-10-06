package com.afrivox.app.web.rest;

import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.service.LanguageQualificationService;
import com.afrivox.app.service.dto.LanguageQualificationDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.LanguageQualification}.
 */
@RestController
@RequestMapping("/api/language-qualifications")
public class LanguageQualificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageQualificationResource.class);

    private static final String ENTITY_NAME = "languageQualification";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final LanguageQualificationService languageQualificationService;

    private final LanguageQualificationRepository languageQualificationRepository;

    public LanguageQualificationResource(
        LanguageQualificationService languageQualificationService,
        LanguageQualificationRepository languageQualificationRepository
    ) {
        this.languageQualificationService = languageQualificationService;
        this.languageQualificationRepository = languageQualificationRepository;
    }

    /**
     * {@code POST  /language-qualifications} : Create a new languageQualification.
     *
     * @param languageQualificationDTO the languageQualificationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new languageQualificationDTO, or with status {@code 400 (Bad Request)} if the languageQualification has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<LanguageQualificationDTO> createLanguageQualification(
        @Valid @RequestBody LanguageQualificationDTO languageQualificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save LanguageQualification : {}", languageQualificationDTO);
        if (languageQualificationDTO.getId() != null) {
            throw new BadRequestAlertException("A new languageQualification cannot already have an ID", ENTITY_NAME, "idexists");
        }
        languageQualificationDTO = languageQualificationService.save(languageQualificationDTO);
        return ResponseEntity.created(new URI("/api/language-qualifications/" + languageQualificationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, languageQualificationDTO.getId().toString()))
            .body(languageQualificationDTO);
    }

    /**
     * {@code PUT  /language-qualifications/:id} : Updates an existing languageQualification.
     *
     * @param id the id of the languageQualificationDTO to save.
     * @param languageQualificationDTO the languageQualificationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated languageQualificationDTO,
     * or with status {@code 400 (Bad Request)} if the languageQualificationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the languageQualificationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<LanguageQualificationDTO> updateLanguageQualification(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody LanguageQualificationDTO languageQualificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update LanguageQualification : {}, {}", id, languageQualificationDTO);
        if (languageQualificationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, languageQualificationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!languageQualificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        languageQualificationDTO = languageQualificationService.update(languageQualificationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, languageQualificationDTO.getId().toString()))
            .body(languageQualificationDTO);
    }

    /**
     * {@code PATCH  /language-qualifications/:id} : Partial updates given fields of an existing languageQualification, field will ignore if it is null
     *
     * @param id the id of the languageQualificationDTO to save.
     * @param languageQualificationDTO the languageQualificationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated languageQualificationDTO,
     * or with status {@code 400 (Bad Request)} if the languageQualificationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the languageQualificationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the languageQualificationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<LanguageQualificationDTO> partialUpdateLanguageQualification(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody LanguageQualificationDTO languageQualificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update LanguageQualification partially : {}, {}", id, languageQualificationDTO);
        if (languageQualificationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, languageQualificationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!languageQualificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<LanguageQualificationDTO> result = languageQualificationService.partialUpdate(languageQualificationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, languageQualificationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /language-qualifications} : get all the Language Qualifications.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Language Qualifications in body.
     */
    @GetMapping("")
    public List<LanguageQualificationDTO> getAllLanguageQualifications(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all LanguageQualifications");
        if (eagerload) {
            return languageQualificationService.findAllWithEagerRelationships();
        } else {
            return languageQualificationService.findAll();
        }
    }

    /**
     * {@code GET  /language-qualifications/:id} : get the "id" languageQualification.
     *
     * @param id the id of the languageQualificationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the languageQualificationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LanguageQualificationDTO> getLanguageQualification(@PathVariable("id") Long id) {
        LOG.debug("REST request to get LanguageQualification : {}", id);
        Optional<LanguageQualificationDTO> languageQualificationDTO = languageQualificationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(languageQualificationDTO);
    }

    /**
     * {@code DELETE  /language-qualifications/:id} : delete the "id" languageQualification.
     *
     * @param id the id of the languageQualificationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLanguageQualification(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete LanguageQualification : {}", id);
        languageQualificationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
