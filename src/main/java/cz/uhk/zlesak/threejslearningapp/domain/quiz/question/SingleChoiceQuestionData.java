package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

/**
 * Single choice question data class - Represents an ordering question with a list of items to be ordered.
 * Extends AbstractQuestionData to inherit common question properties.
 * @see AbstractQuestionData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TypeAlias("SingleChoiceQuestionData")
public class SingleChoiceQuestionData extends AbstractQuestionData {
    List<String> options;
}

