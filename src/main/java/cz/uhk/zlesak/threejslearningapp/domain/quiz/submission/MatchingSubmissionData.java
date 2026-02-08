package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Submission data class for Matching question type where users match items from two columns.
 * Contains a map of matches where the key is the index of the item from the first column
 * and the value is the index of the matched item from the second column.
 * Extends AbstractSubmissionData to inherit common submission properties.
 * @see AbstractSubmissionData
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("MATCHING")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class MatchingSubmissionData extends AbstractSubmissionData {
    Map<Integer, Integer> matches;
}

