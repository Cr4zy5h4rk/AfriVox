package com.afrivox.app.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afrivox.app.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@link ConsentResource} REST controller.
 */
@AutoConfigureMockMvc
@IntegrationTest
class ConsentResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void currentConsentIsPublic() throws Exception {
        // pas d'utilisateur authentifie : la version doit etre lisible avant l'inscription
        mockMvc
            .perform(get("/api/consent/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(AccountResourceIT.CONSENT_VERSION));
    }
}
