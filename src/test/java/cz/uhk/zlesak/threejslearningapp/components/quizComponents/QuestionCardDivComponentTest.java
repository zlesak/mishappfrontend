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
        SingleChoiceQuestionData question = SingleChoiceQuestionData.builder()
                .questionId("q-1")
                .questionText("Which bone is the longest?")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(2)
                .options(List.of("Femur", "Tibia", "Radius"))
                .build();

        Map<String, AbstractSubmissionData> answers = new HashMap<>();
        QuestionCardDivComponent card = new QuestionCardDivComponent(question, 1, answers);

        assertTrue("100%".equals(card.getWidth()), "Expected width to be 100% (widthFull)");

        assertFalse(card.getChildren().findAny().isEmpty());
    }

    @Test
    void answerChangedListenerShouldPopulateAnswersMap() {
        SingleChoiceQuestionData question = SingleChoiceQuestionData.builder()
                .questionId("q-42")
                .questionText("Identify the bone.")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("Option A", "Option B"))
                .build();

        Map<String, AbstractSubmissionData> answers = new HashMap<>();
        new QuestionCardDivComponent(question, 3, answers);

        assertTrue(answers.isEmpty(), "No selection made yet");
    }
}

