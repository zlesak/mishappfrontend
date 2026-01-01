package cz.uhk.zlesak.threejslearningapp.domain.quiz.question;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all question types.
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "_class"
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = MultipleChoiceQuestionData.class, name = "MultipleChoiceQuestionData"),
  @JsonSubTypes.Type(value = SingleChoiceQuestionData.class, name = "SingleChoiceQuestionData"),
  @JsonSubTypes.Type(value = OpenTextQuestionData.class, name = "OpenTextQuestionData"),
  @JsonSubTypes.Type(value = MatchingQuestionData.class, name = "MatchingQuestionData"),
  @JsonSubTypes.Type(value = OrderingQuestionData.class, name = "OrderingQuestionData"),
  @JsonSubTypes.Type(value = TextureClickQuestionData.class, name = "TextureClickQuestionData")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractQuestionData {
    String questionId;
    String questionText;
    QuestionTypeEnum type;
    Integer points;
}

