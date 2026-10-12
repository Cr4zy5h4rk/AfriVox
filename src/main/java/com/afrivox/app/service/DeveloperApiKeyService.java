package com.afrivox.app.service;

import com.afrivox.app.domain.ApiKey;
import com.afrivox.app.domain.Authority;
import com.afrivox.app.domain.User;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ApiKeyScope;
import com.afrivox.app.repository.ApiKeyRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clés d'API des développeurs (et organisations) : automatiser le téléchargement d'un dataset acheté, l'envoi de
 * prédictions pour le classement et la publication d'un modèle, sans passer par le navigateur.
 *
 * <ul>
 *   <li><b>Format</b> : {@code afv_<8 caractères>_<43 caractères>}. Les 12 premiers caractères (préfixe) sont publics
 *       et servent à retrouver la clé ; le reste est un secret de 256 bits tiré au hasard.</li>
 *   <li><b>Stockage</b> : seul le hash SHA-256 de la clé complète est conservé. Un secret de 256 bits aléatoires n'a pas
 *       besoin d'un hash lent ni d'un sel. La clé n'est montrée qu'une fois, à la création.</li>
 *   <li><b>Pouvoirs</b> : une clé agit comme son propriétaire, limitée à ses portées ({@link ApiKeyScope}) et jamais
 *       administrateur, même si le compte l'est.</li>
 *   <li><b>Révocation</b> : immédiate ; une clé révoquée ou expirée n'authentifie plus. La ligne est conservée.</li>
 * </ul>
 */
@Service
// Pas readOnly : getOrCreateCurrent() peut creer le profil (INSERT).
@Transactional
public class DeveloperApiKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(DeveloperApiKeyService.class);

    private static final String ENTITY_NAME = "apiKey";

    /** Nombre maximal de clés actives par compte. */
    public static final int MAX_ACTIVE_KEYS = 10;

    public static final int MAX_EXPIRY_DAYS = 365;

    /** Requêtes par minute et par clé (au-delà : 429). */
    public static final int REQUESTS_PER_MINUTE = 120;

    /** « Dernière utilisation » mise à jour au plus une fois par minute : pas d'écriture à chaque appel. */
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);

    private static final String PREFIX_START = "afv_";
    private static final int PREFIX_LENGTH = 12;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Pattern KEY_FORMAT = Pattern.compile("^afv_[A-Za-z0-9]{8}_[A-Za-z0-9_-]{43}$");

    public record KeyView(
        Long id,
        String name,
        String prefix,
        Set<ApiKeyScope> scopes,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt,
        boolean active
    ) {}

    /** La clé complète n'apparaît que dans cette réponse de création. */
    public record CreatedKey(KeyView key, String secret) {}

    /** Résultat d'une authentification réussie : ce que le filtre de sécurité installe pour la requête. */
    public record Authenticated(Long keyId, String login, Set<ApiKeyScope> scopes, Set<String> authorities) {}

    private final ApiKeyRepository repository;
    private final UserProfileService userProfileService;
    private final SecureRandom random = new SecureRandom();
    /** Fenêtre fixe d'une minute par clé : {minute, nombre de requêtes}. */
    private final Map<Long, long[]> windows = new ConcurrentHashMap<>();

    public DeveloperApiKeyService(ApiKeyRepository repository, UserProfileService userProfileService) {
        this.repository = repository;
        this.userProfileService = userProfileService;
    }

    // ---- Gestion par le propriétaire (session de navigateur) ----------------------------------------------------

    public CreatedKey create(String name, Set<ApiKeyScope> scopes, Integer expiresInDays) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 60) {
            throw new BadRequestAlertException("Nom obligatoire (60 caracteres au plus)", ENTITY_NAME, "invalidname");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new BadRequestAlertException("Choisir au moins une portee", ENTITY_NAME, "invalidscopes");
        }
        if (expiresInDays != null && (expiresInDays < 1 || expiresInDays > MAX_EXPIRY_DAYS)) {
            throw new BadRequestAlertException("Expiration : 1 a " + MAX_EXPIRY_DAYS + " jours", ENTITY_NAME, "invalidexpiry");
        }
        UserProfile me = userProfileService.getOrCreateCurrent();
        if (repository.countByOwnerIdAndRevokedAtIsNull(me.getId()) >= MAX_ACTIVE_KEYS) {
            throw new BadRequestAlertException("Trop de cles actives (" + MAX_ACTIVE_KEYS + " au plus)", ENTITY_NAME, "toomanykeys");
        }

        String prefix = newPrefix();
        String secret = prefix + "_" + newSecret();
        Instant now = Instant.now();

        ApiKey key = new ApiKey();
        key.setName(cleanName);
        key.setPrefix(prefix);
        key.setKeyHash(hash(secret));
        key.setScopes(encode(scopes));
        key.setCreatedAt(now);
        key.setExpiresAt(expiresInDays == null ? null : now.plus(Duration.ofDays(expiresInDays)));
        key.setOwner(me);
        key = repository.save(key);
        LOG.info("Cle d'API {} creee pour le profil {} (portees {})", key.getPrefix(), me.getId(), key.getScopes());
        return new CreatedKey(view(key, now), secret);
    }

    public List<KeyView> listMine() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Instant now = Instant.now();
        return repository
            .findAllByOwnerIdOrderByCreatedAtDesc(me.getId())
            .stream()
            .map(k -> view(k, now))
            .toList();
    }

    /** Révoque une de mes clés (sans effet si elle l'est déjà). Les clés des autres ne sont pas trouvées. */
    public void revoke(Long id) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        ApiKey key = repository
            .findOneByIdAndOwnerId(id, me.getId())
            .orElseThrow(() -> new BadRequestAlertException("Cle introuvable", ENTITY_NAME, "keynotfound"));
        if (key.getRevokedAt() == null) {
            key.setRevokedAt(Instant.now());
            repository.save(key);
            windows.remove(key.getId());
            LOG.info("Cle d'API {} revoquee", key.getPrefix());
        }
    }

    // ---- Authentification (appelée par le filtre, une fois par requête) ------------------------------------------

    /**
     * Vérifie une clé présentée par un client. Vide si le format est faux, la clé inconnue, révoquée ou expirée, ou le
     * compte désactivé : l'appelant répond toujours la même chose, sans dire laquelle de ces raisons s'applique.
     */
    public Optional<Authenticated> authenticate(String rawKey) {
        if (rawKey == null || !KEY_FORMAT.matcher(rawKey).matches()) {
            return Optional.empty();
        }
        Optional<ApiKey> found = repository.findOneForAuthentication(rawKey.substring(0, PREFIX_LENGTH));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ApiKey key = found.orElseThrow();
        // Comparaison en temps constant : le temps de réponse ne révèle rien du hash.
        boolean same = MessageDigest.isEqual(
            hash(rawKey).getBytes(StandardCharsets.UTF_8),
            key.getKeyHash().getBytes(StandardCharsets.UTF_8)
        );
        Instant now = Instant.now();
        if (!same || key.getRevokedAt() != null || (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(now))) {
            return Optional.empty();
        }
        User user = key.getOwner().getUser();
        if (user == null || !user.isActivated()) {
            return Optional.empty();
        }
        if (key.getLastUsedAt() == null || key.getLastUsedAt().isBefore(now.minus(TOUCH_INTERVAL))) {
            key.setLastUsedAt(now);
            repository.save(key);
        }
        // Jamais administrateur : une clé qui fuite ne doit pas ouvrir l'administration.
        Set<String> authorities = user
            .getAuthorities()
            .stream()
            .map(Authority::getName)
            .filter(name -> !AuthoritiesConstants.ADMIN.equals(name))
            .collect(Collectors.toSet());
        return Optional.of(new Authenticated(key.getId(), user.getLogin(), decode(key.getScopes()), authorities));
    }

    /** Vrai tant que la clé n'a pas dépassé {@link #REQUESTS_PER_MINUTE} requêtes dans la minute courante. */
    public boolean allowRequest(Long keyId) {
        long minute = System.currentTimeMillis() / 60_000;
        long[] window = windows.compute(keyId, (id, current) -> {
            if (current == null || current[0] != minute) {
                return new long[] { minute, 1 };
            }
            current[1]++;
            return current;
        });
        return window[1] <= REQUESTS_PER_MINUTE;
    }

    // ---- Interne -------------------------------------------------------------------------------------------------

    private String newPrefix() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(PREFIX_START);
            for (int i = 0; i < PREFIX_LENGTH - PREFIX_START.length(); i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (!repository.existsByPrefix(sb.toString())) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("Impossible de generer un prefixe de cle unique");
    }

    private String newSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(Set<ApiKeyScope> scopes) {
        return EnumSet.copyOf(scopes).stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static Set<ApiKeyScope> decode(String scopes) {
        Set<ApiKeyScope> result = EnumSet.noneOf(ApiKeyScope.class);
        if (scopes != null) {
            for (String part : scopes.split(",")) {
                try {
                    result.add(ApiKeyScope.valueOf(part.trim()));
                } catch (IllegalArgumentException ignored) {
                    // portee inconnue (ancienne ou saisie a la main) : ignoree, donc aucun pouvoir en plus
                }
            }
        }
        return result;
    }

    private static KeyView view(ApiKey key, Instant now) {
        boolean active = key.getRevokedAt() == null && (key.getExpiresAt() == null || key.getExpiresAt().isAfter(now));
        return new KeyView(
            key.getId(),
            key.getName(),
            key.getPrefix(),
            decode(key.getScopes()),
            key.getCreatedAt(),
            key.getExpiresAt(),
            key.getLastUsedAt(),
            key.getRevokedAt(),
            active
        );
    }
}
