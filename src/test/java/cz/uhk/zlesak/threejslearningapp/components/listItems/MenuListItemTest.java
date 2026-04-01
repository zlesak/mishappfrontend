package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MenuListItemTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();

        // Make the mocked VaadinService provide a DeploymentConfiguration so that
        // RouteUtil.checkForClientRouteCollisions does not throw NPE.
        DeploymentConfiguration deploymentConfig = mock(DeploymentConfiguration.class);
        when(deploymentConfig.isProductionMode()).thenReturn(false);
        when(VaadinService.getCurrent().getDeploymentConfiguration()).thenReturn(deploymentConfig);

        // Make the mocked VaadinSession persist attributes so that
        // SessionRouteRegistry (used by Router.getRegistry()) works properly.
        Map<Class<?>, Object> attrs = new HashMap<>();
        VaadinSession session = VaadinSession.getCurrent();
        when(session.getAttribute(any(Class.class)))
                .thenAnswer(inv -> attrs.get(inv.getArgument(0)));
        doAnswer(inv -> { attrs.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(session).setAttribute(any(Class.class), any());

        // Register the test view so RouterLink.setRoute succeeds.
        RouteConfiguration.forSessionScope().setRoute("dummy", DummyView.class);
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldAddRouterLinkWithTitle() {
        MenuListItem item = new MenuListItem("Test Menu", null, DummyView.class);

        assertNotNull(item);
        // The list item must have children (the RouterLink).
        assertTrue(item.getChildren().findAny().isPresent());
    }

    @Test
    void getViewShouldReturnRegisteredViewClass() {
        MenuListItem item = new MenuListItem("Chapters", null, DummyView.class);
        assertEquals(DummyView.class, item.getView());
    }

    /** Minimal view component used only to satisfy the router registry in tests. */
    private static class DummyView extends Component {}
}
