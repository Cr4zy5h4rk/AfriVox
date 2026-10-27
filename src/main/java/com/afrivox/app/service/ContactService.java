package com.afrivox.app.service;

import com.afrivox.app.domain.ContactRequest;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ContactStatus;
import com.afrivox.app.repository.ContactRequestRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prise de contact entre une organisation et un developpeur « ouvert aux opportunites ». Le developpeur ne donne
 * jamais son adresse : l'organisation ecrit un message et indique comment la joindre, le developpeur le lit et choisit
 * d'accepter (c'est alors a lui d'ecrire) ou de decliner (l'organisation n'en est pas notifiee).
 * <p>
 * Garde-fous : seule une organisation envoie ; le destinataire doit avoir un profil public ouvert aux opportunites ;
 * une demande en attente par couple ; {@value #MAX_PER_DAY} demandes par jour et par organisation ; texte brut.
 */
@Service
@Transactional
public class ContactService {

    private static final String ENTITY_NAME = "contactRequest";

    static final int MAX_PER_DAY = 5;
    static final int MIN_MESSAGE = 20;
    static final int MAX_MESSAGE = 1000;
    static final int MIN_CONTACT = 3;
    static final int MAX_CONTACT = 120;

    /** Demande recue : le developpeur voit le message et le moyen de joindre l'organisation. */
    public record ReceivedView(
        Long id,
        String senderName,
        String message,
        String contact,
        ContactStatus status,
        Instant createdAt,
        Instant answeredAt
    ) {}

    /** Demande envoyee : l'organisation voit l'etat, jamais de coordonnees du developpeur. */
    public record SentView(
        Long id,
        Long recipientId,
        String recipientName,
        String message,
        ContactStatus status,
        Instant createdAt,
        Instant answeredAt
    ) {}

    private final ContactRequestRepository requestRepository;
    private final UserProfileRepository profileRepository;
    private final UserProfileService userProfileService;
    private final NotificationService notifications;

    public ContactService(
        ContactRequestRepository requestRepository,
        UserProfileRepository profileRepository,
        UserProfileService userProfileService,
        NotificationService notifications
    ) {
        this.requestRepository = requestRepository;
        this.profileRepository = profileRepository;
        this.userProfileService = userProfileService;
        this.notifications = notifications;
    }

    public SentView send(Long recipientId, String message, String contact) {
        UserProfile sender = userProfileService.getOrCreateCurrent();
        String text = message == null ? "" : message.strip();
        String how = contact == null ? "" : contact.strip();
        if (text.length() < MIN_MESSAGE || text.length() > MAX_MESSAGE) {
            throw new BadRequestAlertException("Le message doit faire entre 20 et 1000 caracteres", ENTITY_NAME, "invalidmessage");
        }
        if (how.length() < MIN_CONTACT || how.length() > MAX_CONTACT) {
            throw new BadRequestAlertException("Indiquez comment vous joindre (3 a 120 caracteres)", ENTITY_NAME, "invalidcontact");
        }
        // Meme reponse pour « n'existe pas », « pas public » et « pas ouvert » : on ne revele rien sur un profil prive.
        UserProfile recipient = profileRepository
            .findById(recipientId)
            .filter(p -> p.isPublicProfile() && p.isOpenToWork())
            .orElseThrow(() ->
                new BadRequestAlertException("Ce developpeur n'accepte pas de demandes", ENTITY_NAME, "recipientunavailable")
            );
        if (recipient.getId().equals(sender.getId())) {
            throw new BadRequestAlertException("On ne se contacte pas soi-meme", ENTITY_NAME, "selfcontact");
        }
        if (requestRepository.existsBySenderIdAndRecipientIdAndStatus(sender.getId(), recipient.getId(), ContactStatus.PENDING)) {
            throw new BadRequestAlertException("Une demande est deja en attente", ENTITY_NAME, "alreadypending");
        }
        if (requestRepository.countBySenderIdAndCreatedAtAfter(sender.getId(), Instant.now().minus(Duration.ofDays(1))) >= MAX_PER_DAY) {
            throw new BadRequestAlertException("Limite de demandes atteinte pour aujourd'hui", ENTITY_NAME, "toomanyrequests");
        }
        ContactRequest saved = requestRepository.save(new ContactRequest(sender, recipient, text, how, Instant.now()));
        notifications.notifyActor(recipient, NotificationService.CONTACT_REQUEST, sender.getDisplayName());
        return toSent(saved, recipient);
    }

    /** Demandes recues par l'utilisateur connecte. Pas de {@code readOnly} : le profil peut etre cree au premier usage. */
    public List<ReceivedView> received() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        return requestRepository
            .findReceived(me.getId())
            .stream()
            .map(r ->
                new ReceivedView(
                    r.getId(),
                    r.getSender().getDisplayName(),
                    r.getMessage(),
                    r.getContact(),
                    r.getStatus(),
                    r.getCreatedAt(),
                    r.getAnsweredAt()
                )
            )
            .toList();
    }

    public List<SentView> sent() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        return requestRepository
            .findSent(me.getId())
            .stream()
            .map(r -> toSent(r, r.getRecipient()))
            .toList();
    }

    /** Accepte ou decline une demande recue et encore en attente ; faux si elle n'existe pas ou n'est pas a lui. */
    public boolean answer(Long requestId, boolean accept) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        ContactRequest request = requestRepository
            .findById(requestId)
            .filter(r -> r.getRecipient().getId().equals(me.getId()))
            .orElse(null);
        if (request == null) {
            return false;
        }
        if (request.getStatus() != ContactStatus.PENDING) {
            throw new BadRequestAlertException("Demande deja traitee", ENTITY_NAME, "alreadyanswered");
        }
        request.answer(accept ? ContactStatus.ACCEPTED : ContactStatus.DECLINED, Instant.now());
        if (accept) {
            notifications.notifyActor(request.getSender(), NotificationService.CONTACT_ACCEPTED, me.getDisplayName());
        }
        return true;
    }

    private static SentView toSent(ContactRequest r, UserProfile recipient) {
        return new SentView(
            r.getId(),
            recipient.getId(),
            recipient.getDisplayName(),
            r.getMessage(),
            r.getStatus(),
            r.getCreatedAt(),
            r.getAnsweredAt()
        );
    }
}
