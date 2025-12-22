package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LoginButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LogoutButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ThemeModeToggleButton;
import cz.uhk.zlesak.threejslearningapp.components.common.MenuItemInfo;
import cz.uhk.zlesak.threejslearningapp.components.common.AvatarItem;
import cz.uhk.zlesak.threejslearningapp.components.notifications.CookiesNotification;
import cz.uhk.zlesak.threejslearningapp.views.administration.AdministrationView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@AnonymousAllowed
@Scope("prototype")
@Layout
@Slf4j
public class MainLayout extends AppLayout {
    public MainLayout() {
        addToNavbar(createHeaderContent());
    }

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

        /// Login user basic information avatar item TODO in phase where ogin is implemented via BE API react to these changes and change accordingly to use the appropriate APIs
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getName();
            AvatarItem avatarItem = new AvatarItem(username, getUserRoleName(authentication), new Avatar(username));
            layout.add(avatarItem);
            LogoutButton logoutButton = new LogoutButton(authenticationContext);
            layout.add(logoutButton);
        } else {
            LoginButton loginButton = new LoginButton();
            layout.add(loginButton);
        }
        header.add(layout);
        return header;
    }

    /// Function for creating menu items to appear at the top navigation
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

    private List<MenuItemInfo> commonMenuItemsForLoggedUsers() {
        return new ArrayList<>(List.of(
                new MenuItemInfo("MISH APP", VaadinIcon.HOME.create(), MainPageView.class),
                new MenuItemInfo("Kapitoly", VaadinIcon.OPEN_BOOK.create(), ChapterListingView.class),
                new MenuItemInfo("Modely", VaadinIcon.CUBES.create(), ModelListingView.class),
                new MenuItemInfo("Kvízy", VaadinIcon.LIGHTBULB.create(), QuizListingView.class)
        ));
    }

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
            throw new ApplicationContextException("Role uživatele nejsou mezi známými rolemi systému" + authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        }
        throw new ApplicationContextException("Neplatné role uživatele, authentication není nastavena!");
    }


    private void showCookieNotification() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();
    }
}
