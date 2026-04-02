package cz.uhk.zlesak.threejslearningapp.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutControllerWebMvcTest {
    private final MockMvc mockMvc;

    LogoutControllerWebMvcTest() {
        LogoutController controller = new LogoutController();
        ReflectionTestUtils.setField(controller, "externalKeycloakUrl", "http://mock-oidc:8080/auth");
        ReflectionTestUtils.setField(controller, "externalGatewayUrl", "https://mish.local/app/logout?source=ui&next=/welcome page");
        ReflectionTestUtils.setField(controller, "keycloakRealm", "my-realm");
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutGet_shouldRedirectToConfiguredOidcEndpoint() throws Exception {
        mockMvc.perform(get("/custom-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedLogoutUrl()));
    }

    @Test
    void logoutPost_shouldRedirectToConfiguredOidcEndpoint() throws Exception {
        mockMvc.perform(post("/custom-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedLogoutUrl()));
    }

    @Test
    void logoutGet_redirectShouldContainConfiguredRealm() throws Exception {
        mockMvc.perform(get("/custom-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedLogoutUrl()));

        String url = expectedLogoutUrl();
        assertTrue(url.contains("my-realm"));
    }

    @Test
    void logoutPost_calledAgainAfterFirst_shouldAlwaysRedirect() throws Exception {
        mockMvc.perform(post("/custom-logout"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/custom-logout"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void logoutGet_redirectUrlShouldContainPostLogoutRedirectUri() throws Exception {
        String redirectUrl = mockMvc.perform(get("/custom-logout"))
                .andReturn().getResponse().getRedirectedUrl();

        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("post_logout_redirect_uri"));
    }

    private String expectedLogoutUrl() {
        return UriComponentsBuilder.fromUriString("http://mock-oidc:8080/auth")
                .pathSegment("realms", "my-realm", "protocol", "openid-connect", "logout")
                .queryParam("id_token_hint", "")
                .queryParam("post_logout_redirect_uri", "https://mish.local/app/logout?source=ui&next=/welcome page")
                .build()
                .encode()
                .toUriString();
    }
}

