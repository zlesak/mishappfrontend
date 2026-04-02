package cz.uhk.zlesak.threejslearningapp.security;

import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    private SecurityConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new SecurityConfig();
        Field field = SecurityConfig.class.getDeclaredField("externalGatewayUrl");
        field.setAccessible(true);
        field.set(config, "http://localhost:8081");
    }

    private OidcIdToken buildToken(Map<String, Object> extraClaims) {
        OidcIdToken.Builder builder = OidcIdToken.withTokenValue("test-token")
                .claim("sub", "user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        extraClaims.forEach(builder::claim);
        return builder.build();
    }

    private OidcUser runLambdaWithToken(OidcIdToken idToken) {
        DefaultOidcUser fakeUser = new DefaultOidcUser(List.of(), idToken);
        OidcUserRequest mockRequest = mock(OidcUserRequest.class);

        try (MockedConstruction<OidcUserService> mocked = mockConstruction(OidcUserService.class,
                (mock, ctx) -> when(mock.loadUser(any())).thenReturn(fakeUser))) {
            OAuth2UserService<OidcUserRequest, OidcUser> service = config.oidcUserService();
            return service.loadUser(mockRequest);
        }
    }

    private Set<String> authorityNames(OidcUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void oidcUserService_withRealmAccessRoles_shouldMapToAuthorities() {
        OidcIdToken token = buildToken(Map.of(
                "realm_access", Map.of("roles", List.of("teacher"))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).contains("ROLE_TEACHER");
    }

    @Test
    void oidcUserService_withNullRealmAccess_shouldHaveNoRealmRoles() {
        OidcIdToken token = buildToken(Map.of());

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).doesNotContain("ROLE_TEACHER");
    }

    @Test
    void oidcUserService_withResourceAccessRoles_shouldMapToAuthorities() {
        OidcIdToken token = buildToken(Map.of(
                "resource_access", Map.of("mish", Map.of("roles", List.of("admin")))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).contains("ROLE_ADMIN");
    }

    @Test
    void oidcUserService_withNonMapResourceAccess_shouldBeSkipped() {
        OidcIdToken token = buildToken(Map.of(
                "resource_access", Map.of("mish", "not-a-map")
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).isEmpty();
    }

    @Test
    void oidcUserService_withResourceAccessMapWithoutRolesKey_shouldBeSkipped() {
        OidcIdToken token = buildToken(Map.of(
                "resource_access", Map.of("mish", Map.of("scopes", List.of("read")))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).isEmpty();
    }

    @Test
    void oidcUserService_withBothRealmAndResourceRoles_shouldCombineAuthorities() {
        OidcIdToken token = buildToken(Map.of(
                "realm_access", Map.of("roles", List.of("teacher")),
                "resource_access", Map.of("mish", Map.of("roles", List.of("admin")))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).containsExactlyInAnyOrder("ROLE_TEACHER", "ROLE_ADMIN");
    }

    @Test
    void oidcUserService_withEmptyRoleLists_shouldHaveNoAuthorities() {
        OidcIdToken token = buildToken(Map.of(
                "realm_access", Map.of("roles", List.of()),
                "resource_access", Map.of("mish", Map.of("roles", List.of()))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).isEmpty();
    }

    @Test
    void oidcUserService_withNullResourceAccess_shouldHaveNoResourceRoles() {
        OidcIdToken token = buildToken(Map.of(
                "realm_access", Map.of("roles", List.of("teacher"))
        ));

        OidcUser result = runLambdaWithToken(token);

        assertThat(authorityNames(result)).containsExactly("ROLE_TEACHER");
    }

    @Test
    void authorizedClientManager_shouldBuildWithAuthCodeAndRefreshToken() {
        ClientRegistrationRepository repo = mock(ClientRegistrationRepository.class);
        OAuth2AuthorizedClientService service = mock(OAuth2AuthorizedClientService.class);

        OAuth2AuthorizedClientManager manager = config.authorizedClientManager(repo, service);

        assertThat(manager).isNotNull();
        assertThat(manager).isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    @ActiveProfiles("test")
    @Import(OAuthTestConfig.class)
    class HttpFilterChainIntegrationTest {

        @Autowired
        WebApplicationContext context;

        MockMvc mockMvc;

        @BeforeEach
        void setUpMockMvc() {
            mockMvc = MockMvcBuilders.webAppContextSetup(context)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }

        @Test
        void unauthenticatedRequestToProtectedRoute_shouldRedirectToOAuth2Login() throws Exception {
            mockMvc.perform(get("/administration"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(result ->
                            assertThat(result.getResponse().getRedirectedUrl()).contains("/oauth2/authorization/"));
        }

        @Test
        void customLogoutShouldBeAccessible() throws Exception {
            mockMvc.perform(get("/custom-logout"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertThat(status).isNotIn(401, 403);
                    });
        }
    }
}
