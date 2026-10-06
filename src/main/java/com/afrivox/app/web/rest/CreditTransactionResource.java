package com.afrivox.app.web.rest;

import com.afrivox.app.repository.CreditTransactionRepository;
import com.afrivox.app.service.CreditTransactionService;
import com.afrivox.app.service.dto.CreditTransactionDTO;
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
 * REST controller for managing {@link com.afrivox.app.domain.CreditTransaction}.
 */
@RestController
@RequestMapping("/api/credit-transactions")
public class CreditTransactionResource {

    private static final Logger LOG = LoggerFactory.getLogger(CreditTransactionResource.class);

    private static final String ENTITY_NAME = "creditTransaction";

    @Value("${jhipster.clientApp.name:afriVox}")
    private String applicationName;

    private final CreditTransactionService creditTransactionService;

    private final CreditTransactionRepository creditTransactionRepository;

    public CreditTransactionResource(
        CreditTransactionService creditTransactionService,
        CreditTransactionRepository creditTransactionRepository
    ) {
        this.creditTransactionService = creditTransactionService;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    /**
     * {@code POST  /credit-transactions} : Create a new creditTransaction.
     *
     * @param creditTransactionDTO the creditTransactionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new creditTransactionDTO, or with status {@code 400 (Bad Request)} if the creditTransaction has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CreditTransactionDTO> createCreditTransaction(@Valid @RequestBody CreditTransactionDTO creditTransactionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save CreditTransaction : {}", creditTransactionDTO);
        if (creditTransactionDTO.getId() != null) {
            throw new BadRequestAlertException("A new creditTransaction cannot already have an ID", ENTITY_NAME, "idexists");
        }
        creditTransactionDTO = creditTransactionService.save(creditTransactionDTO);
        return ResponseEntity.created(new URI("/api/credit-transactions/" + creditTransactionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, creditTransactionDTO.getId().toString()))
            .body(creditTransactionDTO);
    }

    /**
     * {@code PUT  /credit-transactions/:id} : Updates an existing creditTransaction.
     *
     * @param id the id of the creditTransactionDTO to save.
     * @param creditTransactionDTO the creditTransactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated creditTransactionDTO,
     * or with status {@code 400 (Bad Request)} if the creditTransactionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the creditTransactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CreditTransactionDTO> updateCreditTransaction(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody CreditTransactionDTO creditTransactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CreditTransaction : {}, {}", id, creditTransactionDTO);
        if (creditTransactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, creditTransactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!creditTransactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        creditTransactionDTO = creditTransactionService.update(creditTransactionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, creditTransactionDTO.getId().toString()))
            .body(creditTransactionDTO);
    }

    /**
     * {@code PATCH  /credit-transactions/:id} : Partial updates given fields of an existing creditTransaction, field will ignore if it is null
     *
     * @param id the id of the creditTransactionDTO to save.
     * @param creditTransactionDTO the creditTransactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated creditTransactionDTO,
     * or with status {@code 400 (Bad Request)} if the creditTransactionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the creditTransactionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the creditTransactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CreditTransactionDTO> partialUpdateCreditTransaction(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody CreditTransactionDTO creditTransactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update CreditTransaction partially : {}, {}", id, creditTransactionDTO);
        if (creditTransactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, creditTransactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!creditTransactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CreditTransactionDTO> result = creditTransactionService.partialUpdate(creditTransactionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, creditTransactionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /credit-transactions} : get all the Credit Transactions.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Credit Transactions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CreditTransactionDTO>> getAllCreditTransactions(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of CreditTransactions");
        Page<CreditTransactionDTO> page;
        if (eagerload) {
            page = creditTransactionService.findAllWithEagerRelationships(pageable);
        } else {
            page = creditTransactionService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /credit-transactions/:id} : get the "id" creditTransaction.
     *
     * @param id the id of the creditTransactionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the creditTransactionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreditTransactionDTO> getCreditTransaction(@PathVariable("id") Long id) {
        LOG.debug("REST request to get CreditTransaction : {}", id);
        Optional<CreditTransactionDTO> creditTransactionDTO = creditTransactionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(creditTransactionDTO);
    }

    /**
     * {@code DELETE  /credit-transactions/:id} : delete the "id" creditTransaction.
     *
     * @param id the id of the creditTransactionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditTransaction(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete CreditTransaction : {}", id);
        creditTransactionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
