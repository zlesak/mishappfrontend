package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MatchingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MultipleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OrderingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MatchingSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MultipleChoiceSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OrderingSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.SingleChoiceSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.TextureClickSubmissionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGradingServiceTest {

    private final QuizGradingService grading = new QuizGradingService();

    @Test
    void awardsPointsForEveryQuestionTypeAnsweredCorrectly() {
        QuizEntity quiz = quiz(
                List.of(
                        question(new MultipleChoiceQuestionData(), "q1", QuestionTypeEnum.MULTIPLE_CHOICE, 2),
                        question(new cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData(),
                                "q2", QuestionTypeEnum.SINGLE_CHOICE, 3),
                        question(new cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MatchingQuestionData(),
                                "q3", QuestionTypeEnum.MATCHING, 4),
                        question(new cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData(),
                                "q4", QuestionTypeEnum.ORDERING, 5),
                        question(new OpenTextQuestionData(), "q5", QuestionTypeEnum.OPEN_TEXT, 6),
                        question(new cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData(),
                                "q6", QuestionTypeEnum.TEXTURE_CLICK, 7)),
                List.of(
                        multipleChoice("q1", List.of(0, 2)),
                        singleChoice("q2", 1),
                        matching("q3", Map.of(0, 1, 1, 0)),
                        ordering("q4", List.of(2, 0, 1)),
                        openText("q5", List.of("mozek")),
                        textureClick("q6", "#AABBCC")));

        QuizGradingService.Graded graded = grading.grade(quiz, List.of(
                multipleChoiceSubmission("q1", List.of(2, 0)),
                singleChoiceSubmission("q2", 1),
                matchingSubmission("q3", Map.of(0, 1, 1, 0)),
                orderingSubmission("q4", List.of(2, 0, 1)),
                openTextSubmission("q5", "  Mozek "),
                textureClickSubmission("q6", "#aabbcc")));

        assertThat(graded.totalScore()).isEqualTo(27);
        assertThat(graded.maxScore()).isEqualTo(27);
        assertThat(graded.percentage()).isEqualTo(100.0);
        assertThat(graded.results()).hasSize(6).allMatch(result -> Boolean.TRUE.equals(result.getIsCorrect()));
    }

    @Test
    void multipleChoiceIgnoresTheOrderOptionsWereTickedIn() {
        QuizEntity quiz = quiz(
                List.of(question(new MultipleChoiceQuestionData(), "q1", QuestionTypeEnum.MULTIPLE_CHOICE, 1)),
                List.of(multipleChoice("q1", List.of(0, 1, 2))));

        QuizGradingService.Graded graded = grading.grade(quiz,
                List.of(multipleChoiceSubmission("q1", List.of(2, 1, 0))));

        assertThat(graded.totalScore()).isEqualTo(1);
    }

    @Test
    void openTextMatchesRegardlessOfCaseAndSurroundingSpaces() {
        QuizEntity quiz = quiz(
                List.of(question(new OpenTextQuestionData(), "q1", QuestionTypeEnum.OPEN_TEXT, 1)),
                List.of(openText("q1", List.of("Mozeček"))));

        QuizGradingService.Graded graded = grading.grade(quiz,
                List.of(openTextSubmission("q1", "  mozeček  ")));

        assertThat(graded.totalScore()).isEqualTo(1);
    }

    @Test
    void reportsSkippedQuestionsAsIncorrectInsteadOfOmittingThem() {
        QuizEntity quiz = quiz(
                List.of(
                        question(new OpenTextQuestionData(), "q1", QuestionTypeEnum.OPEN_TEXT, 4),
                        question(new OpenTextQuestionData(), "q2", QuestionTypeEnum.OPEN_TEXT, 6)),
                List.of(
                        openText("q1", List.of("ano")),
                        openText("q2", List.of("ne"))));

        QuizGradingService.Graded graded = grading.grade(quiz, List.of(openTextSubmission("q1", "ano")));

        assertThat(graded.results()).hasSize(2);
        assertThat(graded.results().get(1).getIsCorrect()).isFalse();
        assertThat(graded.results().get(1).getSubmission()).isNull();
        assertThat(graded.totalScore()).isEqualTo(4);
        assertThat(graded.maxScore()).isEqualTo(10);
        assertThat(graded.percentage()).isEqualTo(40.0);
    }

    @Test
    void ignoresSubmissionsForQuestionsThatAreNotPartOfTheQuiz() {
        QuizEntity quiz = quiz(
                List.of(question(new OpenTextQuestionData(), "q1", QuestionTypeEnum.OPEN_TEXT, 5)),
                List.of(openText("q1", List.of("ano"))));

        QuizGradingService.Graded graded = grading.grade(quiz, List.of(
                openTextSubmission("q1", "ano"),
                openTextSubmission("neznama-otazka", "ano")));

        assertThat(graded.results()).hasSize(1);
        assertThat(graded.totalScore()).isEqualTo(5);
    }

    @Test
    void scoresAnEmptyQuizAsZeroWithoutDividingByZero() {
        QuizEntity quiz = quiz(List.of(), List.of());

        QuizGradingService.Graded graded = grading.grade(quiz, List.of());

        assertThat(graded.totalScore()).isZero();
        assertThat(graded.percentage()).isZero();
        assertThat(graded.results()).isEmpty();
    }

    private QuizEntity quiz(List<AbstractQuestionData> questions, List<AbstractAnswerData> answers) {
        return QuizEntity.builder().id("quiz-1").name("Kvíz").questions(questions).answers(answers).build();
    }

    private <T extends AbstractQuestionData> T question(T question, String id, QuestionTypeEnum type, int points) {
        question.setQuestionId(id);
        question.setQuestionText("Otázka " + id);
        question.setType(type);
        question.setPoints(points);
        return question;
    }

    private MultipleChoiceAnswerData multipleChoice(String questionId, List<Integer> correctItems) {
        MultipleChoiceAnswerData answer = new MultipleChoiceAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.MULTIPLE_CHOICE);
        answer.setCorrectItems(correctItems);
        return answer;
    }

    private SingleChoiceAnswerData singleChoice(String questionId, int correctIndex) {
        SingleChoiceAnswerData answer = new SingleChoiceAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.SINGLE_CHOICE);
        answer.setCorrectIndex(correctIndex);
        return answer;
    }

    private MatchingAnswerData matching(String questionId, Map<Integer, Integer> correctMatches) {
        MatchingAnswerData answer = new MatchingAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.MATCHING);
        answer.setCorrectMatches(correctMatches);
        return answer;
    }

    private OrderingAnswerData ordering(String questionId, List<Integer> correctOrder) {
        OrderingAnswerData answer = new OrderingAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.ORDERING);
        answer.setCorrectOrder(correctOrder);
        return answer;
    }

    private OpenTextAnswerData openText(String questionId, List<String> acceptableAnswers) {
        OpenTextAnswerData answer = new OpenTextAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.OPEN_TEXT);
        answer.setAcceptableAnswers(acceptableAnswers);
        return answer;
    }

    private TextureClickAnswerData textureClick(String questionId, String hexColor) {
        TextureClickAnswerData answer = new TextureClickAnswerData();
        answer.setQuestionId(questionId);
        answer.setType(QuestionTypeEnum.TEXTURE_CLICK);
        answer.setHexColor(hexColor);
        return answer;
    }

    private AbstractSubmissionData multipleChoiceSubmission(String questionId, List<Integer> selected) {
        MultipleChoiceSubmissionData submission = new MultipleChoiceSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.MULTIPLE_CHOICE);
        submission.setSelectedItems(selected);
        return submission;
    }

    private AbstractSubmissionData singleChoiceSubmission(String questionId, int selected) {
        SingleChoiceSubmissionData submission = new SingleChoiceSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.SINGLE_CHOICE);
        submission.setSelectedIndex(selected);
        return submission;
    }

    private AbstractSubmissionData matchingSubmission(String questionId, Map<Integer, Integer> matches) {
        MatchingSubmissionData submission = new MatchingSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.MATCHING);
        submission.setMatches(matches);
        return submission;
    }

    private AbstractSubmissionData orderingSubmission(String questionId, List<Integer> order) {
        OrderingSubmissionData submission = new OrderingSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.ORDERING);
        submission.setOrder(order);
        return submission;
    }

    private AbstractSubmissionData openTextSubmission(String questionId, String text) {
        OpenTextSubmissionData submission = new OpenTextSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.OPEN_TEXT);
        submission.setText(text);
        return submission;
    }

    private AbstractSubmissionData textureClickSubmission(String questionId, String hexColor) {
        TextureClickSubmissionData submission = new TextureClickSubmissionData();
        submission.setQuestionId(questionId);
        submission.setType(QuestionTypeEnum.TEXTURE_CLICK);
        submission.setHexColor(hexColor);
        return submission;
    }
}
