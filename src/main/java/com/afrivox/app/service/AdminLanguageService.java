package com.afrivox.app.service;

import com.afrivox.app.domain.enumeration.LanguageStatus;
import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.domain.enumeration.ScriptKind;
import com.afrivox.app.domain.enumeration.TextDirection;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.RecordingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord des langues pour l'admin : où en est chaque langue vers son seuil d'activation
 * (validateurs qualifiés) et combien d'enregistrements elle a reçus. Tout est calculé, rien n'est stocké.
 */
@Service
@Transactional(readOnly = true)
public class AdminLanguageService {

    /** Une ligne du tableau. */
    public record AdminLanguageView(
        Long id,
        String code,
        String name,
        String dialect,
        ScriptKind script,
        TextDirection direction,
        LanguageStatus status,
        int minValidators,
        long qualifiedValidators,
        long pendingValidators,
        long recordingsSubmitted,
        long recordingsValidated
    ) {}

    private final LanguageRepository languageRepository;
    private final LanguageQualificationRepository qualificationRepository;
    private final RecordingRepository recordingRepository;

    public AdminLanguageService(
        LanguageRepository languageRepository,
        LanguageQualificationRepository qualificationRepository,
        RecordingRepository recordingRepository
    ) {
        this.languageRepository = languageRepository;
        this.qualificationRepository = qualificationRepository;
        this.recordingRepository = recordingRepository;
    }

    public List<AdminLanguageView> overview() {
        Map<Long, Long> qualified = countsByLanguage(QualificationStatus.QUALIFIED);
        Map<Long, Long> pending = countsByLanguage(QualificationStatus.PENDING);

        // Enregistrements en attente de validation / validés, par langue (les autres statuts n'intéressent pas l'admin)
        Map<Long, Long> submitted = new HashMap<>();
        Map<Long, Long> validated = new HashMap<>();
        for (Object[] row : recordingRepository.countByLanguageAndStatus()) {
            Long languageId = (Long) row[0];
            long count = (Long) row[2];
            if (row[1] == RecordingStatus.SUBMITTED) {
                submitted.merge(languageId, count, Long::sum);
            } else if (row[1] == RecordingStatus.VALIDATED) {
                validated.merge(languageId, count, Long::sum);
            }
        }

        return languageRepository
            .findAllByOrderByNameAsc()
            .stream()
            .map(l ->
                new AdminLanguageView(
                    l.getId(),
                    l.getCode(),
                    l.getName(),
                    l.getDialect(),
                    l.getScript(),
                    l.getDirection(),
                    l.getStatus(),
                    l.getMinValidators() == null ? 0 : l.getMinValidators(),
                    qualified.getOrDefault(l.getId(), 0L),
                    pending.getOrDefault(l.getId(), 0L),
                    submitted.getOrDefault(l.getId(), 0L),
                    validated.getOrDefault(l.getId(), 0L)
                )
            )
            .toList();
    }

    private Map<Long, Long> countsByLanguage(QualificationStatus status) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : qualificationRepository.countByLanguageForStatus(status)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }
}
