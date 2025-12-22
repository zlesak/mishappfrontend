package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Answer data for multiple choice question.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MultipleChoiceAnswerData extends AbstractAnswerData {
    List<Integer> correctItems;
}

