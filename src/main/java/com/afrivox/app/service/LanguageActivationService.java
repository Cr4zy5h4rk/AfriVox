package com.afrivox.app.service;

import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.enumeration.LanguageStatus;
import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.repository.LanguageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regle du projet : une langue devient ACTIVE quand elle atteint son seuil de validateurs qualifies.
 * Decision : l'activation est automatique, mais jamais l'inverse. Repasser une langue en liste d'attente
 * (ou revoquer un validateur) reste une decision de l'admin, pour ne pas fermer une langue en pleine collecte.
 */
@Service
@Transactional
public class LanguageActivationService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageActivationService.class);

    private final LanguageRepository languageRepository;
    private final LanguageQualificationRepository qualificationRepository;

    public LanguageActivationService(LanguageRepository languageRepository, LanguageQualificationRepository qualificationRepository) {
        this.languageRepository = languageRepository;
        this.qualificationRepository = qualificationRepository;
    }

    /** Active la langue si elle est en attente et que le seuil est atteint. Sans effet si {@code languageId} est nul. */
    public void promoteIfReady(Long languageId) {
        if (languageId == null) {
            return;
        }
        languageRepository
            .findById(languageId)
            .filter(language -> language.getStatus() == LanguageStatus.WAITLIST)
            .filter(this::thresholdReached)
            .ifPresent(language -> {
                language.setStatus(LanguageStatus.ACTIVE);
                languageRepository.save(language);
                LOG.info(
                    "Langue {} activee automatiquement : seuil de {} validateurs qualifies atteint",
                    language.getCode(),
                    language.getMinValidators()
                );
            });
    }

    private boolean thresholdReached(Language language) {
        Integer min = language.getMinValidators();
        if (min == null || min < 1) {
            return false;
        }
        return qualificationRepository.countByLanguageIdAndStatus(language.getId(), QualificationStatus.QUALIFIED) >= min;
    }
}
