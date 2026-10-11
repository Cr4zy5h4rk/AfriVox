package com.afrivox.app.service;

import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.AgeRange;
import com.afrivox.app.domain.enumeration.Gender;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Informations declarees par un contributeur apres son inscription : tranche d'age, genre, pays, region, autres
 * langues. Tout est facultatif et declaratif ; la reponse (meme vide) marque le questionnaire comme traite pour qu'il ne
 * soit plus propose. Ces informations ne sortent qu'en agregat (fiche d'un dataset), jamais par locuteur.
 */
@Service
// Pas readOnly : getOrCreateCurrent() peut creer le profil (INSERT).
@Transactional
public class ContributorProfileService {

    private static final String ENTITY_NAME = "contributorProfile";

    static final int MAX_OTHER_LANGUAGES = 8;

    /** Ce que le contributeur a declare. {@code answered} : le questionnaire a ete rempli ou passe. */
    public record ProfileView(
        boolean answered,
        String nativeLanguage,
        AgeRange ageRange,
        Gender gender,
        String country,
        String region,
        List<String> otherLanguages
    ) {}

    /** Reponses envoyees ; toute valeur absente est simplement non declaree. */
    public record ProfileInput(
        AgeRange ageRange,
        Gender gender,
        @Size(max = 60) String country,
        @Size(max = 60) String region,
        @Size(max = 8) List<@Size(max = 20) String> otherLanguages
    ) {}

    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final LanguageRepository languageRepository;

    public ContributorProfileService(
        UserProfileService userProfileService,
        UserProfileRepository userProfileRepository,
        LanguageRepository languageRepository
    ) {
        this.userProfileService = userProfileService;
        this.userProfileRepository = userProfileRepository;
        this.languageRepository = languageRepository;
    }

    public ProfileView mine() {
        return toView(userProfileService.getOrCreateCurrent());
    }

    /** Remplace les reponses : une valeur absente efface la precedente (le contributeur peut tout retirer). */
    public ProfileView save(ProfileInput input) {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        profile.setAgeRange(input.ageRange());
        profile.setGender(input.gender());
        profile.setCountry(clean(input.country()));
        profile.setRegion(clean(input.region()));
        profile.setOtherLanguages(joinLanguages(input.otherLanguages()));
        profile.setDemographicsAnsweredAt(Instant.now());
        return toView(userProfileRepository.save(profile));
    }

    private String joinLanguages(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String trimmed = code.trim();
            boolean known = UserService.OTHER_LANGUAGE.equals(trimmed) || languageRepository.findOneByCode(trimmed).isPresent();
            if (!known || trimmed.contains(",")) {
                throw new BadRequestAlertException("Langue inconnue", ENTITY_NAME, "invalidlanguage");
            }
            distinct.add(trimmed);
        }
        if (distinct.size() > MAX_OTHER_LANGUAGES) {
            throw new BadRequestAlertException("Trop de langues", ENTITY_NAME, "invalidlanguage");
        }
        return distinct.isEmpty() ? null : String.join(",", distinct);
    }

    private static String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static ProfileView toView(UserProfile p) {
        List<String> others =
            p.getOtherLanguages() == null || p.getOtherLanguages().isBlank() ? List.of() : Arrays.asList(p.getOtherLanguages().split(","));
        return new ProfileView(
            p.getDemographicsAnsweredAt() != null,
            p.getNativeLanguage(),
            p.getAgeRange(),
            p.getGender(),
            p.getCountry(),
            p.getRegion(),
            others
        );
    }
}
