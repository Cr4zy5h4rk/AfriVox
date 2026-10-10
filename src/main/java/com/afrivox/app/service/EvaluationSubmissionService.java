package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Evaluation;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.TestReference;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.EvaluationRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.TestReferenceRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classement par predictions : le developpeur ne fournit pas son modele (le Hub ne stocke aucun poids), il fait tourner
 * le modele chez lui sur l'audio du jeu de test et renvoie un CSV {@code id,text}. Le serveur compare chaque ligne a la
 * transcription de reference gardee en base (jamais publiee) et calcule WER et CER.
 * <p>
 * Cout : lecture des references (quelques milliers de lignes) puis une distance d'edition par phrase, sans inference ;
 * l'envoi est borne (taille, lignes, mots par ligne, un envoi toutes les {@link #MIN_INTERVAL}).
 * <p>
 * Pas de {@code readOnly} : {@code getOrCreateCurrent} peut creer le profil.
 */
@Service
@Transactional
public class EvaluationSubmissionService {

    private static final String ENTITY_NAME = "evaluation";

    /** Taille maximale du CSV envoye. */
    static final int MAX_BYTES = 2 * 1024 * 1024;

    /** Une phrase de plus de mots n'est pas une phrase : on refuse plutot que de calculer une distance enorme. */
    static final int MAX_WORDS_PER_LINE = 200;

    /** Delai minimal entre deux envois pour le meme modele et le meme jeu de test. */
    static final Duration MIN_INTERVAL = Duration.ofMinutes(10);

    public record TestSetView(Long versionId, String slug, String title, String version, String languageCode, long sentences) {}

    public record Submission(Long evaluationId, double wer, double cer, int sentences, Instant submittedAt) {}

    private final MlModelRepository modelRepository;
    private final DatasetVersionRepository versionRepository;
    private final TestReferenceRepository referenceRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserProfileService userProfileService;

    public EvaluationSubmissionService(
        MlModelRepository modelRepository,
        DatasetVersionRepository versionRepository,
        TestReferenceRepository referenceRepository,
        EvaluationRepository evaluationRepository,
        UserProfileService userProfileService
    ) {
        this.modelRepository = modelRepository;
        this.versionRepository = versionRepository;
        this.referenceRepository = referenceRepository;
        this.evaluationRepository = evaluationRepository;
        this.userProfileService = userProfileService;
    }

    /** Jeux de test disponibles (versions publiees ayant des references), filtrables par langue. */
    @Transactional(readOnly = true)
    public List<TestSetView> testSets(String languageCode) {
        String wanted = languageCode == null ? "" : languageCode.trim().toLowerCase(Locale.ROOT);
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : referenceRepository.countsByVersion()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        List<TestSetView> result = new ArrayList<>();
        for (DatasetVersion v : versionRepository.findAllById(counts.keySet())) {
            if (v.getStatus() != VersionStatus.PUBLISHED || v.getDataset() == null) {
                continue;
            }
            String code = v.getDataset().getLanguage() == null ? null : v.getDataset().getLanguage().getCode();
            if (!wanted.isEmpty() && !wanted.equals(code)) {
                continue;
            }
            result.add(
                new TestSetView(v.getId(), v.getDataset().getSlug(), v.getDataset().getTitle(), v.getLabel(), code, counts.get(v.getId()))
            );
        }
        result.sort((a, b) -> a.title().compareToIgnoreCase(b.title()));
        return result;
    }

    /** Modele de CSV : en-tete et un identifiant par ligne, texte vide. Les identifiants sont ceux de splits/test.tsv. */
    @Transactional(readOnly = true)
    public String template(Long versionId) {
        List<TestReference> refs = referenceRepository.findAllByVersionIdOrderByRecordingId(versionId);
        if (refs.isEmpty()) {
            throw new BadRequestAlertException("Jeu de test introuvable", ENTITY_NAME, "testsetnotfound");
        }
        StringBuilder sb = new StringBuilder("id,text\n");
        refs.forEach(r -> sb.append(r.getRecordingId()).append(",\n"));
        return sb.toString();
    }

    public Submission submit(Long modelId, Long versionId, byte[] csv) {
        MlModel model = ownModel(modelId);
        if (model.getTask() != MlTask.ASR) {
            throw new BadRequestAlertException("Le classement ne mesure que la reconnaissance vocale (ASR)", ENTITY_NAME, "tasknotasr");
        }
        DatasetVersion version = versionRepository
            .findById(versionId)
            .filter(v -> v.getStatus() == VersionStatus.PUBLISHED)
            .orElseThrow(() -> new BadRequestAlertException("Jeu de test introuvable", ENTITY_NAME, "testsetnotfound"));
        List<TestReference> refs = referenceRepository.findAllByVersionIdOrderByRecordingId(version.getId());
        if (refs.isEmpty()) {
            throw new BadRequestAlertException("Cette version n'a pas de jeu de test", ENTITY_NAME, "testsetnotfound");
        }
        String testLanguage = version.getDataset().getLanguage() == null ? null : version.getDataset().getLanguage().getCode();
        if (model.getLanguage() != null && testLanguage != null && !model.getLanguage().getCode().equals(testLanguage)) {
            throw new BadRequestAlertException("La langue du modele differe de celle du jeu de test", ENTITY_NAME, "languagemismatch");
        }
        evaluationRepository
            .findFirstByModelIdAndTestSetIdOrderBySubmittedAtDesc(model.getId(), version.getId())
            .filter(last -> last.getSubmittedAt().isAfter(Instant.now().minus(MIN_INTERVAL)))
            .ifPresent(last -> {
                throw new BadRequestAlertException("Un envoi toutes les 10 minutes au plus", ENTITY_NAME, "toosoon");
            });

        Map<Long, String> predictions = parse(csv, refs.size());
        Set<Long> expected = new HashSet<>();
        refs.forEach(r -> expected.add(r.getRecordingId()));
        if (!expected.containsAll(predictions.keySet())) {
            throw new BadRequestAlertException("Le CSV contient des identifiants inconnus", ENTITY_NAME, "idunknown");
        }
        if (predictions.size() != expected.size()) {
            // Un sous-ensemble choisi (les phrases faciles) fausserait le score : il faut repondre a toutes les phrases.
            throw new BadRequestAlertException("Il manque des identifiants dans le CSV", ENTITY_NAME, "idsmissing");
        }

        ErrorRate.Counts words = new ErrorRate.Counts(0, 0);
        ErrorRate.Counts chars = new ErrorRate.Counts(0, 0);
        for (TestReference ref : refs) {
            String prediction = predictions.get(ref.getRecordingId());
            words = words.plus(ErrorRate.words(ref.getText(), prediction));
            chars = chars.plus(ErrorRate.characters(ref.getText(), prediction));
        }
        if (words.referenceLength() == 0) {
            throw new BadRequestAlertException("Jeu de test sans texte de reference", ENTITY_NAME, "testsetnotfound");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setModel(model);
        evaluation.setTestSet(version);
        evaluation.setWer((float) round2(words.percent()));
        evaluation.setCer((float) round2(chars.percent()));
        evaluation.setSubmittedAt(Instant.now());
        Evaluation saved = evaluationRepository.save(evaluation);
        return new Submission(saved.getId(), saved.getWer(), saved.getCer(), refs.size(), saved.getSubmittedAt());
    }

    /** Mon modele (ou n'importe lequel pour l'admin). Meme reponse si le modele n'existe pas ou n'est pas a moi. */
    private MlModel ownModel(Long modelId) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        boolean admin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        return modelRepository
            .findById(modelId)
            .filter(m -> admin || (m.getOwner() != null && m.getOwner().getId().equals(me.getId())))
            .orElseThrow(() -> new BadRequestAlertException("Modele introuvable", ENTITY_NAME, "modelnotfound"));
    }

    private static double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }

    /**
     * Lit {@code id,text} (virgule ou tabulation, UTF-8, guillemets acceptes). Refuse un fichier trop gros, une ligne
     * trop longue, un identifiant en double ou trop de lignes.
     */
    static Map<Long, String> parse(byte[] csv, int maxRows) {
        if (csv == null || csv.length == 0) {
            throw new BadRequestAlertException("Fichier vide", ENTITY_NAME, "csvinvalid");
        }
        if (csv.length > MAX_BYTES) {
            throw new BadRequestAlertException("Fichier trop volumineux", ENTITY_NAME, "csvtoolarge");
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(csv))
                .toString();
        } catch (CharacterCodingException e) {
            throw new BadRequestAlertException("Le fichier doit etre en UTF-8", ENTITY_NAME, "csvinvalid");
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        int firstEnd = text.indexOf('\n');
        String header = firstEnd < 0 ? text : text.substring(0, firstEnd);
        char delimiter = header.indexOf('\t') >= 0 ? '\t' : ',';

        List<List<String>> records = records(text, delimiter);
        if (records.isEmpty()) {
            throw new BadRequestAlertException("Fichier vide", ENTITY_NAME, "csvinvalid");
        }
        List<String> head = records.get(0);
        if (head.size() != 2 || !head.get(0).trim().equalsIgnoreCase("id") || !head.get(1).trim().equalsIgnoreCase("text")) {
            throw new BadRequestAlertException("L'en-tete doit etre id,text", ENTITY_NAME, "csvinvalid");
        }
        Map<Long, String> result = new HashMap<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> r = records.get(i);
            if (r.size() == 1 && r.get(0).isBlank()) {
                continue; // ligne vide
            }
            if (r.size() != 2) {
                throw new BadRequestAlertException("Ligne " + (i + 1) + " : deux colonnes attendues", ENTITY_NAME, "csvinvalid");
            }
            long id;
            try {
                id = Long.parseLong(r.get(0).trim());
            } catch (NumberFormatException e) {
                throw new BadRequestAlertException("Ligne " + (i + 1) + " : identifiant invalide", ENTITY_NAME, "csvinvalid");
            }
            if (ErrorRate.wordCount(r.get(1)) > MAX_WORDS_PER_LINE) {
                throw new BadRequestAlertException("Ligne " + (i + 1) + " : phrase trop longue", ENTITY_NAME, "texttoolong");
            }
            if (result.put(id, r.get(1)) != null) {
                throw new BadRequestAlertException("Identifiant en double : " + id, ENTITY_NAME, "idduplicate");
            }
            if (result.size() > maxRows) {
                throw new BadRequestAlertException("Trop de lignes", ENTITY_NAME, "idunknown");
            }
        }
        return result;
    }

    /** Decoupe en enregistrements en respectant les guillemets (doublement pour un guillemet litteral). */
    private static List<List<String>> records(String text, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.length() == 0) {
                quoted = true;
            } else if (c == delimiter) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                records.add(fields);
                fields = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new BadRequestAlertException("Guillemet non ferme", ENTITY_NAME, "csvinvalid");
        }
        if (field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            records.add(fields);
        }
        return records;
    }
}
