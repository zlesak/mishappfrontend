package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Texture click question - user clicks on specific area of 3D model texture.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TextureClickQuestionData extends AbstractQuestionData {
    String modelId;
    String textureId;
}

