package cz.uhk.zlesak.threejslearningapp.services.conflict;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizConflictResolutionTest {

    private static final Instant MINE_BASE = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant THEIRS_SAVED = Instant.parse("2026-11-01T10:05:00Z");

    /** Echoes the key and its parameters, so rendered text can be checked without the texts. */
    private final QuizConflictResolution resolution = new QuizConflictResolution(
            (key, params) -> params.length == 0 ? key : key + Arrays.toString(params));

    @Test
    void reportsNothingWhenOnlyQuestionIdsDiffer() {
        QuizEntity mine = quiz("Kvíz", 10, "q-new", "Kolik?", 0);
        QuizEntity theirs = quiz("Kvíz", 10, "q-old", "Kolik?", 0);

        assertThat(resolution.differences(mine, theirs)).isEmpty();
    }

    @Test
    void aChangedCorrectAnswerIsADifferenceAndIsShown() {
        QuizEntity mine = quiz("Kvíz", 10, "q-1", "Kolik?", 0);
        QuizEntity theirs = quiz("Kvíz", 10, "q-1", "Kolik?", 1);

        List<ConflictField> fields = resolution.differences(mine, theirs);

        assertThat(fields).singleElement().satisfies(field -> {
            assertThat(field.key()).isEqualTo(QuizConflictResolution.QUESTIONS);
            assertThat(field.mine()).endsWith("- Jedna conflict.quiz.correct\n- Dvě");
            assertThat(field.theirs()).contains("- Dvě conflict.quiz.correct");
        });
    }

    @Test
    void combinesTheChosenFieldsAndKeepsQuestionsWithTheirAnswers() {
        QuizEntity mine = quiz("Můj kvíz", 10, "q-1", "Moje otázka", 0);
        QuizEntity theirs = quiz("Jejich kvíz", 20, "q-2", "Jejich otázka", 1);

        QuizEntity combined = resolution.merge(mine, theirs, Map.of(
                QuizConflictResolution.NAME, ConflictChoice.MINE,
                QuizConflictResolution.TIME_LIMIT, ConflictChoice.THEIRS,
                QuizConflictResolution.QUESTIONS, ConflictChoice.THEIRS));

        assertThat(combined.getName()).isEqualTo("Můj kvíz");
        assertThat(combined.getTimeLimit()).isEqualTo(20);
        assertThat(combined.getQuestions()).extracting(AbstractQuestionData::getQuestionId).containsExactly("q-2");
        assertThat(combined.getAnswers()).extracting(AbstractAnswerData::getQuestionId).containsExactly("q-2");
        assertThat(combined.getUpdated()).isEqualTo(THEIRS_SAVED);
        assertThat(combined.getId()).isEqualTo("quiz-1");
    }

    private static QuizEntity quiz(String name, int timeLimit, String questionId, String text, int correct) {
        return QuizEntity.builder()
                .id("quiz-1")
                .name(name)
                .timeLimit(timeLimit)
                .updated(name.startsWith("Jejich") ? THEIRS_SAVED : MINE_BASE)
                .questions(List.of(SingleChoiceQuestionData.builder().questionId(questionId).questionText(text)
                        .type(QuestionTypeEnum.SINGLE_CHOICE).points(1).options(List.of("Jedna", "Dvě")).build()))
                .answers(List.of(SingleChoiceAnswerData.builder().questionId(questionId)
                        .type(QuestionTypeEnum.SINGLE_CHOICE).correctIndex(correct).build()))
                .build();
    }
}
