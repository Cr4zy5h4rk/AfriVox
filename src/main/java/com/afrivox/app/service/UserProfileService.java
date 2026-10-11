package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.User;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ContributorStatus;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.repository.UserRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.service.dto.UserProfileDTO;
import com.afrivox.app.service.mapper.UserProfileMapper;
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
 * Service Implementation for managing {@link com.afrivox.app.domain.UserProfile}.
 */
@Service
@Transactional
public class UserProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    private final UserProfileMapper userProfileMapper;

    private final UserRepository userRepository;

    private final ApplicationProperties applicationProperties;

    public UserProfileService(
        UserProfileRepository userProfileRepository,
        UserProfileMapper userProfileMapper,
        UserRepository userRepository,
        ApplicationProperties applicationProperties
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.userRepository = userRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Save a userProfile.
     *
     * @param userProfileDTO the entity to save.
     * @return the persisted entity.
     */
    /**
     * Profil de l'utilisateur connecte. Un contributeur a deja le sien (cree a l'inscription, avec sa majorite et sa
     * langue maternelle) ; pour les autres roles il est cree au premier usage, le nom affiche etant le login.
     */
    public UserProfile getOrCreateCurrent() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Aucun utilisateur connecte"));
        return userProfileRepository.findOneByUserLogin(login).orElseGet(() -> {
            User user = userRepository.findOneByLogin(login).orElseThrow(() -> new IllegalStateException("Compte introuvable"));
            return create(user, null, null);
        });
    }

    /**
     * Cree le profil d'un compte. Un compte contributeur commence en periode d'essai (si elle est activee) ; les autres
     * roles ne sont pas concernes.
     *
     * @param nativeLanguage code de langue declare a l'inscription (deja valide par l'appelant), ou {@code null}
     * @param adultConfirmedAt moment de la confirmation de majorite, ou {@code null}
     */
    public UserProfile create(User user, String nativeLanguage, Instant adultConfirmedAt) {
        String login = user.getLogin();
        UserProfile profile = new UserProfile();
        profile.setDisplayName(login.length() > 80 ? login.substring(0, 80) : login);
        profile.setCreatedAt(Instant.now());
        profile.setUser(user);
        profile.setNativeLanguage(nativeLanguage);
        profile.setAdultConfirmedAt(adultConfirmedAt);
        boolean contributor = user
            .getAuthorities()
            .stream()
            .anyMatch(a -> AuthoritiesConstants.CONTRIBUTOR.equals(a.getName()));
        if (contributor && applicationProperties.getContributorTrial().isEnabled()) {
            profile.setContributorStatus(ContributorStatus.TRIAL);
        }
        return userProfileRepository.save(profile);
    }

    /**
     * Les champs geres par le serveur (periode d'essai, majorite, informations declarees) ne figurent pas tous dans le
     * DTO du CRUD genere : un PUT ne doit donc pas les remettre a zero, on garde ceux de la base.
     */
    private void keepServerManaged(UserProfile target) {
        if (target.getId() != null) {
            userProfileRepository.findById(target.getId()).ifPresent(existing -> {
                if (target.getContributorStatus() == null) {
                    target.setContributorStatus(existing.getContributorStatus());
                }
                target.setAdultConfirmedAt(existing.getAdultConfirmedAt());
                target.setNativeLanguage(existing.getNativeLanguage());
                target.setAgeRange(existing.getAgeRange());
                target.setGender(existing.getGender());
                target.setRegion(existing.getRegion());
                target.setOtherLanguages(existing.getOtherLanguages());
                target.setDemographicsAnsweredAt(existing.getDemographicsAnsweredAt());
            });
        }
        if (target.getContributorStatus() == null) {
            target.setContributorStatus(ContributorStatus.ACTIVE);
        }
    }

    public UserProfileDTO save(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to save UserProfile : {}", userProfileDTO);
        UserProfile userProfile = userProfileMapper.toEntity(userProfileDTO);
        keepServerManaged(userProfile);
        userProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toDto(userProfile);
    }

    /**
     * Update a userProfile.
     *
     * @param userProfileDTO the entity to save.
     * @return the persisted entity.
     */
    public UserProfileDTO update(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to update UserProfile : {}", userProfileDTO);
        UserProfile userProfile = userProfileMapper.toEntity(userProfileDTO);
        keepServerManaged(userProfile);
        userProfile = userProfileRepository.save(userProfile);
        return userProfileMapper.toDto(userProfile);
    }

    /**
     * Partially update a userProfile.
     *
     * @param userProfileDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserProfileDTO> partialUpdate(UserProfileDTO userProfileDTO) {
        LOG.debug("Request to partially update UserProfile : {}", userProfileDTO);

        return userProfileRepository
            .findById(userProfileDTO.getId())
            .map(existingUserProfile -> {
                userProfileMapper.partialUpdate(existingUserProfile, userProfileDTO);

                return existingUserProfile;
            })
            .map(userProfileRepository::save)
            .map(userProfileMapper::toDto);
    }

    /**
     * Get all the userProfiles.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> findAll() {
        LOG.debug("Request to get all UserProfiles");
        return userProfileRepository.findAll().stream().map(userProfileMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the userProfiles with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<UserProfileDTO> findAllWithEagerRelationships() {
        return userProfileRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(userProfileMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one userProfile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> findOne(Long id) {
        LOG.debug("Request to get UserProfile : {}", id);
        return userProfileRepository.findOneWithEagerRelationships(id).map(userProfileMapper::toDto);
    }

    /**
     * Delete the userProfile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete UserProfile : {}", id);
        userProfileRepository.deleteById(id);
    }
}
