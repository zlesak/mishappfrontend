package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LoginButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LogoutButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ThemeModeToggleButton;
import cz.uhk.zlesak.threejslearningapp.components.common.AvatarItem;
import cz.uhk.zlesak.threejslearningapp.components.common.MenuItemInfo;
import cz.uhk.zlesak.threejslearningapp.components.notifications.CookiesNotification;
import cz.uhk.zlesak.threejslearningapp.views.administration.AdministrationView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.ArrayList;
import java.util.List;

/**
 * MainLayout class - The main layout of the application that includes the header with navigation and user authentication controls.
 */
@AnonymousAllowed
@Scope("prototype")
@Layout
@Slf4j
public class MainLayout extends AppLayout {
    /**
     * Constructor for MainLayout.
     */
    public MainLayout() {
        addToNavbar(createHeaderContent());
    }

    /**
     * Creates the header content including navigation and user authentication controls.
     *
     * @return the header component
     */
    private Component createHeaderContent() {
        AuthenticationContext authenticationContext = VaadinService.getCurrent().getInstantiator().getOrCreate(AuthenticationContext.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        /// Header item component wrapper
        Header header = new Header();
        header.addClassNames(BoxSizing.BORDER, Display.FLEX, FlexDirection.COLUMN, Width.FULL);

        /// Layout item in form of div component
        Div layout = new Div();
        layout.addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE);

        /// Navigation item component
        Nav nav = new Nav();
        nav.addClassNames(Display.FLEX, Overflow.AUTO, Padding.Horizontal.MEDIUM, Padding.Vertical.XSMALL, Margin.End.AUTO, AlignItems.START);
        /// UL for items of the navigation
        UnorderedList list = new UnorderedList();
        list.addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE);
        nav.add(list);
        layout.add(nav);
        /// For loop for inserting the menu items into the UL wrapper
        for (MenuItemInfo menuItem : createMenuItems(authentication)) {
            list.add(menuItem);
        }

        /// Light or dark mode toggle switch with default of light mode
        ThemeModeToggleButton buttonPrimary = new ThemeModeToggleButton();
        layout.add(buttonPrimary);


        UI.getCurrent().getPage().executeJs(
                "const match = document.cookie.match('(^|;) ?cookieConsent=([^;]*)(;|$)'); return match ? match[2] : null;"
        ).then(String.class, value -> {
            if (!"accepted".equals(value)) {
                showCookieNotification();
            }
        });

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getPrincipal() instanceof OidcUser oidcUser ?( oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername()) : authentication.getName();
            layout.add(new AvatarItem(username, getUserRoleName(authentication), new LogoutButton(authenticationContext)));
        } else {
            LoginButton loginButton = new LoginButton();
            layout.add(loginButton);
        }
        header.add(layout);
        return header;
    }

    /**
     * Creates the menu items based on the user's authentication and roles.
     *
     * @param authentication the authentication object of the current user
     * @return the list of menu items
     */
    private List<MenuItemInfo> createMenuItems(Authentication authentication) {
        List<MenuItemInfo> menuItems = commonMenuItemsForLoggedUsers();
        if (authentication != null && authentication.getAuthorities() != null) {
            if (authentication.getAuthorities().stream().anyMatch(auth ->
                    "ROLE_ADMIN".equals(auth.getAuthority()) || "ROLE_TEACHER".equals(auth.getAuthority()))) {
                menuItems.add(new MenuItemInfo("Administrační centrum", VaadinIcon.COG.create(), AdministrationView.class));
            }
        }
        return menuItems;
    }

    /**
     * Creates common menu items for logged-in users.
     *
     * @return the list of common menu items
     */
    private List<MenuItemInfo> commonMenuItemsForLoggedUsers() {
        Image logo = new Image("/icons/MISH_icon.ico", "MISH icon");
        logo.setWidth("24px");
        logo.setHeight("24px");
        return new ArrayList<>(List.of(
                new MenuItemInfo("MISH - Úvod", logo, MainPageView.class),
                new MenuItemInfo("Kapitoly", VaadinIcon.OPEN_BOOK.create(), ChapterListingView.class),
                new MenuItemInfo("Modely", VaadinIcon.CUBES.create(), ModelListingView.class),
                new MenuItemInfo("Kvízy", VaadinIcon.LIGHTBULB.create(), QuizListingView.class)
        ));
    }

    /**
     * Gets the user role name based on the authentication object.
     *
     * @param authentication the authentication object of the current user
     * @return the user role name
     */
    private String getUserRoleName(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                switch (authority.getAuthority()) {
                    case "ROLE_ADMIN":
                        return "Administrátor";
                    case "ROLE_TEACHER":
                        return "Učitel";
                    case "ROLE_STUDENT":
                        return "Student";
                }
            }
            log.warn("User {} has no recognized roles. Authorities: {}",
                    authentication.getName(),
                    authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            return "Uživatel";
        }
        log.warn("Authentication is null or has no authorities");
        return "Uživatel";
    }

    /**
     * Shows the cookie notification to the user.
     *
     */
    private void showCookieNotification() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();
    }
}
