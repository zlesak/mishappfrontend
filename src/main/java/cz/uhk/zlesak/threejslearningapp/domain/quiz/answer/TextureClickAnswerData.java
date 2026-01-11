package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Answer data for texture click question.
 * Contains identifiers for the model and texture, as well as a hex color code.
 * Extends AbstractAnswerData to inherit common answer properties.
 * @see AbstractAnswerData
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TextureClickAnswerData extends AbstractAnswerData {
    String modelId;
    String textureId;
    String hexColor;
}

