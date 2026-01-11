package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Submission for open text question type where user can provide a free-form text answer.
 * Contains a string representing the user's text answer.
 * Extends AbstractSubmissionData to inherit common submission properties.
 * @see AbstractSubmissionData
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("OPEN_TEXT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OpenTextSubmissionData extends AbstractSubmissionData {
    String text;
}

