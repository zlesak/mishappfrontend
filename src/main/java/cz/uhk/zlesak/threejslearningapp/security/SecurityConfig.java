package cz.uhk.zlesak.threejslearningapp.security;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Security configuration for the application.
 * Sets up HTTP security with OAuth2/OIDC authentication via Keycloak.
 * Extracts roles from Keycloak token claims and maps them to Spring Security authorities.
 */
@Slf4j
@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests with OAuth2 login.
     *
     * @param http                         the HttpSecurity object to configure
     * @param clientRegistrationRepository the repository for client registrations
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                configurer.oauth2LoginPage("/oauth2/authorization/keycloak?prompt=login")
        );

        http.oauth2Login(oauth2 ->
                oauth2.userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.oidcUserService()))
        );

        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");

        http.logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));

        http.authorizeHttpRequests(auth ->
            auth.requestMatchers("/img/**").permitAll()
        );

        return http.build();
    }

    /**
     * Configures the OIDC user service to extract roles from Keycloak token.
     * Maps Keycloak roles to Spring Security authorities with ROLE_ prefix.
     *
     * @return the OAuth2UserService for OIDC
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Collection<SimpleGrantedAuthority> mappedAuthorities = new ArrayList<>();

            Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
            }

            Map<String, Object> resourceAccess = oidcUser.getClaim("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, clientRoles) -> {
                    if (clientRoles instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> clientRolesMap = (Map<String, Object>) clientRoles;
                        if (clientRolesMap.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) clientRolesMap.get("roles");
                            roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                        }
                    }
                });
            }

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    /**
     * Configures the OAuth2AuthorizedClientManager bean for managing authorized clients.
     * Enables handling of refresh tokens.
     *
     * @param clientRegistrationRepository the repository for client registrations
     * @param authorizedClientRepository   the repository for authorized clients
     * @return the configured OAuth2AuthorizedClientManager
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
