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

    private final Datasets datasets = new Datasets();

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

    public Datasets getDatasets() {
        return datasets;
    }

    public Audio getAudio() {
        return audio;
    }

    // jhipster-needle-application-properties-property-getter

    /** Datasets publies : dossier des archives et duree de validite des liens de telechargement signes. */
    public static class Datasets {

        /** Dossier des archives .zip, a sauvegarder comme les audios (volume Docker en production). */
        private String storageDir = "data/datasets";

        /** Duree de validite d'un lien de telechargement, en secondes. */
        private long linkTtlSeconds = 600;

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }

        public long getLinkTtlSeconds() {
            return linkTtlSeconds;
        }

        public void setLinkTtlSeconds(long linkTtlSeconds) {
            this.linkTtlSeconds = linkTtlSeconds;
        }

        /** Validite d'un lien lie a un achat (48 h : dans la fourchette 24 a 72 h du cahier des charges). */
        private long purchaseLinkTtlSeconds = 172_800;

        /** Nombre de telechargements permis par lien d'achat. */
        private int maxDownloadsPerLink = 5;

        /** Nombre de liens qu'on peut generer par achat sur 24 h (limite le partage). */
        private int maxLinksPerDay = 5;

        /** Version du texte de licence que l'acheteur accepte ; a changer quand le texte change. */
        private String licenseVersion = "2026-10-v1";

        /**
         * Coefficient applique au tarif horaire de la version selon la licence choisie. Valeurs de depart a valider
         * par l'equipe : la licence commerciale couvre plus d'usages, donc coute plus.
         */
        private double researchMultiplier = 1.0;

        private double prototypeMultiplier = 0.5;

        private double commercialMultiplier = 3.0;

        public long getPurchaseLinkTtlSeconds() {
            return purchaseLinkTtlSeconds;
        }

        public void setPurchaseLinkTtlSeconds(long purchaseLinkTtlSeconds) {
            this.purchaseLinkTtlSeconds = purchaseLinkTtlSeconds;
        }

        public int getMaxDownloadsPerLink() {
            return maxDownloadsPerLink;
        }

        public void setMaxDownloadsPerLink(int maxDownloadsPerLink) {
            this.maxDownloadsPerLink = maxDownloadsPerLink;
        }

        public int getMaxLinksPerDay() {
            return maxLinksPerDay;
        }

        public void setMaxLinksPerDay(int maxLinksPerDay) {
            this.maxLinksPerDay = maxLinksPerDay;
        }

        public String getLicenseVersion() {
            return licenseVersion;
        }

        public void setLicenseVersion(String licenseVersion) {
            this.licenseVersion = licenseVersion;
        }

        public double getResearchMultiplier() {
            return researchMultiplier;
        }

        public void setResearchMultiplier(double researchMultiplier) {
            this.researchMultiplier = researchMultiplier;
        }

        public double getPrototypeMultiplier() {
            return prototypeMultiplier;
        }

        public void setPrototypeMultiplier(double prototypeMultiplier) {
            this.prototypeMultiplier = prototypeMultiplier;
        }

        public double getCommercialMultiplier() {
            return commercialMultiplier;
        }

        public void setCommercialMultiplier(double commercialMultiplier) {
            this.commercialMultiplier = commercialMultiplier;
        }
    }

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
