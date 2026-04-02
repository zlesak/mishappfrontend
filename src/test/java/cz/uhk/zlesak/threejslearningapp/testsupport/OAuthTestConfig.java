package cz.uhk.zlesak.threejslearningapp.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.concurrent.Executor;

@TestConfiguration
public class OAuthTestConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("dev-client")
                .clientSecret("secret")
                .clientName("Keycloak")
                .scope("openid", "profile", "email")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://localhost/mock-auth")
                .tokenUri("http://localhost/mock-token")
                .jwkSetUri("http://localhost/mock-jwks")
                .userInfoUri("http://localhost/mock-userinfo")
                .userNameAttributeName("preferred_username")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean(name = "modelIoExecutor")
    @Primary
    Executor modelIoExecutor() {
        return Runnable::run;
    }
}
