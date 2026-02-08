package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Answer data for matching question.
 * Contains a map of correct matches where key is the index of the item in the first column
 * and value is the index of the corresponding item in the second column.
 * Extends AbstractAnswerData to inherit common answer properties.
 * @see AbstractAnswerData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MatchingAnswerData extends AbstractAnswerData {
    Map<Integer, Integer> correctMatches;
}

