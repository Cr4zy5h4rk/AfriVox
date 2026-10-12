package com.afrivox.app.web.rest;

import com.afrivox.app.domain.enumeration.ApiKeyScope;
import com.afrivox.app.service.DeveloperApiKeyService;
import java.util.List;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mes clés d'API. Réservé aux développeurs et organisations, et uniquement avec une session de navigateur : une clé
 * ne peut pas en créer ni en révoquer d'autres (elle n'est pas dans {@code ApiKeyAccess}).
 */
@RestController
@RequestMapping("/api/me/api-keys")
public class MyApiKeyResource {

    /** Corps de création : {@code expiresInDays} absent = la clé n'expire pas. */
    public record CreateRequest(String name, Set<ApiKeyScope> scopes, Integer expiresInDays) {}

    private final DeveloperApiKeyService service;

    public MyApiKeyResource(DeveloperApiKeyService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<DeveloperApiKeyService.KeyView> mine() {
        return service.listMine();
    }

    /** La clé complète est dans cette réponse et nulle part ailleurs : jamais mise en cache. */
    @PostMapping("")
    public ResponseEntity<DeveloperApiKeyService.CreatedKey> create(@RequestBody CreateRequest request) {
        DeveloperApiKeyService.CreatedKey created = service.create(request.name(), request.scopes(), request.expiresInDays());
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        service.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
