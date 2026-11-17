package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A graded quiz attempt with feedback for every question.
 * The stored answer key is never part of this: each entry says whether the submitted answer was
 * correct and how many points it earned, not what the right answer was.
 *
 * @see QuickQuizResult for the listing form.
 */
@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "quizResults")
@TypeAlias("QuizResult")
public class QuizValidationResult extends QuickQuizResult {

    List<QuizValidationQuestion> questionResults;
}
