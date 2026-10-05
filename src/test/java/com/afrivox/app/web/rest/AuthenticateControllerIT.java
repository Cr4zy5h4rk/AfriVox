package com.afrivox.app.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afrivox.app.IntegrationTest;
import com.afrivox.app.domain.User;
import com.afrivox.app.repository.UserRepository;
import com.afrivox.app.web.rest.vm.LoginVM;
import jakarta.servlet.http.Cookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AuthenticateController} REST controller.
 */
@AutoConfigureMockMvc
@IntegrationTest
class AuthenticateControllerIT {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void testAuthorize() throws Exception {
        User user = new User();
        user.setLogin("user-jwt-controller");
        user.setEmail("user-jwt-controller@example.com");
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode("test"));

        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller");
        login.setPassword("test");
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
            .andExpect(header().string("Authorization", not(nullValue())))
            .andExpect(header().string("Authorization", not(is(emptyString()))));
    }

    @Test
    @Transactional
    void testAuthorizeWithRememberMe() throws Exception {
        User user = new User();
        user.setLogin("user-jwt-controller-remember-me");
        user.setEmail("user-jwt-controller-remember-me@example.com");
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode("test"));

        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller-remember-me");
        login.setPassword("test");
        login.setRememberMe(true);
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
            .andExpect(header().string("Authorization", not(nullValue())))
            .andExpect(header().string("Authorization", not(is(emptyString()))));
    }

    @Test
    void testAuthorizeFails() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.id_token").doesNotExist())
            .andExpect(header().doesNotExist("Authorization"));
    }

    private static final Pattern REFRESH_COOKIE_VALUE = Pattern.compile("refresh_token=([^;]+)");

    private User saveActiveUser(String login) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode("test"));
        return userRepository.saveAndFlush(user);
    }

    private MvcResult login(String login, String password, String ip) throws Exception {
        LoginVM vm = new LoginVM();
        vm.setUsername(login);
        vm.setPassword(password);
        return mockMvc
            .perform(
                post("/api/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(vm))
                    .with(request -> {
                        request.setRemoteAddr(ip);
                        return request;
                    })
            )
            .andReturn();
    }

    private static String refreshCookieValue(MvcResult result) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(setCookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    @Transactional
    void testLoginSetsHardenedRefreshCookie() throws Exception {
        saveActiveUser("user-refresh-cookie");

        MvcResult result = login("user-refresh-cookie", "test", "10.0.0.1");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Strict").contains("Path=/api/authenticate");
        // le jeton de rafraichissement ne doit jamais apparaitre dans le corps de la reponse
        assertThat(result.getResponse().getContentAsString()).doesNotContain(refreshCookieValue(result));
    }

    @Test
    @Transactional
    void testRefreshReturnsNewAccessTokenAndRotatesCookie() throws Exception {
        saveActiveUser("user-refresh-rotate");
        String first = refreshCookieValue(login("user-refresh-rotate", "test", "10.0.0.2"));

        MvcResult refreshed = mockMvc
            .perform(post("/api/authenticate/refresh").cookie(new Cookie("refresh_token", first)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
            .andReturn();

        assertThat(refreshCookieValue(refreshed)).isNotEqualTo(first);
    }

    @Test
    @Transactional
    void testRefreshWithUsedTokenIsRefusedAndKillsTheSession() throws Exception {
        saveActiveUser("user-refresh-replay");
        String first = refreshCookieValue(login("user-refresh-replay", "test", "10.0.0.3"));
        String second = refreshCookieValue(
            mockMvc
                .perform(post("/api/authenticate/refresh").cookie(new Cookie("refresh_token", first)))
                .andExpect(status().isOk())
                .andReturn()
        );

        // rejeu de l'ancien jeton : refuse...
        mockMvc.perform(post("/api/authenticate/refresh").cookie(new Cookie("refresh_token", first))).andExpect(status().isUnauthorized());
        // ...et le jeton legitime plus recent est revoque avec lui
        mockMvc.perform(post("/api/authenticate/refresh").cookie(new Cookie("refresh_token", second))).andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/authenticate/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void testLogoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        saveActiveUser("user-logout");
        String cookie = refreshCookieValue(login("user-logout", "test", "10.0.0.4"));

        mockMvc
            .perform(post("/api/authenticate/logout").cookie(new Cookie("refresh_token", cookie)))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(post("/api/authenticate/refresh").cookie(new Cookie("refresh_token", cookie))).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void testLoginIsBlockedAfterTooManyFailures() throws Exception {
        saveActiveUser("user-throttled");
        for (int i = 0; i < 5; i++) {
            assertThat(login("user-throttled", "bad-password", "10.9.9.9").getResponse().getStatus()).isEqualTo(401);
        }

        // 6e tentative : meme avec le bon mot de passe, l'acces est refuse temporairement
        MvcResult blocked = login("user-throttled", "test", "10.9.9.9");

        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(blocked.getResponse().getHeader("Retry-After")).isNotNull();
        // une autre adresse n'est pas affectee
        assertThat(login("user-throttled", "test", "10.9.9.10").getResponse().getStatus()).isEqualTo(200);
    }
}
