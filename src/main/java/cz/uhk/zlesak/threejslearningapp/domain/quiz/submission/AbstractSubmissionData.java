package cz.uhk.zlesak.threejslearningapp.domain.quiz.submission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for user's answer submission to a question
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_class"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MultipleChoiceSubmissionData.class, name = "MultipleChoiceSubmissionData"),
        @JsonSubTypes.Type(value = SingleChoiceSubmissionData.class, name = "SingleChoiceSubmissionData"),
        @JsonSubTypes.Type(value = OpenTextSubmissionData.class, name = "OpenTextSubmissionData"),
        @JsonSubTypes.Type(value = MatchingSubmissionData.class, name = "MatchingSubmissionData"),
        @JsonSubTypes.Type(value = OrderingSubmissionData.class, name = "OrderingSubmissionData"),
        @JsonSubTypes.Type(value = TextureClickSubmissionData.class, name = "TextureClickSubmissionData")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractSubmissionData {
    String questionId;
    QuestionTypeEnum type;
}

