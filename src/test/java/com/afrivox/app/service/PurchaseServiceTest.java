package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.domain.Purchase;
import com.afrivox.app.domain.PurchaseLink;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.repository.PurchaseLinkRepository;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Parcours d'achat : devis (heures x tarif x coefficient), garde-fous de l'achat, liens signes et limites. */
class PurchaseServiceTest {

    private static final long ME_ID = 1L;
    private static final long PURCHASE_ID = 5L;
    private static final long LINK_ID = 77L;
    private static final String SECRET = "secret-de-test-pour-les-jetons";

    @TempDir
    Path temp;

    /** Racine de stockage des archives : un sous-dossier, pour que « ../ » reste dans le dossier temporaire du test. */
    private Path storage;
    private PurchaseLink savedLink;

    private DatasetRepository datasets;
    private DatasetVersionRepository versions;
    private PurchaseRepository purchases;
    private PurchaseLinkRepository links;
    private DownloadLogRepository downloads;
    private UserProfileService profileService;
    private ApplicationProperties properties;
    private PurchaseService service;

    private UserProfile me;
    private Dataset dataset;
    private DatasetVersion version;
    private Purchase purchase;

    @BeforeEach
    void setUp() throws IOException {
        datasets = mock(DatasetRepository.class);
        versions = mock(DatasetVersionRepository.class);
        purchases = mock(PurchaseRepository.class);
        links = mock(PurchaseLinkRepository.class);
        downloads = mock(DownloadLogRepository.class);
        profileService = mock(UserProfileService.class);
        properties = new ApplicationProperties();
        storage = Files.createDirectory(temp.resolve("storage"));
        properties.getDatasets().setStorageDir(storage.toString());
        service = newService(SECRET);

        me = new UserProfile().id(ME_ID);
        when(profileService.getOrCreateCurrent()).thenReturn(me);

        dataset = new Dataset().id(3L);
        dataset.setSlug("agriculture-wolof");
        dataset.setTitle("Agriculture wolof");
        dataset.setPlan(DatasetPlan.PLUS);
        when(datasets.findOneBySlug("agriculture-wolof")).thenReturn(Optional.of(dataset));

        version = new DatasetVersion().id(9L);
        version.setDataset(dataset);
        version.setLabel("1.0");
        version.setStatus(VersionStatus.PUBLISHED);
        version.setHours(2f);
        version.setPricePerHourFcfa(30_000);
        version.setPackagePath("agriculture-wolof-1.0.zip");
        version.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
        when(versions.findOneByDatasetIdAndLabelAndStatus(3L, "1.0", VersionStatus.PUBLISHED)).thenReturn(Optional.of(version));
        Files.write(storage.resolve("agriculture-wolof-1.0.zip"), new byte[] { 1, 2, 3 });

        purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setProfile(me);
        purchase.setDatasetVersion(version);
        purchase.setLicense(LicenseKind.RESEARCH);
        // Colonne obligatoire en base : un achat reel a toujours un prix (0 pour un dataset gratuit).
        purchase.setPriceFcfa(60_000L);
        purchase.setCreatedAt(Instant.parse("2026-10-02T00:00:00Z"));
        purchase.setFingerprint("0123456789abcdef0123456789abcdef");
        purchase.setLicenseVersion("2026-10-v1");
        purchase.setLicenseAcceptedAt(Instant.parse("2026-10-02T00:00:00Z"));
        when(purchases.findOneByIdAndProfile(PURCHASE_ID, ME_ID)).thenReturn(Optional.of(purchase));

        when(purchases.saveAndFlush(any(Purchase.class))).thenAnswer(call -> call.getArgument(0));
        when(links.save(any(PurchaseLink.class))).thenAnswer(call -> {
            savedLink = call.getArgument(0);
            savedLink.setId(LINK_ID);
            return savedLink;
        });
        when(links.findById(LINK_ID)).thenAnswer(call -> Optional.ofNullable(savedLink));
        when(links.consumeDownload(anyLong(), any(Instant.class))).thenReturn(1);
    }

    private PurchaseService newService(String secret) {
        return new PurchaseService(properties, datasets, versions, purchases, links, downloads, profileService, secret);
    }

    private static void assertBadRequest(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, e ->
            assertThat(e.getStatusCode().value()).isEqualTo(status.value())
        );
    }

    // ---- Devis ---------------------------------------------------------------------------------------------------

    @Test
    void priceIsHoursTimesRateTimesLicenseCoefficient() {
        assertThat(service.quote("agriculture-wolof", "1.0", LicenseKind.PROTOTYPE).totalFcfa()).isEqualTo(30_000);
        assertThat(service.quote("agriculture-wolof", "1.0", LicenseKind.RESEARCH).totalFcfa()).isEqualTo(60_000);
        assertThat(service.quote("agriculture-wolof", "1.0", LicenseKind.COMMERCIAL).totalFcfa()).isEqualTo(180_000);
    }

    @Test
    void quoteCarriesTheVersionFigures() {
        PurchaseService.Quote quote = service.quote("agriculture-wolof", "1.0", LicenseKind.RESEARCH);

        assertThat(quote.free()).isFalse();
        assertThat(quote.hours()).isEqualTo(2f);
        assertThat(quote.pricePerHourFcfa()).isEqualTo(30_000);
        assertThat(quote.versionLabel()).isEqualTo("1.0");
    }

    @Test
    void priceIsRoundedToTheNearestFranc() {
        version.setHours(0.648f);

        // 0,648 h x 30 000 x 1 = 19 440
        assertThat(service.quote("agriculture-wolof", "1.0", LicenseKind.RESEARCH).totalFcfa()).isEqualTo(19_440);
    }

    @Test
    void multipliersComeFromConfiguration() {
        properties.getDatasets().setCommercialMultiplier(5.0);

        assertThat(service.quote("agriculture-wolof", "1.0", LicenseKind.COMMERCIAL).totalFcfa()).isEqualTo(300_000);
    }

    @Test
    void starterPlanIsFreeWhateverTheLicense() {
        dataset.setPlan(DatasetPlan.STARTER);
        version.setPricePerHourFcfa(null);

        PurchaseService.Quote quote = service.quote("agriculture-wolof", "1.0", LicenseKind.COMMERCIAL);

        assertThat(quote.free()).isTrue();
        assertThat(quote.totalFcfa()).isZero();
    }

    @Test
    void paidVersionWithoutPricingCannotBeQuoted() {
        version.setPricePerHourFcfa(null);

        assertBadRequest(() -> service.quote("agriculture-wolof", "1.0", LicenseKind.RESEARCH), "nopricing");

        version.setPricePerHourFcfa(30_000);
        version.setHours(null);
        assertBadRequest(() -> service.quote("agriculture-wolof", "1.0", LicenseKind.RESEARCH), "nopricing");
    }

    @Test
    void unknownDatasetOrUnpublishedVersionIsNotFound() {
        assertStatus(() -> service.quote("inconnu", "1.0", LicenseKind.RESEARCH), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.quote("agriculture-wolof", "9.9", LicenseKind.RESEARCH), HttpStatus.NOT_FOUND);
    }

    // ---- Achat ---------------------------------------------------------------------------------------------------

    @Test
    void licenseMustBeAccepted() {
        assertBadRequest(() -> service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, false), "licensenotaccepted");

        verify(purchases, never()).saveAndFlush(any());
    }

    @Test
    void purchaseRecordsThePriceTheLicenseTextAndASecretFingerprint() {
        PurchaseService.PurchaseView view = service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, true);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).saveAndFlush(captor.capture());
        Purchase saved = captor.getValue();
        assertThat(saved.getProfile()).isSameAs(me);
        assertThat(saved.getPriceFcfa()).isEqualTo(60_000L);
        assertThat(saved.getLicense()).isEqualTo(LicenseKind.RESEARCH);
        assertThat(saved.getLicenseVersion()).isEqualTo(properties.getDatasets().getLicenseVersion());
        assertThat(saved.getLicenseAcceptedAt()).isNotNull();
        assertThat(saved.getPaymentRef()).startsWith("SIM-");
        assertThat(saved.getFingerprint()).matches("[0-9a-f]{32}");
        assertThat(view.priceFcfa()).isEqualTo(60_000L);
        assertThat(view.datasetSlug()).isEqualTo("agriculture-wolof");
    }

    @Test
    void fingerprintsAreNotReusedBetweenPurchases() {
        service.purchase("agriculture-wolof", "1.0", LicenseKind.PROTOTYPE, true);
        service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, true);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues().get(0).getFingerprint()).isNotEqualTo(captor.getAllValues().get(1).getFingerprint());
    }

    @Test
    void freeDatasetHasNoPaymentReference() {
        dataset.setPlan(DatasetPlan.STARTER);

        service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, true);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPaymentRef()).isNull();
        assertThat(captor.getValue().getPriceFcfa()).isZero();
    }

    @Test
    void samePurchaseCannotBeMadeTwice() {
        when(purchases.existsByProfileIdAndDatasetVersionIdAndLicense(ME_ID, 9L, LicenseKind.RESEARCH)).thenReturn(true);

        assertBadRequest(() -> service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, true), "alreadypurchased");
        verify(purchases, never()).saveAndFlush(any());
    }

    @Test
    void sameVersionUnderAnotherLicenseIsAllowed() {
        when(purchases.existsByProfileIdAndDatasetVersionIdAndLicense(ME_ID, 9L, LicenseKind.RESEARCH)).thenReturn(true);

        service.purchase("agriculture-wolof", "1.0", LicenseKind.COMMERCIAL, true);

        verify(purchases).saveAndFlush(any(Purchase.class));
    }

    @Test
    void concurrentDuplicateIsCaughtByTheDatabaseConstraint() {
        when(purchases.saveAndFlush(any(Purchase.class))).thenThrow(new DataIntegrityViolationException("ux_purchase"));

        assertBadRequest(() -> service.purchase("agriculture-wolof", "1.0", LicenseKind.RESEARCH, true), "alreadypurchased");
    }

    // ---- Liens ---------------------------------------------------------------------------------------------------

    @Test
    void linkExpiresAfterTheConfiguredDelayAndIsLimitedInDownloads() {
        properties.getDatasets().setPurchaseLinkTtlSeconds(3_600);
        properties.getDatasets().setMaxDownloadsPerLink(2);
        Instant before = Instant.now();

        PurchaseService.Link link = service.createLink(PURCHASE_ID);

        assertThat(link.maxDownloads()).isEqualTo(2);
        assertThat(link.expiresAt()).isBetween(before.plusSeconds(3_600), Instant.now().plusSeconds(3_600));
        assertThat(link.token()).startsWith(LINK_ID + ".");
    }

    @Test
    void someoneElsesPurchaseIsNotFound() {
        assertStatus(() -> service.createLink(404L), HttpStatus.NOT_FOUND);
    }

    @Test
    void withdrawnVersionNoLongerGivesLinks() {
        version.setStatus(VersionStatus.WITHDRAWN);

        assertStatus(() -> service.createLink(PURCHASE_ID), HttpStatus.GONE);
    }

    @Test
    void dailyLinkLimitIsEnforced() {
        properties.getDatasets().setMaxLinksPerDay(5);
        when(links.countByPurchaseIdAndCreatedAtAfter(org.mockito.ArgumentMatchers.eq(PURCHASE_ID), any(Instant.class))).thenReturn(5L);

        assertStatus(() -> service.createLink(PURCHASE_ID), HttpStatus.TOO_MANY_REQUESTS);
        verify(links, never()).save(any());
    }

    // ---- Telechargement ------------------------------------------------------------------------------------------

    private String token() {
        return service.createLink(PURCHASE_ID).token();
    }

    @Test
    void validTokenOpensTheArchiveAndIsLogged() {
        PurchaseService.Archive archive = service.open(token(), "41.82.10.5");

        assertThat(archive.personalize()).isTrue();
        assertThat(archive.filename()).isEqualTo("agriculture-wolof-1.0-5.zip");
        assertThat(archive.file()).isEqualTo(storage.resolve("agriculture-wolof-1.0.zip").toAbsolutePath().normalize());
        assertThat(archive.fingerprint()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(archive.license()).isEqualTo(LicenseKind.RESEARCH);
        ArgumentCaptor<DownloadLog> log = ArgumentCaptor.forClass(DownloadLog.class);
        verify(downloads).save(log.capture());
        assertThat(log.getValue().getIp()).isEqualTo("41.82.10.5");
        assertThat(log.getValue().getProfile()).isSameAs(me);
    }

    @Test
    void oversizedIpIsNotStored() {
        service.open(token(), "x".repeat(46));

        ArgumentCaptor<DownloadLog> log = ArgumentCaptor.forClass(DownloadLog.class);
        verify(downloads).save(log.capture());
        assertThat(log.getValue().getIp()).isNull();
    }

    @Test
    void personalizationCanBeDisabled() {
        properties.getDatasets().setPersonalizeDownloads(false);

        PurchaseService.Archive archive = service.open(token(), null);

        assertThat(archive.personalize()).isFalse();
        assertThat(archive.filename()).isEqualTo("agriculture-wolof-1.0.zip");
    }

    @Test
    void olderPurchaseReceivesAFingerprintAtFirstDownload() {
        purchase.setFingerprint(null);

        PurchaseService.Archive archive = service.open(token(), null);

        assertThat(archive.fingerprint()).matches("[0-9a-f]{32}");
        verify(purchases).save(purchase);
    }

    @Test
    void malformedAndForgedTokensAreAllNotFound() {
        String valid = token();
        String id = valid.substring(0, valid.indexOf('.'));

        assertStatus(() -> service.open("pas-de-point", null), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.open(id + ".signature-fausse", null), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.open("78" + valid.substring(valid.indexOf('.')), null), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.open("a.b.c", null), HttpStatus.NOT_FOUND);
        verify(links, never()).consumeDownload(anyLong(), any());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = newService("un-autre-secret").createLink(PURCHASE_ID).token();

        assertStatus(() -> service.open(foreign, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void exhaustedOrExpiredLinkIsGone() {
        String token = token();
        when(links.consumeDownload(anyLong(), any(Instant.class))).thenReturn(0);

        assertStatus(() -> service.open(token, null), HttpStatus.GONE);
        verify(downloads, never()).save(any());
    }

    @Test
    void missingArchiveDoesNotUseUpADownload() throws IOException {
        String token = token();
        Files.delete(storage.resolve("agriculture-wolof-1.0.zip"));

        assertBadRequest(() -> service.open(token, null), "packagemissing");
        verify(links, never()).consumeDownload(anyLong(), any());
    }

    @Test
    void packagePathCannotEscapeTheStorageRoot() throws IOException {
        Files.write(temp.resolve("hors-racine.zip"), new byte[] { 1 });
        version.setPackagePath("../hors-racine.zip");
        String token = token();

        assertBadRequest(() -> service.open(token, null), "packagemissing");
    }

    @Test
    void versionWithdrawnAfterTheLinkWasIssuedIsGone() {
        String token = token();
        version.setStatus(VersionStatus.WITHDRAWN);

        assertStatus(() -> service.open(token, null), HttpStatus.GONE);
    }

    // ---- Liste ---------------------------------------------------------------------------------------------------

    @Test
    void myPurchasesAnnounceANewerPublishedVersion() {
        DatasetVersion newer = new DatasetVersion().id(10L);
        newer.setLabel("2.0");
        newer.setCreatedAt(Instant.parse("2026-10-08T00:00:00Z"));
        when(purchases.findAllByProfile(ME_ID)).thenReturn(List.of(purchase));
        when(versions.findAllByDatasetIdAndStatusOrderByCreatedAtDesc(3L, VersionStatus.PUBLISHED)).thenReturn(List.of(newer, version));

        List<PurchaseService.PurchaseView> mine = service.listMine();

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).newerVersionLabel()).isEqualTo("2.0");
    }

    @Test
    void noNewerVersionMeansNoBanner() {
        when(purchases.findAllByProfile(ME_ID)).thenReturn(List.of(purchase));
        when(versions.findAllByDatasetIdAndStatusOrderByCreatedAtDesc(3L, VersionStatus.PUBLISHED)).thenReturn(List.of(version));

        assertThat(service.listMine().get(0).newerVersionLabel()).isNull();
    }
}
