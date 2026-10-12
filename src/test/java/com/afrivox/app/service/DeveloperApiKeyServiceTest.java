package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.ApiKey;
import com.afrivox.app.domain.Authority;
import com.afrivox.app.domain.User;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ApiKeyScope;
import com.afrivox.app.repository.ApiKeyRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Clés d'API : génération, stockage du hash seul, authentification, révocation et limite de débit. */
class DeveloperApiKeyServiceTest {

    private static final long PROFILE_ID = 7L;

    private ApiKeyRepository repository;
    private UserProfileService profileService;
    private DeveloperApiKeyService service;
    private UserProfile me;
    private User user;
    private final List<ApiKey> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(ApiKeyRepository.class);
        profileService = mock(UserProfileService.class);
        service = new DeveloperApiKeyService(repository, profileService);

        user = new User();
        user.setLogin("dev_amadou");
        user.setActivated(true);
        user.setAuthorities(Set.of(new Authority().name("ROLE_DEVELOPER"), new Authority().name("ROLE_ADMIN")));
        me = new UserProfile().id(PROFILE_ID);
        me.setUser(user);
        when(profileService.getOrCreateCurrent()).thenReturn(me);

        when(repository.save(any(ApiKey.class))).thenAnswer(call -> {
            ApiKey key = call.getArgument(0);
            if (key.getId() == null) {
                key.setId(100L + saved.size());
            }
            if (!saved.contains(key)) {
                saved.add(key);
            }
            return key;
        });
    }

    private DeveloperApiKeyService.CreatedKey create(Set<ApiKeyScope> scopes, Integer days) {
        return service.create("Mon script", scopes, days);
    }

    /** Crée une clé et la rend retrouvable par l'authentification, comme la base le ferait. */
    private DeveloperApiKeyService.CreatedKey createStored(Set<ApiKeyScope> scopes, Integer days) {
        DeveloperApiKeyService.CreatedKey created = create(scopes, days);
        ApiKey stored = saved.get(saved.size() - 1);
        when(repository.findOneForAuthentication(stored.getPrefix())).thenReturn(Optional.of(stored));
        return created;
    }

    private ApiKey lastSaved() {
        return saved.get(saved.size() - 1);
    }

    private static void assertErrorKey(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    // ---- Création ------------------------------------------------------------------------------------------------

    @Test
    void createdKeyHasTheDocumentedFormatAndIsShownOnlyInTheResponse() {
        DeveloperApiKeyService.CreatedKey created = create(EnumSet.of(ApiKeyScope.DOWNLOAD), null);

        assertThat(created.secret()).matches("^afv_[A-Za-z0-9]{8}_[A-Za-z0-9_-]{43}$");
        assertThat(created.secret()).startsWith(created.key().prefix() + "_");
        assertThat(created.key().prefix()).hasSize(12);
        assertThat(created.key().active()).isTrue();
        assertThat(created.key().name()).isEqualTo("Mon script");
    }

    @Test
    void onlyTheHashIsStoredNeverTheKey() {
        DeveloperApiKeyService.CreatedKey created = create(EnumSet.of(ApiKeyScope.DOWNLOAD), null);

        ApiKey stored = lastSaved();
        assertThat(stored.getKeyHash()).isEqualTo(DeveloperApiKeyService.hash(created.secret())).hasSize(64);
        assertThat(stored.getKeyHash()).doesNotContain(created.secret());
        assertThat(stored.getOwner()).isSameAs(me);
    }

    @Test
    void twoKeysAreDifferent() {
        String first = create(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        String second = create(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void scopesAndExpiryAreStored() {
        Instant before = Instant.now();

        DeveloperApiKeyService.CreatedKey created = create(EnumSet.of(ApiKeyScope.EVALUATE, ApiKeyScope.DOWNLOAD), 30);

        assertThat(lastSaved().getScopes()).isEqualTo("DOWNLOAD,EVALUATE");
        assertThat(created.key().expiresAt()).isBetween(before.plus(Duration.ofDays(30)), Instant.now().plus(Duration.ofDays(30)));
    }

    @Test
    void nameIsTrimmedAndValidated() {
        service.create("  Mon CI  ", EnumSet.of(ApiKeyScope.DOWNLOAD), null);
        assertThat(lastSaved().getName()).isEqualTo("Mon CI");

        assertErrorKey(() -> service.create("   ", EnumSet.of(ApiKeyScope.DOWNLOAD), null), "invalidname");
        assertErrorKey(() -> service.create(null, EnumSet.of(ApiKeyScope.DOWNLOAD), null), "invalidname");
        assertErrorKey(() -> service.create("x".repeat(61), EnumSet.of(ApiKeyScope.DOWNLOAD), null), "invalidname");
    }

    @Test
    void atLeastOneScopeIsRequired() {
        assertErrorKey(() -> service.create("k", null, null), "invalidscopes");
        assertErrorKey(() -> service.create("k", EnumSet.noneOf(ApiKeyScope.class), null), "invalidscopes");
    }

    @Test
    void expiryMustBeBetweenOneAndThreeHundredSixtyFiveDays() {
        assertErrorKey(() -> service.create("k", EnumSet.of(ApiKeyScope.DOWNLOAD), 0), "invalidexpiry");
        assertErrorKey(() -> service.create("k", EnumSet.of(ApiKeyScope.DOWNLOAD), 366), "invalidexpiry");
        assertThat(service.create("k", EnumSet.of(ApiKeyScope.DOWNLOAD), 365).key().expiresAt()).isNotNull();
    }

    @Test
    void activeKeysAreCapped() {
        when(repository.countByOwnerIdAndRevokedAtIsNull(PROFILE_ID)).thenReturn((long) DeveloperApiKeyService.MAX_ACTIVE_KEYS);

        assertErrorKey(() -> create(EnumSet.of(ApiKeyScope.DOWNLOAD), null), "toomanykeys");
        verify(repository, never()).save(any());
    }

    @Test
    void prefixCollisionIsRetried() {
        when(repository.existsByPrefix(anyString())).thenReturn(true, false);

        create(EnumSet.of(ApiKeyScope.DOWNLOAD), null);

        verify(repository, times(2)).existsByPrefix(anyString());
    }

    @Test
    void givesUpWhenNoUniquePrefixCanBeFound() {
        when(repository.existsByPrefix(anyString())).thenReturn(true);

        assertThatThrownBy(() -> create(EnumSet.of(ApiKeyScope.DOWNLOAD), null)).isInstanceOf(IllegalStateException.class);
    }

    // ---- Liste et révocation -------------------------------------------------------------------------------------

    @Test
    void listShowsActiveRevokedAndExpiredKeysWithoutAnySecret() {
        ApiKey active = new ApiKey();
        active.setId(1L);
        active.setName("a");
        active.setPrefix("afv_AAAAAAAA");
        active.setScopes("DOWNLOAD");
        active.setCreatedAt(Instant.now());
        ApiKey revoked = new ApiKey();
        revoked.setId(2L);
        revoked.setName("b");
        revoked.setPrefix("afv_BBBBBBBB");
        revoked.setScopes("EVALUATE");
        revoked.setCreatedAt(Instant.now());
        revoked.setRevokedAt(Instant.now());
        ApiKey expired = new ApiKey();
        expired.setId(3L);
        expired.setName("c");
        expired.setPrefix("afv_CCCCCCCC");
        expired.setScopes("PUBLISH");
        expired.setCreatedAt(Instant.now());
        expired.setExpiresAt(Instant.now().minusSeconds(5));
        when(repository.findAllByOwnerIdOrderByCreatedAtDesc(PROFILE_ID)).thenReturn(List.of(active, revoked, expired));

        List<DeveloperApiKeyService.KeyView> keys = service.listMine();

        assertThat(keys).extracting(DeveloperApiKeyService.KeyView::active).containsExactly(true, false, false);
        assertThat(keys.get(1).scopes()).containsExactly(ApiKeyScope.EVALUATE);
    }

    @Test
    void revokeStampsTheKey() {
        create(EnumSet.of(ApiKeyScope.DOWNLOAD), null);
        ApiKey key = lastSaved();
        when(repository.findOneByIdAndOwnerId(key.getId(), PROFILE_ID)).thenReturn(Optional.of(key));

        service.revoke(key.getId());

        assertThat(key.getRevokedAt()).isNotNull();
    }

    @Test
    void someoneElsesKeyIsNotFound() {
        when(repository.findOneByIdAndOwnerId(55L, PROFILE_ID)).thenReturn(Optional.empty());

        assertErrorKey(() -> service.revoke(55L), "keynotfound");
    }

    @Test
    void revokingTwiceKeepsTheFirstDate() {
        ApiKey key = new ApiKey();
        key.setId(5L);
        Instant first = Instant.parse("2026-10-01T00:00:00Z");
        key.setRevokedAt(first);
        when(repository.findOneByIdAndOwnerId(5L, PROFILE_ID)).thenReturn(Optional.of(key));

        service.revoke(5L);

        assertThat(key.getRevokedAt()).isEqualTo(first);
        verify(repository, never()).save(key);
    }

    // ---- Authentification ----------------------------------------------------------------------------------------

    @Test
    void validKeyAuthenticatesAsItsOwnerWithItsScopes() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD, ApiKeyScope.EVALUATE), null).secret();

        DeveloperApiKeyService.Authenticated auth = service.authenticate(secret).orElseThrow();

        assertThat(auth.login()).isEqualTo("dev_amadou");
        assertThat(auth.scopes()).containsExactlyInAnyOrder(ApiKeyScope.DOWNLOAD, ApiKeyScope.EVALUATE);
    }

    @Test
    void aKeyNeverCarriesTheAdminRole() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();

        DeveloperApiKeyService.Authenticated auth = service.authenticate(secret).orElseThrow();

        assertThat(auth.authorities()).containsExactly("ROLE_DEVELOPER");
    }

    @Test
    void wrongSecretWithTheRightPrefixIsRefused() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        String forged = secret.substring(0, 13) + "A".repeat(43);

        assertThat(forged).hasSameSizeAs(secret).isNotEqualTo(secret);
        assertThat(service.authenticate(forged)).isEmpty();
    }

    @Test
    void malformedAndUnknownKeysAreRefused() {
        assertThat(service.authenticate(null)).isEmpty();
        assertThat(service.authenticate("")).isEmpty();
        assertThat(service.authenticate("pas-une-cle")).isEmpty();
        assertThat(service.authenticate("afv_court")).isEmpty();
        when(repository.findOneForAuthentication("afv_ZZZZZZZZ")).thenReturn(Optional.empty());
        assertThat(service.authenticate("afv_ZZZZZZZZ_" + "x".repeat(43))).isEmpty();
    }

    @Test
    void revokedKeyIsRefused() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        lastSaved().setRevokedAt(Instant.now());

        assertThat(service.authenticate(secret)).isEmpty();
    }

    @Test
    void expiredKeyIsRefused() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), 1).secret();
        lastSaved().setExpiresAt(Instant.now().minusSeconds(1));

        assertThat(service.authenticate(secret)).isEmpty();
    }

    @Test
    void keyOfADeactivatedAccountIsRefused() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        user.setActivated(false);

        assertThat(service.authenticate(secret)).isEmpty();
    }

    @Test
    void unknownStoredScopeGivesNoExtraPower() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        lastSaved().setScopes("DOWNLOAD,ADMIN,N_IMPORTE_QUOI");

        assertThat(service.authenticate(secret).orElseThrow().scopes()).containsExactly(ApiKeyScope.DOWNLOAD);
    }

    @Test
    void lastUseIsRecordedAtMostOncePerMinute() {
        String secret = createStored(EnumSet.of(ApiKeyScope.DOWNLOAD), null).secret();
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);

        service.authenticate(secret);
        service.authenticate(secret);
        service.authenticate(secret);

        // 1 ecriture a la creation + 1 pour la premiere utilisation ; les deux appels suivants n'ecrivent rien
        verify(repository, times(2)).save(captor.capture());
        assertThat(lastSaved().getLastUsedAt()).isNotNull();
    }

    // ---- Débit ---------------------------------------------------------------------------------------------------

    @Test
    void rateLimitIsPerKeyAndPerMinute() {
        for (int i = 0; i < DeveloperApiKeyService.REQUESTS_PER_MINUTE; i++) {
            assertThat(service.allowRequest(1L))
                .as("requete %d", i + 1)
                .isTrue();
        }

        assertThat(service.allowRequest(1L)).isFalse();
        assertThat(service.allowRequest(2L)).isTrue();
    }
}
