package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationQuestion;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MatchingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MultipleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OrderingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MatchingSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MultipleChoiceSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OrderingSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.SingleChoiceSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.TextureClickSubmissionData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Grades a submitted quiz against its stored answers.
 * Every question of the quiz appears in the result, including the ones the student skipped, so the
 * feedback screen shows the whole quiz rather than only what was answered.
 */
@Service
public class QuizGradingService {

    /**
     * Grades one attempt.
     *
     * @param quiz        the quiz, loaded with its answers.
     * @param submissions what the student submitted.
     * @return per-question feedback together with the score.
     */
    public Graded grade(QuizEntity quiz, List<AbstractSubmissionData> submissions) {
        Map<String, AbstractQuestionData> questions = indexQuestions(quiz);
        Map<String, AbstractAnswerData> answers = indexAnswers(quiz);
        Map<String, AbstractSubmissionData> submitted = indexSubmissions(submissions);

        List<QuizValidationQuestion> results = new ArrayList<>();
        int totalScore = 0;

        for (Map.Entry<String, AbstractQuestionData> entry : questions.entrySet()) {
            AbstractQuestionData question = entry.getValue();
            AbstractSubmissionData submission = submitted.get(entry.getKey());
            AbstractAnswerData answer = answers.get(entry.getKey());

            boolean correct = submission != null && answer != null && isCorrect(answer, submission);
            int points = correct && question.getPoints() != null ? question.getPoints() : 0;
            totalScore += points;

            results.add(new QuizValidationQuestion(question.getQuestionText(), correct, points, submission));
        }

        int maxScore = quiz.maxScore();
        double percentage = maxScore == 0 ? 0.0 : ((double) totalScore / maxScore) * 100.0;
        return new Graded(totalScore, maxScore, percentage, results);
    }

    /**
     * Compares one submitted answer with the stored correct one.
     * Comparisons are all-or-nothing; there is no partial credit for a partly correct answer.
     */
    private boolean isCorrect(AbstractAnswerData answer, AbstractSubmissionData submission) {
        return switch (answer) {
            case MultipleChoiceAnswerData expected -> submission instanceof MultipleChoiceSubmissionData actual
                    && asSet(expected.getCorrectItems()).equals(asSet(actual.getSelectedItems()));
            case SingleChoiceAnswerData expected -> submission instanceof SingleChoiceSubmissionData actual
                    && Objects.equals(expected.getCorrectIndex(), actual.getSelectedIndex());
            case MatchingAnswerData expected -> submission instanceof MatchingSubmissionData actual
                    && Objects.equals(expected.getCorrectMatches(), actual.getMatches());
            case OrderingAnswerData expected -> submission instanceof OrderingSubmissionData actual
                    && Objects.equals(expected.getCorrectOrder(), actual.getOrder());
            case OpenTextAnswerData expected -> submission instanceof OpenTextSubmissionData actual
                    && matchesAnAcceptedAnswer(expected, actual.getText());
            case TextureClickAnswerData expected -> submission instanceof TextureClickSubmissionData actual
                    && expected.getHexColor() != null
                    && expected.getHexColor().equalsIgnoreCase(actual.getHexColor());
            default -> false;
        };
    }

    /**
     * Compares a typed answer with the ones the author accepted.
     * Surrounding space is always ignored, because it is never what the author meant to test.
     * Case is only ignored when the author cleared "exact match"; leaving it on is what lets a
     * question about spelling or capitalisation be marked as such.
     *
     * @param expected the accepted answers and the author's matching preference.
     * @param typed    what the student wrote, may be {@code null}.
     * @return whether the typed text counts as correct.
     */
    private boolean matchesAnAcceptedAnswer(OpenTextAnswerData expected, String typed) {
        if (expected.getAcceptableAnswers() == null) {
            return false;
        }
        boolean exact = Boolean.TRUE.equals(expected.getExactMatch());
        String actual = typed == null ? "" : typed.trim();

        return expected.getAcceptableAnswers().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(accepted -> exact ? accepted.equals(actual) : accepted.equalsIgnoreCase(actual));
    }

    private Map<String, AbstractQuestionData> indexQuestions(QuizEntity quiz) {
        Map<String, AbstractQuestionData> byId = new LinkedHashMap<>();
        if (quiz.getQuestions() != null) {
            for (AbstractQuestionData question : quiz.getQuestions()) {
                if (question != null && question.getQuestionId() != null) {
                    byId.putIfAbsent(question.getQuestionId(), question);
                }
            }
        }
        return byId;
    }

    private Map<String, AbstractAnswerData> indexAnswers(QuizEntity quiz) {
        Map<String, AbstractAnswerData> byId = new LinkedHashMap<>();
        if (quiz.getAnswers() != null) {
            for (AbstractAnswerData answer : quiz.getAnswers()) {
                if (answer != null && answer.getQuestionId() != null) {
                    byId.putIfAbsent(answer.getQuestionId(), answer);
                }
            }
        }
        return byId;
    }

    private Map<String, AbstractSubmissionData> indexSubmissions(List<AbstractSubmissionData> submissions) {
        Map<String, AbstractSubmissionData> byId = new LinkedHashMap<>();
        if (submissions != null) {
            for (AbstractSubmissionData submission : submissions) {
                if (submission != null && submission.getQuestionId() != null) {
                    byId.putIfAbsent(submission.getQuestionId(), submission);
                }
            }
        }
        return byId;
    }

    private static Set<Integer> asSet(List<Integer> values) {
        return values == null ? Set.of() : new HashSet<>(values);
    }

    /**
     * Outcome of grading one attempt.
     *
     * @param totalScore points earned.
     * @param maxScore   points obtainable.
     * @param percentage {@code totalScore} as a percentage of {@code maxScore}.
     * @param results    per-question feedback, in the quiz's own question order.
     */
    public record Graded(int totalScore, int maxScore, double percentage, List<QuizValidationQuestion> results) {
    }
}
