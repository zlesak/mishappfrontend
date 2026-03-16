package cz.uhk.zlesak.threejslearningapp.testsupport;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class KaribuSpringTestSupport {
    private static final String VIEWS_PACKAGE = "cz.uhk.zlesak.threejslearningapp.views";

    private KaribuSpringTestSupport() {
    }

    public static void setUp(ApplicationContext applicationContext) {
        SpringContextUtils.setContext(applicationContext);
        Routes routes = new Routes().autoDiscoverViews(VIEWS_PACKAGE);
        MockSpringServlet servlet = new MockSpringServlet(routes, applicationContext, UI::new);
        MockVaadin.setup(UI::new, servlet);
        UI.getCurrent().setLocale(java.util.Locale.forLanguageTag("cs"));
    }

    public static void tearDown() {
        SecurityContextHolder.clearContext();
        MockVaadin.tearDown();
    }
}
