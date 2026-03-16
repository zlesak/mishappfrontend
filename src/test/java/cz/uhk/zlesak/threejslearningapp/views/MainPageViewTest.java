package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.DividerComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainPageViewTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void constructor_shouldBuildLandingPageSectionsAndTranslatedCtas() {
        MainPageView view = new MainPageView();
        Footer footer = findAll(view.getContent(), Footer.class).getFirst();
        Span footerText = findAll(footer, Span.class).getFirst();

        assertEquals("MISH - Úvod", view.getPageTitle());
        assertEquals(4, findAll(view.getContent(), DividerComponent.class).size());
        assertEquals(2, findAll(view.getContent(), Button.class).size());
        assertTrue(footerText.getText().contains(String.valueOf(Year.now().getValue())));
        assertTrue(footerText.getText().contains("Vytvořeno ve spolupráci"));
    }
}
