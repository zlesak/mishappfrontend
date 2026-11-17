package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

/**
 * Answer data for single choice question.
 * Contains the index of the correct item.
 * Extends AbstractAnswerData to inherit common answer properties.
 * @see AbstractAnswerData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TypeAlias("SingleChoiceAnswerData")
public class SingleChoiceAnswerData extends AbstractAnswerData {
    Integer correctIndex;
}

