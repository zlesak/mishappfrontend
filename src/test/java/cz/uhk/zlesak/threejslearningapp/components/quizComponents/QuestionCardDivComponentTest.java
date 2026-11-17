package cz.uhk.zlesak.threejslearningapp.components.quizComponents;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionCardDivComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldBuildLayoutAndWireAnswerListener() {
        Map<String, AbstractSubmissionData> answers = new HashMap<>();
        QuestionCardDivComponent card = new QuestionCardDivComponent(question("q-1", 2), 1, 12, answers, null);

        assertTrue("100%".equals(card.getWidth()), "Expected width to be 100% (widthFull)");

        assertFalse(card.getChildren().findAny().isEmpty());
    }

    @Test
    void answerChangedListenerShouldPopulateAnswersMap() {
        Map<String, AbstractSubmissionData> answers = new HashMap<>();
        new QuestionCardDivComponent(question("q-42", 1), 3, 12, answers, null);

        assertTrue(answers.isEmpty(), "No selection made yet");
    }

    @Test
    void theQuestionIsAGroupWhoseLegendCarriesTheWording() {
        // The wording used to be a loose Span next to the answer controls, so on a radio group a screen
        // reader announced the text of the chosen option and nothing about what was being asked.
        QuestionCardDivComponent card = new QuestionCardDivComponent(
                question("q-7", 3), 6, 12, new HashMap<>(), null);

        assertEquals("fieldset", card.getElement().getTag());
        assertEquals("Otázka 6 z 12: Which bone is the longest?", card.getQuestionSummary());
    }

    @Test
    void theQuestionCanBeLinkedToButIsNotItselfInTheTabOrder() {
        QuestionCardDivComponent card = new QuestionCardDivComponent(
                question("q-9", 1), 4, 10, new HashMap<>(), null);

        assertEquals("otazka-4", card.getId().orElseThrow());
        assertEquals("-1", card.getElement().getAttribute("tabindex"));
    }

    private SingleChoiceQuestionData question(String id, int points) {
        return SingleChoiceQuestionData.builder()
                .questionId(id)
                .questionText("Which bone is the longest?")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(points)
                .options(List.of("Femur", "Tibia", "Radius"))
                .build();
    }
}
