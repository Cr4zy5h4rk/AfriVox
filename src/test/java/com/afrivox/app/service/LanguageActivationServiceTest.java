package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.enumeration.LanguageStatus;
import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.repository.LanguageRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Une langue en liste d'attente devient ACTIVE au seuil de validateurs qualifies ; jamais l'inverse. */
class LanguageActivationServiceTest {

    private static final long LANGUAGE_ID = 3L;

    private LanguageRepository languages;
    private LanguageQualificationRepository qualifications;
    private LanguageActivationService service;

    @BeforeEach
    void setUp() {
        languages = mock(LanguageRepository.class);
        qualifications = mock(LanguageQualificationRepository.class);
        service = new LanguageActivationService(languages, qualifications);
    }

    private Language language(LanguageStatus status, Integer minValidators) {
        Language language = new Language().id(LANGUAGE_ID).status(status).minValidators(minValidators);
        when(languages.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        return language;
    }

    private void qualified(long count) {
        when(qualifications.countByLanguageIdAndStatus(LANGUAGE_ID, QualificationStatus.QUALIFIED)).thenReturn(count);
    }

    @Test
    void waitlistedLanguageIsActivatedWhenTheThresholdIsReached() {
        Language language = language(LanguageStatus.WAITLIST, 3);
        qualified(3);

        service.promoteIfReady(LANGUAGE_ID);

        assertThat(language.getStatus()).isEqualTo(LanguageStatus.ACTIVE);
        verify(languages).save(language);
    }

    @Test
    void languageBelowTheThresholdStaysWaitlisted() {
        Language language = language(LanguageStatus.WAITLIST, 3);
        qualified(2);

        service.promoteIfReady(LANGUAGE_ID);

        assertThat(language.getStatus()).isEqualTo(LanguageStatus.WAITLIST);
        verify(languages, never()).save(any());
    }

    @Test
    void activeLanguageIsLeftAlone() {
        Language language = language(LanguageStatus.ACTIVE, 3);
        qualified(0);

        service.promoteIfReady(LANGUAGE_ID);

        // Jamais de retour en liste d'attente automatique, meme si les validateurs sont partis.
        assertThat(language.getStatus()).isEqualTo(LanguageStatus.ACTIVE);
        verify(languages, never()).save(any());
    }

    @Test
    void missingOrNonPositiveThresholdNeverActivates() {
        Language noThreshold = language(LanguageStatus.WAITLIST, null);
        qualified(50);
        service.promoteIfReady(LANGUAGE_ID);
        assertThat(noThreshold.getStatus()).isEqualTo(LanguageStatus.WAITLIST);

        Language zeroThreshold = language(LanguageStatus.WAITLIST, 0);
        service.promoteIfReady(LANGUAGE_ID);
        assertThat(zeroThreshold.getStatus()).isEqualTo(LanguageStatus.WAITLIST);
    }

    @Test
    void nullLanguageIdDoesNothing() {
        service.promoteIfReady(null);

        verifyNoInteractions(languages, qualifications);
    }

    @Test
    void unknownLanguageDoesNothing() {
        when(languages.findById(99L)).thenReturn(Optional.empty());

        service.promoteIfReady(99L);

        verify(languages, never()).save(any());
    }
}
