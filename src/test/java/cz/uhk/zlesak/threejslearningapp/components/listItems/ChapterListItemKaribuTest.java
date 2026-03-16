package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ChapterListItemKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void shouldRenderMetadataAndStoreSessionStateForOpenActions() {
        QuickModelEntity model = model();
        ChapterEntity chapter = chapter(model);
        ChapterListItem item = new ChapterListItem(chapter, true, true);
        UI.getCurrent().add(item);

        List<String> texts = findAll(item, Span.class).stream()
                .map(Span::getText)
                .toList();

        assertTrue(texts.contains("Kapitola anatomie"));
        assertTrue(texts.contains("teacher"));
        assertTrue(texts.contains("Lebka 3D"));

        button(item, "Otevřít").click();
        assertSame(chapter, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("chapterEntity"));

        button(item, "Upravit").click();
        assertSame(chapter, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("chapterEntity"));

        assertEquals(1, findAll(item, Span.class).stream()
                .filter(span -> "Lebka 3D".equals(span.getText()))
                .count());
    }

    @Test
    void deleteConfirmationShouldCallChapterService() {
        when(chapterService.delete("chapter-1")).thenReturn(true);

        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI.getCurrent().add(item);

        button(item, "Smazat").click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);

        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));

        assertFalse(dialog.isOpened());
        verify(chapterService).delete("chapter-1");
    }

    private Button button(ChapterListItem item, String text) {
        return findAll(item, Button.class).stream()
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow();
    }

    private ChapterEntity chapter(QuickModelEntity model) {
        return ChapterEntity.builder()
                .id("chapter-1")
                .name("Kapitola anatomie")
                .creatorId("teacher")
                .created(Instant.parse("2025-01-12T10:15:00Z"))
                .updated(Instant.parse("2025-02-05T08:30:00Z"))
                .models(List.of(model))
                .build();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder()
                .metadataId("metadata-model-1")
                .model(ModelFileEntity.builder()
                        .id("model-1")
                        .name("Lebka 3D")
                        .build())
                .build();
    }
}
