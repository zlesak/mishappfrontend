package cz.uhk.zlesak.threejslearningapp.security;

import com.vaadin.flow.server.VaadinService;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue")
class SecuritySupportTest {
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        VaadinService.setCurrent(null);
    }

    @Test
    void accessTokenProviderShouldReturnNullWithoutOAuthAuthentication() {
        AccessTokenProvider provider = new AccessTokenProvider();

        assertNull(provider.getValidAccessToken());
    }

    @Test
    void accessTokenProviderShouldResolveTokenForOAuthAuthentication() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        registerBean(OAuth2AuthorizedClientManager.class, manager);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-123",
                Instant.now().minusSeconds(10),
                Instant.now().plusSeconds(3600),
                java.util.Set.of("openid")
        );
        when(client.getAccessToken()).thenReturn(token);
        when(manager.authorize(any())).thenReturn(client);

        AccessTokenProvider provider = new AccessTokenProvider();

        assertEquals("token-123", provider.getValidAccessToken());
    }

    @Test
    void securityConfigShouldCreateAuthorizedClientManagerBean() {
        SecurityConfig config = new SecurityConfig();

        var manager = config.authorizedClientManager(mock(org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class),
                mock(OAuth2AuthorizedClientService.class));

        assertNotNull(manager);
    }

    @Test
    void accessTokenProviderShouldFallBackToClientServiceWhenManagerReturnsNullClient() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        registerBothBeans(OAuth2AuthorizedClientManager.class, manager, OAuth2AuthorizedClientService.class, clientService);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        when(authentication.getName()).thenReturn("user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(manager.authorize(any())).thenReturn(null);

        OAuth2AuthorizedClient storedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "fallback-token",
                Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));
        when(storedClient.getAccessToken()).thenReturn(token);
        when(clientService.loadAuthorizedClient("keycloak", "user")).thenReturn(storedClient);

        AccessTokenProvider provider = new AccessTokenProvider();

        assertEquals("fallback-token", provider.getValidAccessToken());
    }

    @Test
    void accessTokenProviderShouldReturnNullWhenClientServiceReturnsNull() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        registerBothBeans(OAuth2AuthorizedClientManager.class, manager, OAuth2AuthorizedClientService.class, clientService);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        when(authentication.getName()).thenReturn("user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(manager.authorize(any())).thenReturn(null);
        when(clientService.loadAuthorizedClient("keycloak", "user")).thenReturn(null);

        AccessTokenProvider provider = new AccessTokenProvider();

        assertNull(provider.getValidAccessToken());
    }

    @Test
    void accessTokenProviderShouldReturnNullWhenClientServiceThrows() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        registerBothBeans(OAuth2AuthorizedClientManager.class, manager, OAuth2AuthorizedClientService.class, clientService);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        when(authentication.getName()).thenReturn("user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(manager.authorize(any())).thenReturn(null);
        when(clientService.loadAuthorizedClient(any(), any())).thenThrow(new RuntimeException("service unavailable"));

        AccessTokenProvider provider = new AccessTokenProvider();

        assertNull(provider.getValidAccessToken());
    }

    @Test
    void accessTokenProviderShouldReturnNullWhenClientHasNullAccessToken() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        registerBothBeans(OAuth2AuthorizedClientManager.class, manager, OAuth2AuthorizedClientService.class, clientService);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("keycloak");
        when(authentication.getName()).thenReturn("user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        OAuth2AuthorizedClient clientWithNullToken = mock(OAuth2AuthorizedClient.class);
        when(clientWithNullToken.getAccessToken()).thenReturn(null);
        when(manager.authorize(any())).thenReturn(clientWithNullToken);
        when(clientService.loadAuthorizedClient(any(), any())).thenReturn(null);

        AccessTokenProvider provider = new AccessTokenProvider();

        assertNull(provider.getValidAccessToken());
    }

    @Test
    void securityConfig_authorizedClientManagerShouldBeNonNullWithRealMocks() {
        SecurityConfig config = new SecurityConfig();

        var manager = config.authorizedClientManager(
                mock(org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class),
                mock(OAuth2AuthorizedClientService.class));

        assertNotNull(manager);
    }

    private <T> void registerBean(Class<T> type, T bean) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(type, () -> bean);
        context.refresh();
        SpringContextUtils.setContext(context);
    }

    @SuppressWarnings("SameParameterValue")
    private <A, B> void registerBothBeans(Class<A> typeA, A beanA, Class<B> typeB, B beanB) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(typeA, () -> beanA);
        context.registerBean(typeB, () -> beanB);
        context.refresh();
        SpringContextUtils.setContext(context);
    }
}

