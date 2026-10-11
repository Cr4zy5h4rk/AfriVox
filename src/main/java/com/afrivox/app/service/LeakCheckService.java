package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Purchase;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrouve l'acheteur d'une copie diffusee sans autorisation. Deux indices, selon ce que l'on a :
 * <ul>
 *   <li><b>un fichier audio WAV</b> : on compare ses bits de poids faible a la suite attendue pour chaque achat
 *       ({@link WavWatermark}) ; une concordance quasi totale designe l'achat ;</li>
 *   <li><b>des noms de fichiers</b> (le {@code manifest.tsv} de la copie, ou un listing de l'archive) : les noms
 *       {@code audio/<16 caracteres>.<ext>} derivent de l'empreinte de chaque achat ; on compte combien concordent.</li>
 * </ul>
 * C'est un indice, pas une preuve : un fichier recode ou renomme echappe a la detection, et un resultat doit etre
 * recoupe avec le journal des telechargements avant toute decision.
 */
@Service
@Transactional(readOnly = true)
public class LeakCheckService {

    /** Concordance minimale des bits de poids faible (0,5 est le hasard). */
    static final double AUDIO_THRESHOLD = 0.98;

    /** Part minimale des noms du fichier suspect retrouves dans la copie d'un achat. */
    static final double NAMES_THRESHOLD = 0.5;

    /** En dessous, une concordance de noms n'a aucune valeur. */
    static final int MIN_NAMES = 3;

    private static final Pattern HASHED_NAME = Pattern.compile(
        "(?:^|[/\\\\\\s\"',;])(?:audio[/\\\\])?([0-9a-f]{16})\\.[A-Za-z0-9]{2,5}(?=$|[\\s\"',;])"
    );

    /** Un achat qui correspond a l'indice. */
    public record Match(Long purchaseId, String datasetSlug, String versionLabel, String buyer, Instant purchasedAt, double score) {}

    /** Resultat : le type d'indice utilise, ce qui a ete examine, et les achats concordants (meilleurs d'abord). */
    public record Report(String kind, int inspected, List<Match> matches) {}

    private final PurchaseRepository purchaseRepository;
    private final Path datasetRoot;

    public LeakCheckService(PurchaseRepository purchaseRepository, ApplicationProperties applicationProperties) {
        this.purchaseRepository = purchaseRepository;
        this.datasetRoot = Path.of(applicationProperties.getDatasets().getStorageDir()).toAbsolutePath().normalize();
    }

    /** Analyse un fichier envoye : un WAV est traite comme indice audio, tout autre fichier comme une liste de noms. */
    public Report check(byte[] content) {
        if (content.length == 0) {
            throw new BadRequestAlertException("Fichier vide", "leakCheck", "emptyfile");
        }
        boolean wav = content.length > 12 && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F';
        return wav ? checkAudio(content) : checkNames(new String(content, StandardCharsets.UTF_8));
    }

    Report checkAudio(byte[] wav) {
        List<Purchase> purchases = purchaseRepository.findAllTraceable();
        List<Match> matches = new java.util.ArrayList<>();
        for (Purchase purchase : purchases) {
            double score = WavWatermark.score(wav, ArchivePersonalizer.watermarkKey(purchase.getFingerprint()));
            if (!Double.isNaN(score) && score >= AUDIO_THRESHOLD) {
                matches.add(toMatch(purchase, score));
            }
        }
        matches.sort(Comparator.comparingDouble(Match::score).reversed());
        return new Report("AUDIO", purchases.size(), matches);
    }

    Report checkNames(String text) {
        Set<String> suspect = new HashSet<>();
        Matcher matcher = HASHED_NAME.matcher(text);
        while (matcher.find()) {
            suspect.add(matcher.group(1));
        }
        if (suspect.size() < MIN_NAMES) {
            return new Report("NAMES", suspect.size(), List.of());
        }
        List<Purchase> purchases = purchaseRepository.findAllTraceable();
        Map<Long, List<String>> originalsByVersion = new HashMap<>();
        List<Match> matches = new java.util.ArrayList<>();
        for (Purchase purchase : purchases) {
            DatasetVersion version = purchase.getDatasetVersion();
            List<String> originals = originalsByVersion.computeIfAbsent(version.getId(), id -> originalNames(version));
            if (originals.isEmpty()) {
                continue;
            }
            int found = 0;
            for (String original : originals) {
                String hashed = ArchivePersonalizer.hashedName(purchase.getFingerprint(), original);
                // hashedName renvoie « audio/<16>.<ext> » : on compare la partie hachee seule
                if (suspect.contains(hashed.substring("audio/".length(), "audio/".length() + 16))) {
                    found++;
                }
            }
            double share = (double) found / suspect.size();
            if (found >= MIN_NAMES && share >= NAMES_THRESHOLD) {
                matches.add(toMatch(purchase, share));
            }
        }
        matches.sort(Comparator.comparingDouble(Match::score).reversed());
        return new Report("NAMES", suspect.size(), matches);
    }

    private List<String> originalNames(DatasetVersion version) {
        if (version.getPackagePath() == null) {
            return List.of();
        }
        Path file = datasetRoot.resolve(version.getPackagePath()).normalize();
        if (!file.startsWith(datasetRoot) || !Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            return ArchivePersonalizer.audioNames(file);
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Match toMatch(Purchase purchase, double score) {
        DatasetVersion version = purchase.getDatasetVersion();
        return new Match(
            purchase.getId(),
            version.getDataset().getSlug(),
            version.getLabel(),
            purchase.getProfile().getDisplayName(),
            purchase.getCreatedAt(),
            score
        );
    }
}
