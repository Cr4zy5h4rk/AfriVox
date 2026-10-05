package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Authority;
import com.afrivox.app.domain.RefreshToken;
import com.afrivox.app.domain.User;
import com.afrivox.app.repository.RefreshTokenRepository;
import com.afrivox.app.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des jetons de rafraichissement.
 * <p>
 * Decisions de securite :
 * <ul>
 *   <li>le jeton est aleatoire (256 bits, {@link SecureRandom}) et opaque ; seul son SHA-256 est stocke ;</li>
 *   <li>rotation : chaque rafraichissement revoque le jeton presente et en emet un nouveau ;</li>
 *   <li>detection de rejeu : presenter un jeton deja revoque revoque toute sa famille (connexion volee probable).</li>
 * </ul>
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    private final ApplicationProperties applicationProperties;

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        ApplicationProperties applicationProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.applicationProperties = applicationProperties;
    }

    /** Jeton en clair, a envoyer une seule fois au client, avec sa duree de vie. */
    public record IssuedToken(String value, long maxAgeSeconds) {}

    /** Resultat d'un rafraichissement : de quoi construire le jeton d'acces et le nouveau cookie. */
    public record RotatedToken(Long userId, String login, Set<String> authorities, IssuedToken token) {}

    /** Ouvre une nouvelle famille de jetons (connexion). */
    public IssuedToken issue(Long userId, boolean rememberMe) {
        return issueInFamily(userRepository.getReferenceById(userId), rememberMe, UUID.randomUUID().toString());
    }

    /**
     * Echange un jeton valide contre un nouveau. Les revocations faites avant une exception
     * (rejeu) doivent etre conservees, d'ou l'absence de rollback pour cette exception.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository
            .findOneByTokenHash(hash(rawToken))
            .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (current.isRevoked()) {
            // Jeton deja utilise ou revoque : on suppose un vol et on coupe toute la famille.
            int count = refreshTokenRepository.revokeFamily(current.getFamilyId());
            LOG.warn("Refresh token reuse detected for user id {}, {} token(s) revoked", current.getUser().getId(), count);
            throw new InvalidRefreshTokenException("Refresh token already used");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }
        User user = current.getUser();
        if (!user.isActivated()) {
            throw new InvalidRefreshTokenException("User not activated");
        }

        current.setRevoked(true);
        IssuedToken next = issueInFamily(user, current.isRememberMe(), current.getFamilyId());
        Set<String> authorities = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
        return new RotatedToken(user.getId(), user.getLogin(), authorities, next);
    }

    /** Deconnexion : revoque la famille du jeton presente (silencieux si inconnu). */
    public void revoke(String rawToken) {
        refreshTokenRepository.findOneByTokenHash(hash(rawToken)).ifPresent(t -> refreshTokenRepository.revokeFamily(t.getFamilyId()));
    }

    /** Appele au changement ou a la reinitialisation du mot de passe : toutes les sessions sont fermees. */
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /** Les jetons revoques mais non expires sont gardes pour detecter les rejeux ; on purge les expires. */
    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeExpired() {
        int count = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        LOG.debug("Purged {} expired refresh token(s)", count);
    }

    private IssuedToken issueInFamily(User user, boolean rememberMe, String familyId) {
        ApplicationProperties.Security conf = applicationProperties.getSecurity();
        long maxAge = rememberMe ? conf.getRefreshTokenRememberMeValiditySeconds() : conf.getRefreshTokenValiditySeconds();

        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(raw));
        entity.setUser(user);
        entity.setFamilyId(familyId);
        entity.setRememberMe(rememberMe);
        entity.setExpiresAt(Instant.now().plusSeconds(maxAge));
        refreshTokenRepository.save(entity);
        return new IssuedToken(raw, maxAge);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti par toute JVM
            throw new IllegalStateException(e);
        }
    }
}
