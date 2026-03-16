package cz.uhk.zlesak.threejslearningapp.components.listItems;

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
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void shouldRenderTimeLimitAndChapterSnippet() {
        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);

        List<String> texts = findAll(item, Span.class).stream()
                .map(Span::getText)
                .toList();

        assertTrue(texts.contains("Procviceni kosti"));
        assertTrue(texts.contains("3 minuty"));
        assertTrue(texts.contains("chapter-"));
    }

    @Test
    void deleteConfirmationShouldCallQuizService() {
        when(quizService.delete("quiz-1")).thenReturn(true);

        QuizListItem item = new QuizListItem(quiz(), true);
        UI.getCurrent().add(item);

        findAll(item, Button.class).stream()
                .filter(candidate -> "Smazat".equals(candidate.getText()))
                .findFirst()
                .orElseThrow()
                .click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);

        _click(_get(Button.class, spec -> spec.withText("Smazat kvíz")));

        assertFalse(dialog.isOpened());
        verify(quizService).delete("quiz-1");
    }


    private QuickQuizEntity quiz() {
        return QuickQuizEntity.builder()
                .id("quiz-1")
                .name("Procviceni kosti")
                .timeLimit(3)
                .chapterId("chapter-123456789")
                .build();
    }
}
