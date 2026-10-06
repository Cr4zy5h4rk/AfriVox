package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Consent;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ConsentKind;
import com.afrivox.app.repository.ConsentRepository;
import com.afrivox.app.service.dto.ConsentDTO;
import com.afrivox.app.service.mapper.ConsentMapper;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.afrivox.app.domain.Consent}.
 */
@Service
@Transactional
public class ConsentService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentRepository consentRepository;

    private final ConsentMapper consentMapper;

    private final UserProfileService userProfileService;

    private final ApplicationProperties applicationProperties;

    public ConsentService(
        ConsentRepository consentRepository,
        ConsentMapper consentMapper,
        UserProfileService userProfileService,
        ApplicationProperties applicationProperties
    ) {
        this.consentRepository = consentRepository;
        this.consentMapper = consentMapper;
        this.userProfileService = userProfileService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Save a consent.
     *
     * @param consentDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsentDTO save(ConsentDTO consentDTO) {
        LOG.debug("Request to save Consent : {}", consentDTO);
        Consent consent = consentMapper.toEntity(consentDTO);
        consent = consentRepository.save(consent);
        return consentMapper.toDto(consent);
    }

    /**
     * Update a consent.
     *
     * @param consentDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsentDTO update(ConsentDTO consentDTO) {
        LOG.debug("Request to update Consent : {}", consentDTO);
        Consent consent = consentMapper.toEntity(consentDTO);
        consent = consentRepository.save(consent);
        return consentMapper.toDto(consent);
    }

    /**
     * Partially update a consent.
     *
     * @param consentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConsentDTO> partialUpdate(ConsentDTO consentDTO) {
        LOG.debug("Request to partially update Consent : {}", consentDTO);

        return consentRepository
            .findById(consentDTO.getId())
            .map(existingConsent -> {
                consentMapper.partialUpdate(existingConsent, consentDTO);

                return existingConsent;
            })
            .map(consentRepository::save)
            .map(consentMapper::toDto);
    }

    /**
     * Get all the consents.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ConsentDTO> findAll() {
        LOG.debug("Request to get all Consents");
        return consentRepository.findAll().stream().map(consentMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the consents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<ConsentDTO> findAllWithEagerRelationships() {
        return consentRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(consentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one consent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConsentDTO> findOne(Long id) {
        LOG.debug("Request to get Consent : {}", id);
        return consentRepository.findOneWithEagerRelationships(id).map(consentMapper::toDto);
    }

    /**
     * Delete the consent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Consent : {}", id);
        consentRepository.deleteById(id);
    }

    /** Consentements de l'utilisateur connecte. */
    @Transactional(readOnly = true)
    public List<ConsentDTO> findMine() {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        return consentRepository.findAllByProfileIdOrderByGrantedAtDesc(profile.getId()).stream().map(consentMapper::toDto).toList();
    }

    /**
     * Enregistre le consentement de l'utilisateur connecte pour un type donne. La version de la politique est fixee
     * par le serveur (jamais par le client). Idempotent : si un consentement actif existe deja pour ce type, il est
     * renvoye.
     */
    public ConsentDTO grant(ConsentKind kind) {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        Consent consent = consentRepository.findFirstByProfileIdAndKindAndRevokedAtIsNull(profile.getId(), kind).orElseGet(() -> {
            Consent created = new Consent();
            created.setProfile(profile);
            created.setKind(kind);
            created.setPolicyVersion(applicationProperties.getConsent().getCurrentVersion());
            created.setGrantedAt(Instant.now());
            return consentRepository.save(created);
        });
        return consentMapper.toDto(consent);
    }

    /**
     * Retire un consentement de l'utilisateur connecte. On ne supprime rien : {@code revokedAt} est date, ce qui
     * permettra d'exclure ses enregistrements de la version suivante du dataset (a brancher lors de la construction
     * des versions). Renvoie vide si le consentement n'existe pas ou n'appartient pas a l'utilisateur.
     */
    public Optional<ConsentDTO> revoke(Long id) {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        return consentRepository.findOneByIdAndProfileId(id, profile.getId()).map(consent -> {
            if (consent.getRevokedAt() == null) {
                consent.setRevokedAt(Instant.now());
            }
            return consentMapper.toDto(consent);
        });
    }
}
