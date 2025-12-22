package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Answer data for single choice question.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SingleChoiceAnswerData extends AbstractAnswerData {
    Integer correctIndex;
}

