package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A quiz with its questions and the correct answers.
 * Questions and answers are linked by question id, not by position. The answers are left out
 * whenever a quiz is loaded for a student, so the answer key never reaches the browser.
 *
 * @see QuickQuizEntity for the listing form.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "quiz")
@TypeAlias("Quiz")
public class QuizEntity extends QuickQuizEntity {

    List<AbstractQuestionData> questions;

    List<AbstractAnswerData> answers;

    /**
     * Seconds left of the reader's own attempt, filled in when a student opens the quiz to take it.
     * Attempt state rather than quiz state, and so never stored: it is carried here because the
     * player needs the time this student has left, which is not the quiz's time limit once the
     * attempt has been running for a while.
     */
    @Transient
    Integer remainingSeconds;

    /**
     * @return points obtainable when every question is answered correctly.
     */
    public int maxScore() {
        if (questions == null) {
            return 0;
        }
        return questions.stream()
                .filter(question -> question != null && question.getPoints() != null)
                .mapToInt(AbstractQuestionData::getPoints)
                .sum();
    }
}
