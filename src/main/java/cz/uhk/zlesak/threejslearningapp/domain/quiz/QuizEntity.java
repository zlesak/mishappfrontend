package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * QuizEntity Class - Represents a quiz entity with questions, answers, and time limit.
 * Extends QuickQuizEntity to inherit common quiz properties.
 * @see QuickQuizEntity for inherited properties.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuizEntity extends QuickQuizEntity {
    List<AbstractQuestionData> questions;
    List<AbstractAnswerData> answers;
}
