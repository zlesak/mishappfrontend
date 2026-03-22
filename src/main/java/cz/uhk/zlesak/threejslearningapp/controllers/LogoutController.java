package cz.uhk.zlesak.threejslearningapp.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Controller that handles user logout by invalidating the session and redirecting to Keycloak's logout endpoint.
 * Supports both GET and POST requests to the /custom-logout endpoint.
 * Implemented as the nginx routing with keycloak default logout strategy is not able to be reconfigured to custom endpoint
 */
@RestController
public class LogoutController {

    @Value("${KEYCLOAK_EXTERNAL_URL:http://mock-oidc:8080/auth}")
    private String externalKeycloakUrl;

    @Value("${EXTERNAL_GATEWAY_URL:http://localhost:8081}")
    private String externalGatewayUrl;

    @Value("${KEYCLOAK_REALM:mock-realm}")
    private String keycloakRealm;

    /**
     * Handles POST requests to /custom-logout by performing logout and redirecting to Keycloak's logout endpoint.
     * @param request the HttpServletRequest object containing the logout request
     * @param response the HttpServletResponse object to send the logout response
     * @throws IOException if an input or output error occurs during logout processing
     */
    @PostMapping("/custom-logout")
    public void logoutPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        performLogout(request, response);
    }

    /**
     * Handles GET requests to /custom-logout by performing logout and redirecting to Keycloak's logout endpoint.
     * @param request the HttpServletRequest object containing the logout request
     * @param response the HttpServletResponse object to send the logout response
     * @throws IOException if an input or output error occurs during logout processing
     */
    @GetMapping("/custom-logout") 
    public void logoutGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        performLogout(request, response);
    }

    /**
     * Performs the logout by invalidating the user's session and redirecting to Keycloak's logout endpoint with the ID token hint and post-logout redirect URI.
     * @param request the HttpServletRequest object containing the logout request
     * @param response the HttpServletResponse object to send the logout response
     * @throws IOException if an input or output error occurs during logout processing
     */
    private void performLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String idToken = "";
        
        if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
            Object principal = oauth2Token.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                idToken = oidcUser.getIdToken().getTokenValue();
            }
        }

        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, auth);

        String logoutUrl = UriComponentsBuilder.fromUriString(externalKeycloakUrl)
                .pathSegment("realms", keycloakRealm, "protocol", "openid-connect", "logout")
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", externalGatewayUrl)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(logoutUrl);
    }
}