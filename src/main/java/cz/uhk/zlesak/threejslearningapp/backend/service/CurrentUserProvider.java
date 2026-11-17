package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated user for the embedded backend.
 * The UI authenticates through the OIDC login flow, so the principal is normally an {@link OidcUser};
 * a {@link Jwt} principal is also accepted so the same code works behind a bearer-token filter chain.
 */
@Component
public class CurrentUserProvider {

    /**
     * @return stable id of the current user, taken from the {@code sub} claim.
     * @throws BackendException.Forbidden when nobody is authenticated.
     */
    public String requireUserId() {
        String userId = currentUserId();
        if (userId == null) {
            throw new BackendException.Forbidden("Pro tuto akci je nutné být přihlášen.");
        }
        return userId;
    }

    /**
     * @return id of the current user, or {@code null} when the request is anonymous.
     */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        if ("anonymousUser".equals(principal)) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Resolves the name to record on things the current user creates.
     * Prefers the username over the {@code sub} claim, because the id is meaningless to anyone
     * reading a listing.
     *
     * @return the current user's username, or {@code null} when the request is anonymous.
     */
    public String currentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || currentUserId() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return firstNonBlank(oidcUser.getPreferredUsername(), oidcUser.getFullName(), authentication.getName());
        }
        if (principal instanceof Jwt jwt) {
            return firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("name"),
                    authentication.getName());
        }
        return authentication.getName();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * @param role role name without the {@code ROLE_} prefix.
     * @return whether the current user holds the role.
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    /**
     * Lets a change through only for the author of the thing being changed, or for a teacher.
     *
     * <p>Whether this ever refuses anybody depends on how the realm is set up. With the bundled
     * demo realm every account that can author content is a teacher, so it does not; a realm that
     * hands out the authoring roles separately — which a deployment is free to do, and which
     * {@code infra/.env.production.example} tells the operator to build for themselves — is
     * exactly the case this guards. It is applied to every write that changes or removes existing
     * content, so the rule does not depend on which screen the request came from.
     *
     * @param creatorId id of the account that created the thing, may be {@code null} for content
     *                  that predates authorship being recorded.
     * @param message   what to tell the user when they may not do this.
     * @throws BackendException.Forbidden when the current user is neither the author nor a teacher.
     */
    public void requireAuthorOrTeacher(String creatorId, String message) {
        String userId = requireUserId();
        if (creatorId != null && !creatorId.equals(userId) && !hasRole("TEACHER")) {
            throw new BackendException.Forbidden(message);
        }
    }
}
