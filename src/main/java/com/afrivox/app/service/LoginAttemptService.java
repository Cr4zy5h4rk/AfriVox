package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Limitation des tentatives de connexion, en memoire (un seul serveur, pas de cache distribue au MVP).
 * <p>
 * Deux compteurs independants :
 * <ul>
 *   <li>par couple (adresse IP, identifiant) : freine la devinette d'un mot de passe, sans permettre a un tiers
 *       de verrouiller le compte de quelqu'un depuis une autre adresse ;</li>
 *   <li>par adresse IP : freine le balayage de nombreux identifiants.</li>
 * </ul>
 * Au-dela du seuil, les tentatives sont refusees pendant {@code loginLockSeconds}. Un redemarrage remet les compteurs a zero.
 */
@Service
public class LoginAttemptService {

    /** Echecs comptes dans la fenetre commencant a {@code windowStart} ; {@code blockedUntil} non nul une fois bloque. */
    private record Entry(int failures, Instant windowStart, Instant blockedUntil) {}

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    private final ApplicationProperties.Security conf;

    private final Clock clock;

    @Autowired
    public LoginAttemptService(ApplicationProperties applicationProperties) {
        this(applicationProperties, Clock.systemUTC());
    }

    // Horloge injectable pour les tests
    LoginAttemptService(ApplicationProperties applicationProperties, Clock clock) {
        this.conf = applicationProperties.getSecurity();
        this.clock = clock;
    }

    public static String loginKey(String ip, String login) {
        return "login:" + ip + ":" + login.toLowerCase(java.util.Locale.ROOT);
    }

    public static String ipKey(String ip) {
        return "ip:" + ip;
    }

    /** Secondes a attendre avant de pouvoir reessayer, ou 0 si la tentative est autorisee. */
    public long retryAfterSeconds(String loginKey, String ipKey) {
        return Math.max(remaining(loginKey), remaining(ipKey));
    }

    public void recordFailure(String loginKey, String ipKey) {
        increment(loginKey, conf.getLoginMaxAttempts());
        increment(ipKey, conf.getIpMaxAttempts());
    }

    /** Une connexion reussie efface le compteur du couple (IP, identifiant), pas celui de l'IP. */
    public void recordSuccess(String loginKey) {
        entries.remove(loginKey);
    }

    @Scheduled(fixedRate = 600_000)
    public void purge() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(e -> isOver(e.getValue(), now));
    }

    private long remaining(String key) {
        Entry entry = entries.get(key);
        Instant now = clock.instant();
        if (entry == null || entry.blockedUntil() == null || !entry.blockedUntil().isAfter(now)) {
            return 0;
        }
        return Duration.between(now, entry.blockedUntil()).toSeconds() + 1;
    }

    private void increment(String key, int max) {
        Instant now = clock.instant();
        entries.compute(key, (k, current) -> {
            Entry base = current == null || isOver(current, now) ? new Entry(0, now, null) : current;
            int failures = base.failures() + 1;
            Instant blockedUntil = failures >= max ? now.plusSeconds(conf.getLoginLockSeconds()) : base.blockedUntil();
            return new Entry(failures, base.windowStart(), blockedUntil);
        });
    }

    /** Une entree est perimee quand sa fenetre est finie et qu'elle n'est plus bloquee. */
    private boolean isOver(Entry entry, Instant now) {
        boolean windowEnded = entry.windowStart().plusSeconds(conf.getLoginLockSeconds()).isBefore(now);
        boolean stillBlocked = entry.blockedUntil() != null && entry.blockedUntil().isAfter(now);
        return windowEnded && !stillBlocked;
    }
}
