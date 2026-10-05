package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Authority;
import com.afrivox.app.domain.RefreshToken;
import com.afrivox.app.domain.User;
import com.afrivox.app.repository.RefreshTokenRepository;
import com.afrivox.app.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(tokens, users, new ApplicationProperties());
        user = new User();
        user.setId(42L);
        user.setLogin("alice");
        user.setActivated(true);
        Authority role = new Authority();
        role.setName("ROLE_USER");
        user.setAuthorities(Set.of(role));
        when(users.getReferenceById(42L)).thenReturn(user);
    }

    private RefreshToken storedToken(String raw, String family, boolean revoked, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.setTokenHash(RefreshTokenService.hash(raw));
        t.setUser(user);
        t.setFamilyId(family);
        t.setRevoked(revoked);
        t.setExpiresAt(expiresAt);
        when(tokens.findOneByTokenHash(RefreshTokenService.hash(raw))).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void issueStoresOnlyTheHashNeverTheRawToken() {
        var issued = service.issue(42L, false);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.value())).isNotEqualTo(issued.value());
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(issued.value()).hasSizeGreaterThanOrEqualTo(43); // 32 octets en Base64 URL sans bourrage
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(600_000)); // ~7 jours
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void rememberMeGivesLongerLifetime() {
        var normal = service.issue(42L, false);
        var remembered = service.issue(42L, true);

        assertThat(remembered.maxAgeSeconds()).isGreaterThan(normal.maxAgeSeconds());
    }

    @Test
    void twoIssuedTokensAreDifferent() {
        assertThat(service.issue(42L, false).value()).isNotEqualTo(service.issue(42L, false).value());
    }

    @Test
    void rotateRevokesOldTokenAndKeepsFamily() {
        RefreshToken old = storedToken("raw-old", "family-1", false, Instant.now().plusSeconds(3600));
        old.setRememberMe(true);

        var rotated = service.rotate("raw-old");

        assertThat(old.isRevoked()).isTrue();
        assertThat(rotated.login()).isEqualTo("alice");
        assertThat(rotated.userId()).isEqualTo(42L);
        assertThat(rotated.authorities()).containsExactly("ROLE_USER");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo("family-1");
        assertThat(captor.getValue().isRememberMe()).isTrue();
        assertThat(rotated.token().value()).isNotEqualTo("raw-old");
    }

    @Test
    void reusedTokenRevokesWholeFamily() {
        storedToken("raw-used", "family-9", true, Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> service.rotate("raw-used")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokens).revokeFamily("family-9");
        verify(tokens, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        storedToken("raw-expired", "family-2", false, Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> service.rotate("raw-expired")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokens, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(tokens.findOneByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("whatever")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void deactivatedUserCannotRefresh() {
        storedToken("raw-ok", "family-3", false, Instant.now().plusSeconds(3600));
        user.setActivated(false);

        assertThatThrownBy(() -> service.rotate("raw-ok")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeClosesTheWholeFamily() {
        storedToken("raw-logout", "family-4", false, Instant.now().plusSeconds(3600));

        service.revoke("raw-logout");

        verify(tokens).revokeFamily("family-4");
    }

    @Test
    void revokeOfUnknownTokenIsSilent() {
        when(tokens.findOneByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revoke("nope");

        verify(tokens, never()).revokeFamily(anyString());
    }

    @Test
    void revokeAllForUserDelegatesToRepository() {
        service.revokeAllForUser(42L);

        verify(tokens).revokeAllByUserId(42L);
    }
}
