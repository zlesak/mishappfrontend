package cz.uhk.zlesak.threejslearningapp.integration;

import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class MainPageRouteTest {
    @Autowired
    WebApplicationContext context;

    @Test
    void applicationContext_shouldLoadWebLayer() {
        assertTrue(context.containsBean("dispatcherServlet"));
        assertTrue(context.containsBean("securityFilterChain"));
        assertTrue(context.containsBean("logoutController"));
    }
}
