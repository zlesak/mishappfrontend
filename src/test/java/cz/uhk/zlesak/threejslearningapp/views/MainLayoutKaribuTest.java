package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.menubar.MenuBar;
import cz.uhk.zlesak.threejslearningapp.components.buttons.LoginButton;
import cz.uhk.zlesak.threejslearningapp.components.buttons.ThemeModeToggleButton;
import cz.uhk.zlesak.threejslearningapp.components.listItems.MenuListItem;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class MainLayoutKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void anonymousNavigation_shouldRenderSharedMenuAndLoginButton() {
        MainLayout layout = new MainLayout();
        UI.getCurrent().add(layout);

        assertEquals(4, findAll(layout, MenuListItem.class).size());
        assertEquals(1, findAll(layout, ThemeModeToggleButton.class).size());
        assertEquals(1, findAll(layout, LoginButton.class).size());
        assertEquals(0, findAll(layout, MenuBar.class).size());
    }

    @Test
    void adminNavigation_shouldRenderUserMenuWithAdministrationEntry() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));

        MainLayout layout = new MainLayout();
        UI.getCurrent().add(layout);
        MenuBar menuBar = findAll(layout, MenuBar.class).getFirst();

        assertEquals(0, findAll(layout, LoginButton.class).size());
        assertEquals(3, menuBar.getItems().getFirst().getSubMenu().getItems().size());
    }
}
