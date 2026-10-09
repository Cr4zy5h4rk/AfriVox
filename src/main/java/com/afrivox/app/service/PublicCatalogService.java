package com.afrivox.app.service;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Evaluation;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.LanguageStatus;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.domain.enumeration.ScriptKind;
import com.afrivox.app.domain.enumeration.TextDirection;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.EvaluationRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.RecordingRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue public (lecture seule) : statistiques, langues, datasets, modeles, classement.
 * Principes :
 * <ul>
 *   <li>on n'expose jamais de chemin de fichier ni d'identifiant interne inutile (les vues ci-dessous sont des
 *       {@code record} ecrits a la main, pas les entites) ;</li>
 *   <li>tous les chiffres sont calcules depuis la base a chaque appel : rien n'est stocke ni invente ; sans donnees,
 *       les compteurs valent 0 et les listes sont vides.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogService {

    private static final int MAX_PAGE_SIZE = 48;
    private static final int MAX_LEADERBOARD = 100;
    private static final int MAX_VERSION_OPTIONS = 30;
    private static final double MS_PER_HOUR = 3_600_000d;

    /** Chiffres de la page d'accueil. */
    public record Stats(
        long activeLanguages,
        long contributors,
        long recordingsReceived,
        long recordingsValidated,
        double validatedHours,
        long publishedDatasets,
        long models
    ) {}

    public record LanguageView(
        String code,
        String name,
        ScriptKind script,
        TextDirection direction,
        LanguageStatus status,
        int minValidators,
        long validatedRecordings
    ) {}

    public record VersionView(
        String label,
        Float hours,
        Integer speakers,
        Float qualityScore,
        Integer pricePerHourFcfa,
        LicenseKind license,
        Instant createdAt
    ) {}

    public record DatasetSummary(
        String slug,
        String title,
        String description,
        String domain,
        DatasetPlan plan,
        String languageCode,
        String languageName,
        boolean published,
        VersionView latestVersion,
        long validatedRecordings,
        double validatedHours
    ) {}

    public record DatasetDetail(DatasetSummary dataset, List<VersionView> versions) {}

    /** Une version de dataset citee par un modele. */
    public record DatasetRef(String slug, String title, String version) {}

    /** Une version publiee proposee dans le formulaire de publication d'un modele. */
    public record VersionOption(Long id, String slug, String title, String version, String languageCode) {}

    public record ModelSummary(
        Long id,
        String name,
        MlTask task,
        String description,
        String license,
        String url,
        Integer downloads,
        Instant createdAt,
        String ownerName,
        String languageCode,
        String languageName,
        String trainedOnDataset,
        String trainedOnVersion,
        List<DatasetRef> otherDatasets,
        Float bestWer
    ) {}

    public record EvaluationRow(
        Long modelId,
        String modelName,
        MlTask task,
        String languageCode,
        String languageName,
        Float wer,
        Float cer,
        String testSetDataset,
        String testSetVersion,
        Instant submittedAt
    ) {}

    public record ModelDetail(ModelSummary model, List<EvaluationRow> evaluations) {}

    public record PageView<T>(List<T> items, int page, int size, long total, int pages) {}

    private final RecordingRepository recordingRepository;
    private final LanguageRepository languageRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final MlModelRepository modelRepository;
    private final EvaluationRepository evaluationRepository;

    public PublicCatalogService(
        RecordingRepository recordingRepository,
        LanguageRepository languageRepository,
        DatasetRepository datasetRepository,
        DatasetVersionRepository datasetVersionRepository,
        MlModelRepository modelRepository,
        EvaluationRepository evaluationRepository
    ) {
        this.recordingRepository = recordingRepository;
        this.languageRepository = languageRepository;
        this.datasetRepository = datasetRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.modelRepository = modelRepository;
        this.evaluationRepository = evaluationRepository;
    }

    public Stats stats() {
        long submitted = recordingRepository.countByStatus(RecordingStatus.SUBMITTED);
        long validated = recordingRepository.countByStatus(RecordingStatus.VALIDATED);
        return new Stats(
            languageRepository.countByStatus(LanguageStatus.ACTIVE),
            recordingRepository.countActiveContributors(),
            submitted + validated,
            validated,
            recordingRepository.validatedDurationMs() / MS_PER_HOUR,
            datasetVersionRepository.countDatasetsWithPublishedVersion(),
            modelRepository.count()
        );
    }

    public List<LanguageView> languages() {
        return languageRepository
            .findAllByOrderByNameAsc()
            .stream()
            .map(l ->
                new LanguageView(
                    l.getCode(),
                    l.getName(),
                    l.getScript(),
                    l.getDirection(),
                    l.getStatus(),
                    l.getMinValidators() == null ? 0 : l.getMinValidators(),
                    recordingRepository.countValidatedByLanguage(l.getId())
                )
            )
            .toList();
    }

    public PageView<DatasetSummary> searchDatasets(String q, String language, DatasetPlan plan, String domain, int page, int size) {
        String text = normalize(q);
        String lang = normalize(language);
        String dom = normalize(domain);
        Specification<Dataset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!text.isEmpty()) {
                String like = likePattern(text);
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like, '\\')
                    )
                );
            }
            if (!lang.isEmpty()) {
                predicates.add(cb.equal(root.get("language").get("code"), lang));
            }
            if (plan != null) {
                predicates.add(cb.equal(root.get("plan"), plan));
            }
            if (!dom.isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.<String>get("domain")), dom));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Dataset> result = datasetRepository.findAll(spec, pageRequest(page, size, Sort.by("title")));
        return toPageView(result.map(this::toSummary));
    }

    public Optional<DatasetDetail> dataset(String slug) {
        return datasetRepository
            .findOneBySlug(slug)
            .map(d ->
                new DatasetDetail(
                    toSummary(d),
                    datasetVersionRepository
                        .findAllByDatasetIdAndStatusOrderByCreatedAtDesc(d.getId(), VersionStatus.PUBLISHED)
                        .stream()
                        .map(PublicCatalogService::toVersionView)
                        .toList()
                )
            );
    }

    public PageView<ModelSummary> searchModels(String q, MlTask task, String language, int page, int size) {
        String text = normalize(q);
        String lang = normalize(language);
        Specification<MlModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!text.isEmpty()) {
                String like = likePattern(text);
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), like, '\\')
                    )
                );
            }
            if (task != null) {
                predicates.add(cb.equal(root.get("task"), task));
            }
            if (!lang.isEmpty()) {
                predicates.add(cb.equal(root.get("language").get("code"), lang));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<MlModel> result = modelRepository.findAll(spec, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return toPageView(result.map(this::toModelSummary));
    }

    public Optional<ModelDetail> model(Long id) {
        return modelRepository
            .findById(id)
            .map(m ->
                new ModelDetail(
                    toModelSummary(m),
                    evaluationRepository.findForModel(m.getId()).stream().map(PublicCatalogService::toEvaluationRow).toList()
                )
            );
    }

    public List<EvaluationRow> leaderboard(String language, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LEADERBOARD));
        return evaluationRepository
            .leaderboard(normalize(language), PageRequest.of(0, size))
            .stream()
            .map(PublicCatalogService::toEvaluationRow)
            .toList();
    }

    /** Vue publique d'un modele (sert aussi a {@link ModelPublishingService}). */
    public ModelSummary toModelSummary(MlModel m) {
        Language language = m.getLanguage();
        DatasetVersion trained = m.getTrainedOn();
        return new ModelSummary(
            m.getId(),
            m.getName(),
            m.getTask(),
            m.getDescription(),
            m.getLicense(),
            m.getUrl(),
            m.getDownloads(),
            m.getCreatedAt(),
            m.getOwner() == null ? null : m.getOwner().getDisplayName(),
            language == null ? null : language.getCode(),
            language == null ? null : language.getName(),
            trained == null || trained.getDataset() == null ? null : trained.getDataset().getSlug(),
            trained == null ? null : trained.getLabel(),
            m
                .getOtherDatasets()
                .stream()
                .filter(v -> v.getDataset() != null)
                .sorted(Comparator.comparing((DatasetVersion v) -> v.getDataset().getTitle()).thenComparing(DatasetVersion::getLabel))
                .map(v -> new DatasetRef(v.getDataset().getSlug(), v.getDataset().getTitle(), v.getLabel()))
                .toList(),
            evaluationRepository.bestWer(m.getId())
        );
    }

    /**
     * Versions publiees que l'on peut citer comme donnees d'entrainement, filtrees par texte (titre ou slug) et
     * limitees : le catalogue peut compter des centaines de datasets, on ne les envoie jamais tous.
     */
    public List<VersionOption> publishedVersions(String q, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_VERSION_OPTIONS));
        return datasetVersionRepository
            .searchPublishedWithDataset(likePattern(normalize(q)), PageRequest.of(0, size))
            .stream()
            .map(v ->
                new VersionOption(
                    v.getId(),
                    v.getDataset().getSlug(),
                    v.getDataset().getTitle(),
                    v.getLabel(),
                    v.getDataset().getLanguage() == null ? null : v.getDataset().getLanguage().getCode()
                )
            )
            .toList();
    }

    private DatasetSummary toSummary(Dataset d) {
        Language language = d.getLanguage();
        VersionView latest = datasetVersionRepository
            .findAllByDatasetIdAndStatusOrderByCreatedAtDesc(d.getId(), VersionStatus.PUBLISHED)
            .stream()
            .findFirst()
            .map(PublicCatalogService::toVersionView)
            .orElse(null);
        long recordings = language == null ? 0 : recordingRepository.countValidatedByLanguage(language.getId());
        double hours = language == null ? 0 : recordingRepository.validatedDurationMsByLanguage(language.getId()) / MS_PER_HOUR;
        return new DatasetSummary(
            d.getSlug(),
            d.getTitle(),
            d.getDescription(),
            d.getDomain(),
            d.getPlan(),
            language == null ? null : language.getCode(),
            language == null ? null : language.getName(),
            latest != null,
            latest,
            recordings,
            hours
        );
    }

    private static VersionView toVersionView(DatasetVersion v) {
        return new VersionView(
            v.getLabel(),
            v.getHours(),
            v.getSpeakers(),
            v.getQualityScore(),
            v.getPricePerHourFcfa(),
            v.getLicense(),
            v.getCreatedAt()
        );
    }

    private static EvaluationRow toEvaluationRow(Evaluation e) {
        MlModel m = e.getModel();
        Language language = m == null ? null : m.getLanguage();
        DatasetVersion testSet = e.getTestSet();
        return new EvaluationRow(
            m == null ? null : m.getId(),
            m == null ? null : m.getName(),
            m == null ? null : m.getTask(),
            language == null ? null : language.getCode(),
            language == null ? null : language.getName(),
            e.getWer(),
            e.getCer(),
            testSet == null || testSet.getDataset() == null ? null : testSet.getDataset().getSlug(),
            testSet == null ? null : testSet.getLabel(),
            e.getSubmittedAt()
        );
    }

    private static <T> PageView<T> toPageView(Page<T> page) {
        return new PageView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)), sort);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /** Motif LIKE sur le texte saisi, avec echappement de % _ \ (la saisie est du texte, pas un motif). */
    private static String likePattern(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
