package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LoginButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ThemeModeToggleButton;
import cz.uhk.zlesak.threejslearningapp.components.listItems.AvatarListItem;
import cz.uhk.zlesak.threejslearningapp.components.listItems.MenuListItem;
import cz.uhk.zlesak.threejslearningapp.components.notifications.CookiesNotification;
import cz.uhk.zlesak.threejslearningapp.views.administration.AdministrationView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import cz.uhk.zlesak.threejslearningapp.views.documentation.DocumentationView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

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
        addClassName("app-shell");
        addToNavbar(createHeaderContent());
    }

    /**
     * Creates the header content including navigation and user authentication controls.
     *
     * @return the header component
     */
    private Component createHeaderContent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        /// Header item component wrapper
        Header header = new Header();
        header.addClassName("app-shell-header");
        header.addClassNames(BoxSizing.BORDER, Display.FLEX, FlexDirection.COLUMN, Width.FULL);

        /// Layout item in form of div component
        Div layout = new Div();
        layout.addClassName("app-shell-topbar");
        layout.addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE);

        ThemeModeToggleButton.applyThemeFromCookie(UI.getCurrent());

        layout.add(createMobileNavigationMenu());
        Span brand = new Span("MISH");
        brand.addClassNames(
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.Start.XSMALL
        );
        brand.addClassName("app-shell-brand");
        layout.add(brand);
        layout.add(createDesktopNavigation());
        layout.getStyle().set("flex-wrap", "nowrap");
        layout.getStyle().set("min-width", "0");
        brand.getStyle().set("white-space", "nowrap");

        Div spacer = new Div();
        spacer.getStyle().set("flex", "1 1 auto");
        layout.add(spacer);


        UI.getCurrent().getPage().executeJs(
                "const match = document.cookie.match('(^|;) ?cookieConsent=([^;]*)(;|$)'); return match ? match[2] : null;"
        ).then(String.class, value -> {
            if (!"accepted".equals(value)) {
                showCookieNotification();
            }
        });

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getPrincipal() instanceof OidcUser oidcUser ? (oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername()) : authentication.getName();
            Component userMenu = createUserMenu(authentication, username);
            userMenu.getElement().getClassList().add("app-shell-right-item");
            layout.add(userMenu);
        } else {
            LoginButton loginButton = new LoginButton();
            loginButton.addClassName("app-login-button");
            loginButton.getElement().getClassList().add("app-shell-right-item");
            layout.add(loginButton);
        }
        header.add(layout);
        return header;
    }

    private Component createMobileNavigationMenu() {
        MenuBar navMenuBar = new MenuBar();
        navMenuBar.addClassName("app-nav-menu");
        Icon menuIcon = VaadinIcon.MENU.create();
        menuIcon.setSize("1.1rem");

        MenuItem root = navMenuBar.addItem(menuIcon);
        root.getElement().setProperty("title", "Navigace");
        var subMenu = root.getSubMenu();
        for (NavigationTarget target : commonNavigationTargets()) {
            HorizontalLayout row = new HorizontalLayout(target.icon().create(), new Span(target.label()));
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);
            subMenu.addItem(row, e -> UI.getCurrent().navigate(target.view()));
        }
        return navMenuBar;
    }

    private Component createDesktopNavigation() {
        Nav nav = new Nav();
        nav.addClassNames(
                "desktop-nav",
                Display.FLEX,
                Overflow.AUTO,
                Padding.Horizontal.MEDIUM,
                Padding.Vertical.XSMALL,
                AlignItems.START
        );
        UnorderedList list = new UnorderedList();
        list.addClassNames(Display.FLEX, Gap.XSMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE);
        for (NavigationTarget target : commonNavigationTargets()) {
            list.add(new MenuListItem(target.label(), target.icon().create(), target.view()));
        }
        nav.add(list);
        return nav;
    }

    /**
     * Creates common menu items for logged-in users.
     *
     * @return the list of common menu items
     */
    private List<NavigationTarget> commonNavigationTargets() {
        return List.of(
                new NavigationTarget("MISH - Úvod", VaadinIcon.HOME, MainPageView.class),
                new NavigationTarget("Kapitoly", VaadinIcon.OPEN_BOOK, ChapterListingView.class),
                new NavigationTarget("Modely", VaadinIcon.CUBES, ModelListingView.class),
                new NavigationTarget("Kvízy", VaadinIcon.LIGHTBULB, QuizListingView.class)
        );
    }

    /**
     * Shows the cookie notification to the user.
     *
     */
    private void showCookieNotification() {
        CookiesNotification notification = new CookiesNotification();
        notification.open();
    }

    private Component createUserMenu(Authentication authentication, String username) {

        MenuBar menuBar = new MenuBar();
        menuBar.addClassName("app-user-menu");


        HorizontalLayout avatarLayout = new HorizontalLayout();
        avatarLayout.addClassName("app-user-trigger");
        avatarLayout.setSpacing(false);
        avatarLayout.setPadding(false);
        avatarLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        avatarLayout.getStyle().set("cursor", "pointer");
        avatarLayout.getStyle().set("min-width", "0");

        AvatarListItem avatar = new AvatarListItem(username);

        Icon dropdownIcon = VaadinIcon.ELLIPSIS_DOTS_V.create();
        dropdownIcon.addClassNames(LumoUtility.TextColor.SECONDARY);
        dropdownIcon.setSize("1rem");

        avatarLayout.add(avatar, dropdownIcon);

        MenuItem userItem = menuBar.addItem(avatarLayout);

        var subMenu = userItem.getSubMenu();

        subMenu.addItem(new HorizontalLayout(VaadinIcon.QUESTION.create(), new Span("Dokumentace")),
                e -> UI.getCurrent().navigate(DocumentationView.class));

        if (authentication.getAuthorities().stream().anyMatch(auth ->
                "ROLE_ADMIN".equals(auth.getAuthority()) || "ROLE_TEACHER".equals(auth.getAuthority()))) {

            subMenu.addItem(new HorizontalLayout(VaadinIcon.COG.create(), new Span("Administrační centrum")),
                    e -> UI.getCurrent().navigate(AdministrationView.class));
        }

        Span themeLabel = new Span(currentThemeLabel());
        MenuItem themeItem = subMenu.addItem(new HorizontalLayout(VaadinIcon.MOON.create(), themeLabel), e -> {
            ThemeModeToggleButton.toggleTheme(UI.getCurrent());
            themeLabel.setText(currentThemeLabel());
        });
        themeItem.getElement().setProperty("title", "Přepnout režim");

        subMenu.addItem(new HorizontalLayout(VaadinIcon.SIGN_OUT.create(), new Span("Odhlásit se")),
                e -> UI.getCurrent().getPage().setLocation("/custom-logout"));

        return menuBar;
    }

    private record NavigationTarget(String label, VaadinIcon icon, Class<? extends Component> view) {
    }

    private String currentThemeLabel() {
        return ThemeModeToggleButton.isDarkMode(UI.getCurrent()) ? "Režim: tmavý" : "Režim: světlý";
    }
}
