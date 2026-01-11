package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Answer data for open text question.
 * Contains a list of acceptable answers and a flag indicating if exact match is required.
 * Extends AbstractAnswerData to inherit common answer properties.
 * @see AbstractAnswerData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OpenTextAnswerData extends AbstractAnswerData {
    List<String> acceptableAnswers;
    Boolean exactMatch;
}

