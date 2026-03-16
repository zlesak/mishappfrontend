package cz.uhk.zlesak.threejslearningapp.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutControllerWebMvcTest {
    private final MockMvc mockMvc;

    LogoutControllerWebMvcTest() {
        LogoutController controller = new LogoutController();
        ReflectionTestUtils.setField(controller, "externalKeycloakUrl", "http://mock-oidc:8080/auth");
        ReflectionTestUtils.setField(controller, "externalGatewayUrl", "http://localhost:8081");
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
                .andExpect(redirectedUrl(
                        "http://mock-oidc:8080/auth/realms/mock-realm/protocol/openid-connect/logout?id_token_hint=&post_logout_redirect_uri=http://localhost:8081"
                ));
    }

    @Test
    void logoutPost_shouldRedirectToConfiguredOidcEndpoint() throws Exception {
        mockMvc.perform(post("/custom-logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "http://mock-oidc:8080/auth/realms/mock-realm/protocol/openid-connect/logout?id_token_hint=&post_logout_redirect_uri=http://localhost:8081"
                ));
    }
}
