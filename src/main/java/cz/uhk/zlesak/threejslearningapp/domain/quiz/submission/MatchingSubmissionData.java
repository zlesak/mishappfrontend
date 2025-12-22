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
 * Submission data for matching question type
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

