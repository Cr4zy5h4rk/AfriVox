package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.service.PurchaseService;
import com.afrivox.app.service.PurchaseService.Archive;
import com.afrivox.app.service.PurchaseService.Link;
import com.afrivox.app.service.PurchaseService.PurchaseView;
import com.afrivox.app.service.PurchaseService.Quote;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Achat de datasets : devis public, achat (licence acceptee + paiement simule), liste « Mes achats », puis liens de
 * telechargement lies a l'achat. Comme pour les autres telechargements, le GET du fichier est public : c'est la
 * signature du lien qui l'autorise.
 */
@RestController
@RequestMapping("/api")
public class PurchaseResource {

    public record PurchaseRequest(@NotBlank String slug, @NotBlank String versionLabel, @NotNull LicenseKind license, boolean accepted) {}

    public record LinkView(String url, Instant expiresAt, int maxDownloads) {}

    private final PurchaseService service;

    public PurchaseResource(PurchaseService service) {
        this.service = service;
    }

    @GetMapping("/public/datasets/{slug}/versions/{label}/quote")
    public Quote quote(@PathVariable String slug, @PathVariable String label, @RequestParam LicenseKind license) {
        return service.quote(slug, label, license);
    }

    @PostMapping("/me/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseView purchase(@Valid @RequestBody PurchaseRequest request) {
        return service.purchase(request.slug(), request.versionLabel(), request.license(), request.accepted());
    }

    @GetMapping("/me/purchases")
    public List<PurchaseView> mine() {
        return service.listMine();
    }

    /** Genere (ou regenere) un lien pour un achat, sans repayer. */
    @PostMapping("/purchases/{id}/download-link")
    public LinkView downloadLink(@PathVariable Long id) {
        Link link = service.createLink(id);
        return new LinkView("/api/public/purchase-downloads/" + link.token(), link.expiresAt(), link.maxDownloads());
    }

    @GetMapping("/public/purchase-downloads/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token, HttpServletRequest request) {
        Archive archive = service.open(token, request.getRemoteAddr());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archive.filename()).build().toString())
            .body(new FileSystemResource(archive.file()));
    }
}
