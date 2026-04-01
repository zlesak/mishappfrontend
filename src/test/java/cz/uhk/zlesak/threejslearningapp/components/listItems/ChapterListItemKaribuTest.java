package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.github.mvysny.kaributesting.v10.ElementUtilsKt;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import tools.jackson.databind.node.NullNode;
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

        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("Kapitola anatomie"));
        assertTrue(texts.contains("teacher"));
        assertTrue(texts.contains("Lebka 3D"));

        button(item, "Otevřít").click();
        assertSame(chapter, VaadinSession.getCurrent().getAttribute("chapterEntity"));
        button(item, "Upravit").click();
        assertSame(chapter, VaadinSession.getCurrent().getAttribute("chapterEntity"));
    }

    @Test
    void deleteConfirmationShouldCallChapterService() {
        when(chapterService.delete("chapter-1")).thenReturn(true);
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI.getCurrent().add(item);
        button(item, "Smazat").click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        MockVaadin.clientRoundtrip(false);
        assertFalse(dialog.isOpened());
        verify(chapterService).delete("chapter-1");
    }

    @Test
    void chapter_withNullCreatorId_shouldNotRenderCreatorRow() {
        ChapterEntity c = ChapterEntity.builder().id("c2").name("Bez autora").creatorId(null).models(List.of()).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertFalse(texts.contains("teacher"));
    }

    @Test
    void chapter_withDuplicateModels_shouldDeduplicateModelBadges() {
        QuickModelEntity dup = QuickModelEntity.builder().metadataId("meta-dup")
                .model(ModelFileEntity.builder().id("model-1").name("Lebka 3D").build()).build();
        ChapterEntity chapter = ChapterEntity.builder().id("ch-dup").name("Dup").models(List.of(model(), dup)).build();
        ChapterListItem item = new ChapterListItem(chapter, true, false);
        UI.getCurrent().add(item);
        assertEquals(1, findAll(item, Span.class).stream().filter(s -> "Lebka 3D".equals(s.getText())).count());
    }

    @Test
    void chapter_withModelHavingNullName_shouldSkipModelBadge() {
        QuickModelEntity nullName = QuickModelEntity.builder().metadataId("meta-nn")
                .model(ModelFileEntity.builder().id("model-nn").name(null).build()).build();
        ChapterEntity chapter = ChapterEntity.builder().id("ch-nn").name("NN").models(List.of(nullName)).build();
        ChapterListItem item = new ChapterListItem(chapter, true, false);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void modelBadgeClick_shouldStoreModelInSession() {
        QuickModelEntity model = model();
        ChapterListItem item = new ChapterListItem(chapter(model), true, false);
        UI.getCurrent().add(item);
        Span badge = findAll(item, Span.class).stream()
                .filter(s -> s.getClassNames().contains("chapter-model-badge")).findFirst().orElseThrow();
        ElementUtilsKt._fireDomEvent(badge.getElement(), new DomEvent(badge.getElement(), "click", NullNode.instance));
        assertSame(model, VaadinSession.getCurrent().getAttribute("quickModelEntity"));
    }

    @Test
    void delete_whenServiceReturnsFalse_shouldShowErrorNotification() {
        when(chapterService.delete("chapter-1")).thenReturn(false);
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI.getCurrent().add(item);
        button(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        MockVaadin.clientRoundtrip(false);
        verify(chapterService).delete("chapter-1");
    }

    @Test
    void delete_whenServiceThrows_shouldHandleErrorGracefully() {
        when(chapterService.delete("chapter-1")).thenThrow(new RuntimeException("network error"));
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI.getCurrent().add(item);
        button(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        MockVaadin.clientRoundtrip(false);
        verify(chapterService).delete("chapter-1");
    }

    @Test
    void delete_success_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(chapterService.delete("chapter-1")).thenReturn(true);
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI ui = UI.getCurrent();
        ui.add(item);
        button(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(chapterService).delete("chapter-1");
    }

    @Test
    void delete_false_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(chapterService.delete("chapter-1")).thenReturn(false);
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI ui = UI.getCurrent();
        ui.add(item);
        button(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(chapterService).delete("chapter-1");
    }

    @Test
    void delete_exception_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(chapterService.delete("chapter-1")).thenThrow(new RuntimeException("timeout"));
        ChapterListItem item = new ChapterListItem(chapter(model()), true, true);
        UI ui = UI.getCurrent();
        ui.add(item);
        button(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kapitolu")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(chapterService).delete("chapter-1");
    }

    private Button button(ChapterListItem item, String text) {
        return findAll(item, Button.class).stream().filter(c -> text.equals(c.getText())).findFirst().orElseThrow();
    }

    private ChapterEntity chapter(QuickModelEntity model) {
        return ChapterEntity.builder().id("chapter-1").name("Kapitola anatomie").creatorId("teacher")
                .created(Instant.parse("2025-01-12T10:15:00Z")).updated(Instant.parse("2025-02-05T08:30:00Z"))
                .models(List.of(model)).build();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder().metadataId("metadata-model-1")
                .model(ModelFileEntity.builder().id("model-1").name("Lebka 3D").build()).build();
    }
}
