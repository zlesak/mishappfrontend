package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Submission for texture click question type
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

