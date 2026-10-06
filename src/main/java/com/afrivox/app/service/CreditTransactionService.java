package com.afrivox.app.service;

import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.repository.CreditTransactionRepository;
import com.afrivox.app.service.dto.CreditTransactionDTO;
import com.afrivox.app.service.mapper.CreditTransactionMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.CreditTransaction}.
 */
@Service
@Transactional
public class CreditTransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditTransactionService.class);

    private final CreditTransactionRepository creditTransactionRepository;

    private final CreditTransactionMapper creditTransactionMapper;

    public CreditTransactionService(
        CreditTransactionRepository creditTransactionRepository,
        CreditTransactionMapper creditTransactionMapper
    ) {
        this.creditTransactionRepository = creditTransactionRepository;
        this.creditTransactionMapper = creditTransactionMapper;
    }

    /**
     * Save a creditTransaction.
     *
     * @param creditTransactionDTO the entity to save.
     * @return the persisted entity.
     */
    public CreditTransactionDTO save(CreditTransactionDTO creditTransactionDTO) {
        LOG.debug("Request to save CreditTransaction : {}", creditTransactionDTO);
        CreditTransaction creditTransaction = creditTransactionMapper.toEntity(creditTransactionDTO);
        creditTransaction = creditTransactionRepository.save(creditTransaction);
        return creditTransactionMapper.toDto(creditTransaction);
    }

    /**
     * Update a creditTransaction.
     *
     * @param creditTransactionDTO the entity to save.
     * @return the persisted entity.
     */
    public CreditTransactionDTO update(CreditTransactionDTO creditTransactionDTO) {
        LOG.debug("Request to update CreditTransaction : {}", creditTransactionDTO);
        CreditTransaction creditTransaction = creditTransactionMapper.toEntity(creditTransactionDTO);
        creditTransaction = creditTransactionRepository.save(creditTransaction);
        return creditTransactionMapper.toDto(creditTransaction);
    }

    /**
     * Partially update a creditTransaction.
     *
     * @param creditTransactionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CreditTransactionDTO> partialUpdate(CreditTransactionDTO creditTransactionDTO) {
        LOG.debug("Request to partially update CreditTransaction : {}", creditTransactionDTO);

        return creditTransactionRepository
            .findById(creditTransactionDTO.getId())
            .map(existingCreditTransaction -> {
                creditTransactionMapper.partialUpdate(existingCreditTransaction, creditTransactionDTO);

                return existingCreditTransaction;
            })
            .map(creditTransactionRepository::save)
            .map(creditTransactionMapper::toDto);
    }

    /**
     * Get all the creditTransactions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<CreditTransactionDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all CreditTransactions");
        return creditTransactionRepository.findAll(pageable).map(creditTransactionMapper::toDto);
    }

    /**
     * Get all the creditTransactions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CreditTransactionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return creditTransactionRepository.findAllWithEagerRelationships(pageable).map(creditTransactionMapper::toDto);
    }

    /**
     * Get one creditTransaction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CreditTransactionDTO> findOne(Long id) {
        LOG.debug("Request to get CreditTransaction : {}", id);
        return creditTransactionRepository.findOneWithEagerRelationships(id).map(creditTransactionMapper::toDto);
    }

    /**
     * Delete the creditTransaction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete CreditTransaction : {}", id);
        creditTransactionRepository.deleteById(id);
    }
}
