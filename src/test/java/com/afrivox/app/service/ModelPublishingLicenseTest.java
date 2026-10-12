package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Publication d'un modèle : le plafond vient des licences d'achat des datasets cités. */
class ModelPublishingLicenseTest {

    private static final long OWNER_ID = 1L;
    private static final long V10 = 10L;
    private static final long V11 = 11L;

    private MlModelRepository models;
    private PurchaseRepository purchases;
    private ModelPublishingService service;
    private final List<Object[]> held = new ArrayList<>();

    @BeforeEach
    void setUp() {
        models = mock(MlModelRepository.class);
        purchases = mock(PurchaseRepository.class);
        DatasetVersionRepository versions = mock(DatasetVersionRepository.class);
        UserProfileService profileService = mock(UserProfileService.class);
        when(profileService.getOrCreateCurrent()).thenReturn(new UserProfile().id(OWNER_ID));
        for (long id : new long[] { V10, V11 }) {
            DatasetVersion v = new DatasetVersion().id(id);
            v.setStatus(VersionStatus.PUBLISHED);
            when(versions.findById(id)).thenReturn(Optional.of(v));
        }
        when(models.save(any(MlModel.class))).thenAnswer(call -> call.getArgument(0));
        when(purchases.findHeldLicenses(org.mockito.ArgumentMatchers.eq(OWNER_ID), anyCollection())).thenAnswer(call -> held);
        service = new ModelPublishingService(
            models,
            mock(LanguageRepository.class),
            versions,
            profileService,
            mock(PublicCatalogService.class),
            purchases
        );
    }

    private void holds(long version, LicenseKind license) {
        held.add(new Object[] { version, license });
    }

    private static ModelPublishingService.NewModel model(Long primary, List<Long> others, String license, LicenseKind usage) {
        return new ModelPublishingService.NewModel("modele", MlTask.ASR, null, license, null, null, null, primary, others, usage);
    }

    private MlModel savedModel() {
        ArgumentCaptor<MlModel> captor = ArgumentCaptor.forClass(MlModel.class);
        verify(models).save(captor.capture());
        return captor.getValue();
    }

    private static void assertRejected(Runnable publish, String errorKey) {
        assertThatThrownBy(publish::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(errorKey)
        );
    }

    @Test
    void modelWithoutDatasetHasNothingToInherit() {
        service.publish(model(null, null, "MIT", null));

        assertThat(savedModel().getUsageKind()).isNull();
        verify(purchases, never()).findHeldLicenses(any(), anyCollection());
    }

    @Test
    void usageIsRequiredAsSoonAsADatasetIsCited() {
        holds(V10, LicenseKind.COMMERCIAL);

        assertRejected(() -> service.publish(model(V10, null, null, null)), "usagerequired");
        verify(models, never()).save(any());
    }

    @Test
    void citedDatasetMustHaveBeenPurchased() {
        assertRejected(() -> service.publish(model(V10, null, null, LicenseKind.RESEARCH)), "licensenotheld");
    }

    @Test
    void aSecondCitedDatasetMustAlsoHaveBeenPurchased() {
        holds(V10, LicenseKind.COMMERCIAL);

        assertRejected(() -> service.publish(model(V10, List.of(V11), null, LicenseKind.RESEARCH)), "licensenotheld");
    }

    @Test
    void commercialModelOnResearchDatasetIsRefused() {
        holds(V10, LicenseKind.RESEARCH);

        assertRejected(() -> service.publish(model(V10, null, null, LicenseKind.COMMERCIAL)), "licenseinheritance");
        verify(models, never()).save(any());
    }

    @Test
    void researchModelOnResearchDatasetIsAccepted() {
        holds(V10, LicenseKind.RESEARCH);

        service.publish(model(V10, null, "CC-BY-NC-4.0", LicenseKind.RESEARCH));

        assertThat(savedModel().getUsageKind()).isEqualTo(LicenseKind.RESEARCH);
    }

    @Test
    void permissiveLicenseNameOnResearchDatasetIsAConflict() {
        holds(V10, LicenseKind.RESEARCH);

        assertRejected(() -> service.publish(model(V10, null, "MIT", LicenseKind.RESEARCH)), "licenseconflict");
    }

    @Test
    void commercialDatasetAllowsACommercialModelUnderMit() {
        holds(V10, LicenseKind.COMMERCIAL);

        service.publish(model(V10, null, "MIT", LicenseKind.COMMERCIAL));

        assertThat(savedModel().getUsageKind()).isEqualTo(LicenseKind.COMMERCIAL);
    }

    @Test
    void mostRestrictiveOfSeveralDatasetsSetsTheCeiling() {
        holds(V10, LicenseKind.COMMERCIAL);
        holds(V11, LicenseKind.PROTOTYPE);

        assertRejected(() -> service.publish(model(V10, List.of(V11), null, LicenseKind.RESEARCH)), "licenseinheritance");
    }

    @Test
    void severalPurchasesOfOneVersionCountForTheBroadest() {
        holds(V10, LicenseKind.PROTOTYPE);
        holds(V10, LicenseKind.COMMERCIAL);

        service.publish(model(V10, null, null, LicenseKind.COMMERCIAL));

        assertThat(savedModel().getUsageKind()).isEqualTo(LicenseKind.COMMERCIAL);
    }

    @Test
    void unknownLicenseNameIsNotJudgedOnlyTheDeclaredUseIs() {
        holds(V10, LicenseKind.RESEARCH);

        service.publish(model(V10, null, "Licence maison", LicenseKind.RESEARCH));

        assertThat(savedModel().getUsageKind()).isEqualTo(LicenseKind.RESEARCH);
    }
}
