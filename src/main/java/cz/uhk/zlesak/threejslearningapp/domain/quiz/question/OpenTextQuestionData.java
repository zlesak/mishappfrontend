package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Open text question
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OpenTextQuestionData extends AbstractQuestionData {
    String placeholder = "";
}

