package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

/**
 * Multiple choice question data class - Represents a multiple choice question with a list of options.
 * Extends AbstractQuestionData to inherit common question properties.
 * @see AbstractQuestionData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TypeAlias("MultipleChoiceQuestionData")
public class MultipleChoiceQuestionData extends AbstractQuestionData {
    List<String> options;
}

