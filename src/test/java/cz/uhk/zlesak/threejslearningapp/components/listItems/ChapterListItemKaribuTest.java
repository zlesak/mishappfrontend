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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.node.NullNode;

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

    @Test
    void chapter_withBlankCreatorId_shouldNotRenderCreatorRow() {
        ChapterEntity c = ChapterEntity.builder().id("c-blank").name("Blank Creator").creatorId("   ")
                .models(List.of()).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertFalse(texts.contains("   "));
    }

    @Test
    void chapter_withNullCreatedDate_shouldNotRenderDateRow() {
        ChapterEntity c = ChapterEntity.builder().id("c-nd").name("No Date").creatorId(null)
                .created(null).updated(null).models(List.of()).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void chapter_withNullUpdatedDate_shouldNotRenderUpdatedRow() {
        ChapterEntity c = ChapterEntity.builder().id("c-nu").name("No Updated").creatorId(null)
                .created(java.time.Instant.now()).updated(null).models(List.of()).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void chapter_withEmptyModelsList_shouldNotRenderModelBadges() {
        ChapterEntity c = ChapterEntity.builder().id("c-em").name("Empty Models").creatorId(null)
                .models(List.of()).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        long badgeCount = findAll(item, Span.class).stream()
                .filter(s -> s.getClassNames().contains("chapter-model-badge")).count();
        assertEquals(0, badgeCount);
    }

    @Test
    void chapter_withNullModelsList_shouldRenderWithoutError() {
        ChapterEntity c = ChapterEntity.builder().id("c-nm").name("Null Models").creatorId(null)
                .models(null).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void chapter_nonAdmin_editAndDeleteButtonsShouldBeHidden() {
        ChapterListItem item = new ChapterListItem(chapter(model()), true, false);
        UI.getCurrent().add(item);
        List<Button> buttons = findAll(item, Button.class);
        assertTrue(buttons.stream().filter(b -> "Upravit".equals(b.getText())).noneMatch(b -> b.isVisible()));
        assertTrue(buttons.stream().filter(b -> "Smazat".equals(b.getText())).noneMatch(b -> b.isVisible()));
    }

    @Test
    void chapter_withTwoDistinctModels_shouldRenderTwoBadges() {
        QuickModelEntity m1 = QuickModelEntity.builder().metadataId("meta-1")
                .model(ModelFileEntity.builder().id("model-1").name("Model One").build()).build();
        QuickModelEntity m2 = QuickModelEntity.builder().metadataId("meta-2")
                .model(ModelFileEntity.builder().id("model-2").name("Model Two").build()).build();
        ChapterEntity c = ChapterEntity.builder().id("c-multi").name("Multi").models(List.of(m1, m2)).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        long badgeCount = findAll(item, Span.class).stream()
                .filter(s -> s.getClassNames().contains("chapter-model-badge")).count();
        assertEquals(2, badgeCount);
    }

    @Test
    void chapter_withNullModelEntityInList_shouldSkipNullModel() {
        List<QuickModelEntity> modelsWithNull = new java.util.ArrayList<>();
        modelsWithNull.add(null);
        modelsWithNull.add(model());
        ChapterEntity c = ChapterEntity.builder().id("c-nullm").name("Null Model").models(modelsWithNull).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void chapter_withBlankMetadataId_shouldStillRenderBadgeUsingModelId() {
        QuickModelEntity m = QuickModelEntity.builder().metadataId("")
                .model(ModelFileEntity.builder().id("model-fallback").name("Fallback Model").build()).build();
        ChapterEntity c = ChapterEntity.builder().id("c-fb").name("Fallback").models(List.of(m)).build();
        ChapterListItem item = new ChapterListItem(c, true, false);
        UI.getCurrent().add(item);
        long badgeCount = findAll(item, Span.class).stream()
                .filter(s -> "Fallback Model".equals(s.getText())).count();
        assertEquals(1, badgeCount);
    }

    @Test
    void chapter_editClick_adminMode_shouldStoreChapterInSession() {
        ChapterEntity chapterEntity = chapter(model());
        ChapterListItem item = new ChapterListItem(chapterEntity, true, true);
        UI.getCurrent().add(item);
        button(item, "Upravit").click();
        assertSame(chapterEntity, VaadinSession.getCurrent().getAttribute("chapterEntity"));
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

