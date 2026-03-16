package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
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

import java.util.List;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class QuizResultListItemKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void shouldRenderResultLabelsAndFormattedPercentage() {
        QuizResultListItem item = new QuizResultListItem(result(), false, "quiz-results");
        UI.getCurrent().add(item);

        List<String> texts = findAll(item, Span.class).stream()
                .map(Span::getText)
                .toList();

        assertTrue(texts.contains("10"));
        assertTrue(texts.contains("7"));
        assertTrue(texts.stream().anyMatch(text -> text.contains("70") && text.contains("%")));
        assertTrue(findAll(item, Button.class).stream().anyMatch(button -> "Otevřít".equals(button.getText())));
    }

    private QuickQuizResult result() {
        return QuickQuizResult.builder()
                .id("result-1")
                .name("Výsledky kostry")
                .maxScore(10)
                .totalScore(7)
                .percentage(70.0)
                .build();
    }
}
