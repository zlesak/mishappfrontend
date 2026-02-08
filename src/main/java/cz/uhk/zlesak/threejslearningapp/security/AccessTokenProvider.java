package cz.uhk.zlesak.threejslearningapp.security;

import com.vaadin.flow.spring.annotation.UIScope;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * AccessTokenProvider is a Spring component that provides methods to retrieve
 * a valid OAuth2 access token for the currently authenticated user.
 * Used in Three.js to access protected resources.
 */
@Component
@UIScope
public class AccessTokenProvider {
    /**
     * Retrieves a valid OAuth2 access token for the currently authenticated user.
     *
     * @return the access token as a String, or null if no valid token is found.
     */
    public String getValidAccessToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof OAuth2AuthenticationToken oauth) {
            OAuth2AuthorizedClientManager clientManager =
                    SpringContextUtils.getBean(OAuth2AuthorizedClientManager.class);

            OAuth2AuthorizeRequest.Builder builder = OAuth2AuthorizeRequest
                    .withClientRegistrationId(oauth.getAuthorizedClientRegistrationId())
                    .principal(oauth);

            if (VaadinService.getCurrentRequest() instanceof VaadinServletRequest vaadinRequest) {
                builder.attribute(HttpServletRequest.class.getName(), vaadinRequest.getHttpServletRequest());
            }
            if (VaadinService.getCurrentResponse() instanceof VaadinServletResponse vaadinResponse) {
                builder.attribute(HttpServletResponse.class.getName(), vaadinResponse.getHttpServletResponse());
            }

            OAuth2AuthorizeRequest authorizeRequest = builder.build();

            OAuth2AuthorizedClient client = clientManager.authorize(authorizeRequest);

            if (client != null && client.getAccessToken() != null) {
                return client.getAccessToken().getTokenValue();
            }
        }
        return null;
    }
}
