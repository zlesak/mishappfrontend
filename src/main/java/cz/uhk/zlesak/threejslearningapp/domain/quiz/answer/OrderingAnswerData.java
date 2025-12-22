package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Answer data for ordering question.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderingAnswerData extends AbstractAnswerData {
    List<Integer> correctOrder;
}

