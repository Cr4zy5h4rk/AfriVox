package com.afrivox.app.web.rest;

import com.afrivox.app.service.DatasetDownloadService;
import com.afrivox.app.service.DatasetDownloadService.Archive;
import com.afrivox.app.service.DatasetDownloadService.Link;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Telechargement en deux temps : un utilisateur connecte demande un lien ({@code POST /api/me/downloads/...}),
 * puis le navigateur le suit sans en-tete d'authentification ({@code GET /api/public/downloads/{token}}) :
 * c'est la signature du lien qui l'autorise.
 */
@RestController
@RequestMapping("/api")
public class DatasetDownloadResource {

    public record LinkView(String url, Instant expiresAt) {}

    private final DatasetDownloadService service;

    public DatasetDownloadResource(DatasetDownloadService service) {
        this.service = service;
    }

    @PostMapping("/me/downloads/{slug}/{label}/link")
    public LinkView link(@PathVariable String slug, @PathVariable String label) {
        Link link = service.createLink(slug, label);
        return new LinkView("/api/public/downloads/" + link.token(), link.expiresAt());
    }

    @GetMapping("/public/downloads/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token, HttpServletRequest request) {
        Archive archive = service.open(token, request.getRemoteAddr());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archive.filename()).build().toString())
            .body(new FileSystemResource(archive.file()));
    }
}
