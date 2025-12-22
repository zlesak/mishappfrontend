package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Submission for multiple choice question type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("MULTIPLE_CHOICE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class MultipleChoiceSubmissionData extends AbstractSubmissionData {
    List<Integer> selectedItems;
}

