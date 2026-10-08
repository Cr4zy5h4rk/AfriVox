package com.afrivox.app.web.rest;

import com.afrivox.app.service.AdminLanguageService;
import com.afrivox.app.service.AdminLanguageService.AdminLanguageView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vue d'ensemble des langues pour l'admin ({@code /api/admin/**} est réservé à ROLE_ADMIN, voir SecurityConfiguration). */
@RestController
@RequestMapping("/api/admin/languages")
public class AdminLanguageResource {

    private final AdminLanguageService service;

    public AdminLanguageResource(AdminLanguageService service) {
        this.service = service;
    }

    @GetMapping("")
    public List<AdminLanguageView> overview() {
        return service.overview();
    }
}
