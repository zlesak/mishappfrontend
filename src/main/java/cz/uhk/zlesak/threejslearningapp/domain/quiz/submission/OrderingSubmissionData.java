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
 * Submission for ordering question type where user arranges items in a specific order.
 * Contains a list of indices representing the ordered items.
 * Extends AbstractSubmissionData to inherit common submission properties.
 * @see AbstractSubmissionData
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ORDERING")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OrderingSubmissionData extends AbstractSubmissionData {
    List<Integer> order;
}

