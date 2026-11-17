package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.DividerComponent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ShowcaseVideo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(footerText.getText().contains(String.valueOf(Year.now().getValue())));
        assertTrue(footerText.getText().contains("Vytvořeno ve spolupráci"));
    }

    @Test
    void theHeadingLadderHasNoMissingRungs() {
        // The slogan was an H2 although it introduces nothing, and the showcase captions were H4s
        // under an H2 — so the outline read 1, 2, 2, 4.
        MainPageView view = new MainPageView();

        assertEquals(1, findAll(view.getContent(), H1.class).size());
        assertEquals("MISH", findAll(view.getContent(), H1.class).getFirst().getText());
        assertTrue(findAll(view.getContent(), H4.class).isEmpty(), "No level may be skipped");

        List<String> h3Texts = findAll(view.getContent(), H3.class).stream().map(H3::getText).toList();
        assertTrue(h3Texts.contains("Prohlížení modelů"), h3Texts.toString());

        List<String> h2Texts = findAll(view.getContent(), H2.class).stream().map(H2::getText).toList();
        assertTrue(h2Texts.contains("Spolupráce"), h2Texts.toString());
        assertTrue(h2Texts.stream().noneMatch(t -> t.contains("Interaktivní studium")), h2Texts.toString());
    }

    @Test
    void theSloganIsAParagraph() {
        MainPageView view = new MainPageView();

        assertTrue(findAll(view.getContent(), Paragraph.class).stream()
                .anyMatch(p -> "Interaktivní studium anatomie ve 3D".equals(p.getText())));
    }

    @Test
    void theRecordingsAreVideosThatCanBePausedAndFetchNothingUpFront() {
        // Three GIFs, thirteen megabytes of them, looped for longer than five seconds with no way to
        // stop them. As video the same three come to under three megabytes and bring their own controls.
        MainPageView view = new MainPageView();

        List<ShowcaseVideo> recordings = findAll(view.getContent(), ShowcaseVideo.class);
        assertEquals(3, recordings.size());

        recordings.forEach(video -> {
            assertEquals("video", video.getElement().getTag());
            // WCAG 2.2.2: pausing is possible because the element brings the control itself.
            assertEquals("", video.getElement().getAttribute("controls"));
            assertEquals("", video.getElement().getAttribute("muted"));
            assertEquals("", video.getElement().getAttribute("loop"));
            // Nothing but the poster is fetched until the visitor scrolls to it; the source is applied
            // from the observer, so it is not on the element yet.
            assertEquals("none", video.getElement().getAttribute("preload"));
            assertNull(video.getElement().getAttribute("src"));
            assertNotNull(video.getElement().getAttribute("poster"));
            assertNotNull(video.getElement().getAttribute("aria-label"));

            // Two formats. H.264 is what mainstream browsers play; Chromium builds without proprietary
            // codecs cannot decode it and would sit on the poster forever, so VP9 is offered first.
            List<String> sources = video.getElement().getChildren()
                    .map(child -> child.getAttribute("data-src"))
                    .toList();
            assertEquals(2, sources.size());
            assertTrue(sources.getFirst().endsWith(".webm"), sources.toString());
            assertTrue(sources.get(1).endsWith(".mp4"), sources.toString());
        });
    }

    @Test
    void noLongParagraphIsJustified() {
        // Justification stretches each line's word spacing differently; the resulting rivers of white
        // are a documented obstacle for readers with dyslexia.
        MainPageView view = new MainPageView();

        findAll(view.getContent(), Paragraph.class).forEach(paragraph ->
                assertTrue(!"justify".equals(paragraph.getStyle().get("text-align"))
                                && !paragraph.getClassNames().contains("text-justify"),
                        "Paragraph is still justified: " + paragraph.getText()));
    }

    @Test
    void theHeroLogoIsDecorativeAndTheFacultyLogosAreNamedInFull() {
        MainPageView view = new MainPageView();
        List<Image> images = findAll(view.getContent(), Image.class);

        Image heroLogo = images.stream()
                .filter(image -> "/img/MISH_big.png".equals(image.getSrc()))
                .findFirst()
                .orElseThrow();
        assertEquals("", heroLogo.getAlt().orElse(null));

        List<String> alts = images.stream().map(image -> image.getAlt().orElse("")).toList();
        assertTrue(alts.contains("Fakulta informatiky a managementu Univerzity Hradec Králové"), alts.toString());
        assertTrue(alts.contains("Lékařská fakulta Univerzity Karlovy v Hradci Králové"), alts.toString());
        assertTrue(alts.stream().noneMatch(alt -> alt.endsWith("Logo")), alts.toString());
    }

    @Test
    void theFooterLinksToTheAccessibilityStatement() {
        MainPageView view = new MainPageView();
        Footer footer = findAll(view.getContent(), Footer.class).getFirst();

        Anchor statement = findAll(footer, Anchor.class).stream()
                .filter(anchor -> "Prohlášení o přístupnosti".equals(anchor.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The statement required by the EAA is not linked"));
        // Its own public route, not the documentation: that sits behind the login, and someone who
        // cannot use the login screen is the person most likely to need the statement.
        assertEquals("/accessibility", statement.getHref());
        assertNull(statement.getElement().getAttribute("target"));

        // On the same line as the copyright rather than under it.
        assertEquals("flex", footer.getStyle().get("display"));
    }
}
