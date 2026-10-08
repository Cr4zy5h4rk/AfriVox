package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.afrivox.app.repository.DatasetRequestRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demandes de dataset sur mesure déposées par les organisations. Le demandeur, le statut et la date sont fixés ici,
 * jamais par le client : une organisation ne peut ni choisir son statut, ni fixer son devis, ni voir les demandes
 * des autres.
 */
@Service
@Transactional
public class OrganizationRequestService {

    private static final String ENTITY_NAME = "datasetRequest";

    /** Plafond de demandes non traitées par demandeur (limite l'abus ; l'admin les traite une à une). */
    private static final int MAX_PENDING = 10;

    /** Données saisies par l'organisation. */
    public record NewRequest(
        String orgName,
        Sector sector,
        String languageCode,
        Integer hours,
        Integer speakersTarget,
        String conversationType,
        UsageKind usageKind,
        LocalDate deadline,
        String description
    ) {}

    /** Ce que l'organisation voit de sa demande (rien du profil du demandeur). */
    public record RequestView(
        Long id,
        String orgName,
        Sector sector,
        String languageCode,
        String languageName,
        Integer hours,
        Integer speakersTarget,
        String conversationType,
        UsageKind usageKind,
        LocalDate deadline,
        String description,
        RequestStatus status,
        Integer quoteAmountFcfa,
        Instant createdAt
    ) {}

    private final DatasetRequestRepository requestRepository;
    private final LanguageRepository languageRepository;
    private final UserProfileService userProfileService;

    public OrganizationRequestService(
        DatasetRequestRepository requestRepository,
        LanguageRepository languageRepository,
        UserProfileService userProfileService
    ) {
        this.requestRepository = requestRepository;
        this.languageRepository = languageRepository;
        this.userProfileService = userProfileService;
    }

    public RequestView submit(NewRequest input) {
        UserProfile requester = userProfileService.getOrCreateCurrent();

        long pending = requestRepository
            .findAllByRequesterId(requester.getId())
            .stream()
            .filter(r -> r.getStatus() == RequestStatus.RECEIVED)
            .count();
        if (pending >= MAX_PENDING) {
            throw new BadRequestAlertException("Trop de demandes en attente", ENTITY_NAME, "toomanyrequests");
        }
        if (input.deadline() != null && !input.deadline().isAfter(LocalDate.now())) {
            throw new BadRequestAlertException("L'échéance doit être dans le futur", ENTITY_NAME, "invaliddeadline");
        }
        Language language = languageRepository
            .findOneByCode(input.languageCode().trim().toLowerCase())
            .orElseThrow(() -> new BadRequestAlertException("Langue inconnue", ENTITY_NAME, "languagenotfound"));

        DatasetRequest request = new DatasetRequest();
        request.setOrgName(input.orgName().trim());
        request.setSector(input.sector());
        request.setLanguage(language);
        request.setHours(input.hours());
        request.setSpeakersTarget(input.speakersTarget());
        request.setConversationType(blankToNull(input.conversationType()));
        request.setUsageKind(input.usageKind());
        request.setDeadline(input.deadline());
        request.setDescription(blankToNull(input.description()));
        // Champs réservés au serveur
        request.setStatus(RequestStatus.RECEIVED);
        request.setCreatedAt(Instant.now());
        request.setRequester(requester);
        return toView(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<RequestView> mine() {
        UserProfile requester = userProfileService.getOrCreateCurrent();
        return requestRepository.findAllByRequesterId(requester.getId()).stream().map(OrganizationRequestService::toView).toList();
    }

    private static RequestView toView(DatasetRequest r) {
        Language language = r.getLanguage();
        return new RequestView(
            r.getId(),
            r.getOrgName(),
            r.getSector(),
            language == null ? null : language.getCode(),
            language == null ? null : language.getName(),
            r.getHours(),
            r.getSpeakersTarget(),
            r.getConversationType(),
            r.getUsageKind(),
            r.getDeadline(),
            r.getDescription(),
            r.getStatus(),
            r.getQuoteAmountFcfa(),
            r.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
