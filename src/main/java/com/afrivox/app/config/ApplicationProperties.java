package com.afrivox.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Afri Vox.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Security security = new Security();

    private final Consent consent = new Consent();

    private final Audio audio = new Audio();

    /** Active les comptes a l inscription (sans e-mail d activation). Faux par defaut. */
    private boolean autoActivateAccounts = false;

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public boolean isAutoActivateAccounts() {
        return autoActivateAccounts;
    }

    public void setAutoActivateAccounts(boolean autoActivateAccounts) {
        this.autoActivateAccounts = autoActivateAccounts;
    }

    public Security getSecurity() {
        return security;
    }

    public Consent getConsent() {
        return consent;
    }

    public Audio getAudio() {
        return audio;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    /** Parametres d'authentification : jeton de rafraichissement et limitation des tentatives de connexion. */
    public static class Security {

        /** Duree de vie du jeton de rafraichissement (7 jours). */
        private long refreshTokenValiditySeconds = 604_800;

        /** Duree de vie avec "se souvenir de moi" (30 jours). */
        private long refreshTokenRememberMeValiditySeconds = 2_592_000;

        /** Attribut Secure du cookie : vrai partout sauf en developpement local en HTTP. */
        private boolean refreshCookieSecure = true;

        /** Echecs toleres pour un couple (adresse IP, identifiant) avant blocage. */
        private int loginMaxAttempts = 5;

        /** Echecs toleres pour une adresse IP (tous identifiants confondus) avant blocage. */
        private int ipMaxAttempts = 30;

        /** Duree du blocage, et fenetre de comptage des echecs. */
        private long loginLockSeconds = 900;

        public long getRefreshTokenValiditySeconds() {
            return refreshTokenValiditySeconds;
        }

        public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) {
            this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        }

        public long getRefreshTokenRememberMeValiditySeconds() {
            return refreshTokenRememberMeValiditySeconds;
        }

        public void setRefreshTokenRememberMeValiditySeconds(long refreshTokenRememberMeValiditySeconds) {
            this.refreshTokenRememberMeValiditySeconds = refreshTokenRememberMeValiditySeconds;
        }

        public boolean isRefreshCookieSecure() {
            return refreshCookieSecure;
        }

        public void setRefreshCookieSecure(boolean refreshCookieSecure) {
            this.refreshCookieSecure = refreshCookieSecure;
        }

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public int getIpMaxAttempts() {
            return ipMaxAttempts;
        }

        public void setIpMaxAttempts(int ipMaxAttempts) {
            this.ipMaxAttempts = ipMaxAttempts;
        }

        public long getLoginLockSeconds() {
            return loginLockSeconds;
        }

        public void setLoginLockSeconds(long loginLockSeconds) {
            this.loginLockSeconds = loginLockSeconds;
        }
    }

    /** Consentement : version du texte que l'utilisateur doit accepter a l'inscription. */
    public static class Consent {

        private String currentVersion = "2026-10-v1";

        public String getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }
    }

    /** Envoi des enregistrements : dossier de stockage sur disque et limites acceptees. */
    public static class Audio {

        /** Dossier racine des fichiers audio (volume Docker sauvegarde en production). */
        private String storageDir = "data/audio";

        /** Taille maximale d'un fichier, en octets (une phrase courte tient largement dans 5 Mo). */
        private long maxBytes = 5_000_000;

        private int minDurationMs = 500;

        private int maxDurationMs = 30_000;

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public int getMinDurationMs() {
            return minDurationMs;
        }

        public void setMinDurationMs(int minDurationMs) {
            this.minDurationMs = minDurationMs;
        }

        public int getMaxDurationMs() {
            return maxDurationMs;
        }

        public void setMaxDurationMs(int maxDurationMs) {
            this.maxDurationMs = maxDurationMs;
        }
    }

    // jhipster-needle-application-properties-property-class
}
