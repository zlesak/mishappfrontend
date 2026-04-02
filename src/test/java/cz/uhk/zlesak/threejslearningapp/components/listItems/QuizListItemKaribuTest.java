package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
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

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class QuizListItemKaribuTest {
    @Autowired private ApplicationContext applicationContext;
    @MockitoBean private QuizService quizService;

    @BeforeEach void setUp() { KaribuSpringTestSupport.setUp(applicationContext); }
    @AfterEach void tearDown() { KaribuSpringTestSupport.tearDown(); }

    @Test
    void shouldRenderTimeLimitAndChapterSnippet() {
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("Procviceni kosti"));
        assertTrue(texts.contains("3 minuty"));
        assertTrue(texts.contains("chapter-"));
    }

    @Test
    void deleteConfirmationShouldCallQuizService() {
        when(quizService.delete("quiz-1")).thenReturn(true);
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        smazat(item).click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        MockVaadin.clientRoundtrip(false);
        assertFalse(dialog.isOpened());
        verify(quizService).delete("quiz-1");
    }

    @Test
    void editButton_adminMode_shouldNavigateToEditView() {
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        findAll(item, Button.class).stream().filter(b -> "Upravit".equals(b.getText())).findFirst().orElseThrow().click();
    }

    @Test
    void delete_whenServiceReturnsFalse_shouldShowErrorNotification() {
        when(quizService.delete("quiz-1")).thenReturn(false);
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        smazat(item).click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        MockVaadin.clientRoundtrip(false);
        verify(quizService).delete("quiz-1");
    }

    @Test
    void delete_whenServiceThrows_shouldHandleErrorGracefully() {
        when(quizService.delete("quiz-1")).thenThrow(new RuntimeException("delete error"));
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        smazat(item).click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        MockVaadin.clientRoundtrip(false);
        verify(quizService).delete("quiz-1");
    }

    @Test
    void delete_success_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(quizService.delete("quiz-1")).thenReturn(true);
        QuizListItem item = new QuizListItem(quiz(), true);
        UI ui = UI.getCurrent(); ui.add(item);
        smazat(item).click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(quizService).delete("quiz-1");
    }

    @Test
    void delete_false_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(quizService.delete("quiz-1")).thenReturn(false);
        QuizListItem item = new QuizListItem(quiz(), true);
        UI ui = UI.getCurrent(); ui.add(item);
        smazat(item).click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(quizService).delete("quiz-1");
    }

    @Test
    void delete_exception_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(quizService.delete("quiz-1")).thenThrow(new RuntimeException("timeout"));
        QuizListItem item = new QuizListItem(quiz(), true);
        UI ui = UI.getCurrent(); ui.add(item);
        smazat(item).click();
        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(quizService).delete("quiz-1");
    }

    @Test
    void quiz_withNullTimeLimit_shouldNotRenderTimeLimitRow() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-2").name("No Limit Quiz").timeLimit(null).chapterId("ch-1").build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.stream().noneMatch(t -> t.contains("limit") || t.contains("minut") || t.contains("minuta")));
    }

    @Test
    void quiz_withTimeLimitOfOne_shouldShowSingularMinutaForm() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-3").name("One Min Quiz").timeLimit(1).chapterId("ch-1").build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("1 minuta"));
    }

    @Test
    void quiz_withTimeLimitOfZero_shouldShowBezLimitu() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-4").name("Zero Limit Quiz").timeLimit(0).chapterId("ch-1").build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("Bez limitu"));
    }

    @Test
    void quiz_withTimeLimitOfSix_shouldShowMinutPluralForm() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-5").name("Six Min Quiz").timeLimit(6).chapterId("ch-1").build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.contains("6 minut"));
    }

    @Test
    void quiz_withNullChapterId_shouldNotRenderChapterRow() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-6").name("No Chapter Quiz").timeLimit(5).chapterId(null).build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.stream().noneMatch(t -> t.contains("chapter-")));
    }

    @Test
    void quiz_withBlankChapterId_shouldNotRenderChapterRow() {
        QuickQuizEntity q = QuickQuizEntity.builder().id("quiz-7").name("Blank Chapter Quiz").timeLimit(5).chapterId("   ").build();
        QuizListItem item = new QuizListItem(q, true);
        UI.getCurrent().add(item);
        List<String> texts = findAll(item, Span.class).stream().map(Span::getText).toList();
        assertTrue(texts.stream().noneMatch(t -> t.trim().isEmpty() || "   ".equals(t)));
    }

    @Test
    void quiz_openButton_shouldNotThrowWhenClicked() {
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);
        Button openButton = findAll(item, Button.class).stream()
                .filter(b -> "Otevřít".equals(b.getText())).findFirst().orElseThrow();
        openButton.click();
    }

    @Test
    void quiz_nonAdminView_editAndDeleteButtonsShouldBeHidden() {
        QuizListItem item = new QuizListItem(quiz(), false);
        UI.getCurrent().add(item);
        List<Button> buttons = findAll(item, Button.class);
        assertTrue(buttons.stream().filter(b -> "Upravit".equals(b.getText())).noneMatch(b -> b.isVisible()));
        assertTrue(buttons.stream().filter(b -> "Smazat".equals(b.getText())).noneMatch(b -> b.isVisible()));
    }

    private Button smazat(QuizListItem item) {
        return findAll(item, Button.class).stream().filter(b -> "Smazat".equals(b.getText())).findFirst().orElseThrow();
    }

    private QuickQuizEntity quiz() {
        return QuickQuizEntity.builder().id("quiz-1").name("Procviceni kosti").timeLimit(3).chapterId("chapter-123456789").build();
    }
}

