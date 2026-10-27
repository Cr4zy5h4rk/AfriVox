package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.ContactRequest;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ContactStatus;
import com.afrivox.app.repository.ContactRequestRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prise de contact : seul un profil public ouvert est joignable, garde-fous de rythme et de doublon, reponse du destinataire. */
class ContactServiceTest {

    private static final String VALID_MESSAGE = "Bonjour, nous cherchons un specialiste de la reconnaissance vocale.";

    private ContactRequestRepository requests;
    private UserProfileRepository profiles;
    private UserProfileService profileService;
    private NotificationService notifications;
    private ContactService service;
    private UserProfile sender;
    private UserProfile recipient;

    @BeforeEach
    void setUp() {
        requests = mock(ContactRequestRepository.class);
        profiles = mock(UserProfileRepository.class);
        profileService = mock(UserProfileService.class);
        notifications = mock(NotificationService.class);
        service = new ContactService(requests, profiles, profileService, notifications);

        sender = new UserProfile().id(1L);
        sender.setDisplayName("acme");
        recipient = new UserProfile().id(2L);
        recipient.setDisplayName("awa");
        recipient.setPublicProfile(true);
        recipient.setOpenToWork(true);
        when(profileService.getOrCreateCurrent()).thenReturn(sender);
        when(profiles.findById(2L)).thenReturn(Optional.of(recipient));
        when(requests.save(any(ContactRequest.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static void assertErrorKey(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    @Test
    void sendsARequestAndNotifiesTheRecipient() {
        ContactService.SentView view = service.send(2L, VALID_MESSAGE, "rh@acme.test");

        assertThat(view.status()).isEqualTo(ContactStatus.PENDING);
        assertThat(view.recipientName()).isEqualTo("awa");
        verify(notifications).notifyActor(recipient, NotificationService.CONTACT_REQUEST, "acme");
    }

    @Test
    void refusesAPrivateProfileWithTheSameAnswerAsAnUnknownOne() {
        recipient.setPublicProfile(false);
        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "rh@acme.test"), "recipientunavailable");

        when(profiles.findById(99L)).thenReturn(Optional.empty());
        assertErrorKey(() -> service.send(99L, VALID_MESSAGE, "rh@acme.test"), "recipientunavailable");
    }

    @Test
    void refusesAProfileThatIsNotOpenToWork() {
        recipient.setOpenToWork(false);

        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "rh@acme.test"), "recipientunavailable");
        verify(requests, never()).save(any());
    }

    @Test
    void refusesContactingYourself() {
        when(profiles.findById(1L)).thenReturn(Optional.of(sender));
        sender.setPublicProfile(true);
        sender.setOpenToWork(true);

        assertErrorKey(() -> service.send(1L, VALID_MESSAGE, "rh@acme.test"), "selfcontact");
    }

    @Test
    void validatesTheMessageAndTheContactDetail() {
        assertErrorKey(() -> service.send(2L, "trop court", "rh@acme.test"), "invalidmessage");
        assertErrorKey(() -> service.send(2L, "x".repeat(1001), "rh@acme.test"), "invalidmessage");
        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "  "), "invalidcontact");
        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "x".repeat(121)), "invalidcontact");
    }

    @Test
    void allowsOnlyOnePendingRequestPerPair() {
        when(requests.existsBySenderIdAndRecipientIdAndStatus(1L, 2L, ContactStatus.PENDING)).thenReturn(true);

        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "rh@acme.test"), "alreadypending");
    }

    @Test
    void limitsRequestsPerDay() {
        when(requests.countBySenderIdAndCreatedAtAfter(eq(1L), any(Instant.class))).thenReturn((long) ContactService.MAX_PER_DAY);

        assertErrorKey(() -> service.send(2L, VALID_MESSAGE, "rh@acme.test"), "toomanyrequests");
    }

    @Test
    void acceptingNotifiesTheSenderButDecliningDoesNot() {
        ContactRequest accepted = new ContactRequest(sender, recipient, VALID_MESSAGE, "rh@acme.test", Instant.now());
        ContactRequest declined = new ContactRequest(sender, recipient, VALID_MESSAGE, "rh@acme.test", Instant.now());
        when(profileService.getOrCreateCurrent()).thenReturn(recipient);
        when(requests.findById(10L)).thenReturn(Optional.of(accepted));
        when(requests.findById(11L)).thenReturn(Optional.of(declined));

        assertThat(service.answer(10L, true)).isTrue();
        assertThat(accepted.getStatus()).isEqualTo(ContactStatus.ACCEPTED);
        verify(notifications).notifyActor(sender, NotificationService.CONTACT_ACCEPTED, "awa");

        assertThat(service.answer(11L, false)).isTrue();
        assertThat(declined.getStatus()).isEqualTo(ContactStatus.DECLINED);
        verify(notifications, never()).notifyActor(any(), eq(NotificationService.CONTACT_ACCEPTED), eq("awa"));
    }

    @Test
    void onlyTheRecipientCanAnswerAndOnlyOnce() {
        ContactRequest request = new ContactRequest(sender, recipient, VALID_MESSAGE, "rh@acme.test", Instant.now());
        when(requests.findById(10L)).thenReturn(Optional.of(request));

        // L'expediteur (connecte ici) n'est pas le destinataire : on ne revele rien, la demande est « introuvable ».
        assertThat(service.answer(10L, true)).isFalse();

        when(profileService.getOrCreateCurrent()).thenReturn(recipient);
        assertThat(service.answer(10L, true)).isTrue();
        assertErrorKey(() -> service.answer(10L, false), "alreadyanswered");
    }
}
