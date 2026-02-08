package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Ordering question data class - Represents an ordering question with a list of items to be ordered.
 * Extends AbstractQuestionData to inherit common question properties.
 * @see AbstractQuestionData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderingQuestionData extends AbstractQuestionData {
    List<String> items;
}

