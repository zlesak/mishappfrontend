package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Submission for texture click question type where user selects a color from given question texture on a model from clicked area.
 * Contains the hex color, model ID, and texture ID representing the user's selection.
 * Extends AbstractSubmissionData to inherit common submission properties.
 * @see AbstractSubmissionData
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("TEXTURE_CLICK")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class TextureClickSubmissionData extends AbstractSubmissionData {
    String hexColor;
    String modelId;
    String textureId;
}

