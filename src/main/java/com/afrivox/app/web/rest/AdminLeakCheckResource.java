package com.afrivox.app.web.rest;

import com.afrivox.app.service.LeakCheckService;
import com.afrivox.app.service.LeakCheckService.Report;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Retrouve l'acheteur d'une copie diffusee sans autorisation ({@code /api/admin/**} est reserve a ROLE_ADMIN).
 * L'admin envoie un fichier audio WAV de la copie suspecte, ou son {@code manifest.tsv} (ou une liste de noms de
 * fichiers). Le fichier n'est pas conserve.
 */
@RestController
@RequestMapping("/api/admin/leak-check")
public class AdminLeakCheckResource {

    /** Un WAV de phrase ou un manifeste tiennent largement ; au-dela, ce n'est pas le bon fichier. */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final LeakCheckService service;

    public AdminLeakCheckResource(LeakCheckService service) {
        this.service = service;
    }

    @PostMapping
    public Report check(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestAlertException("Fichier trop volumineux", "leakCheck", "filetoolarge");
        }
        return service.check(file.getBytes());
    }
}
