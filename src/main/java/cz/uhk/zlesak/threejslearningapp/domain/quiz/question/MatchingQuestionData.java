package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Matching question data class - Represents a matching question with left and right items to be matched.
 * Extends AbstractQuestionData to inherit common question properties.
 * @see AbstractQuestionData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MatchingQuestionData extends AbstractQuestionData {
    List<String> leftItems;
    List<String> rightItems;
}

