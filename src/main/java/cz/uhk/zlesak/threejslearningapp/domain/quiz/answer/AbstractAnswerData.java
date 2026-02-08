package cz.uhk.zlesak.threejslearningapp.domain.quiz.answer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all answer types.
 * Contains common properties shared by all answer types as questionId and type.
 * Used for polymorphic serialization/deserialization of different answer types.
 * @see MultipleChoiceAnswerData
 * @see SingleChoiceAnswerData
 * @see OpenTextAnswerData
 * @see MatchingAnswerData
 * @see OrderingAnswerData
 * @see TextureClickAnswerData
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "_class"
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = MultipleChoiceAnswerData.class, name = "MultipleChoiceAnswerData"),
  @JsonSubTypes.Type(value = SingleChoiceAnswerData.class, name = "SingleChoiceAnswerData"),
  @JsonSubTypes.Type(value = OpenTextAnswerData.class, name = "OpenTextAnswerData"),
  @JsonSubTypes.Type(value = MatchingAnswerData.class, name = "MatchingAnswerData"),
  @JsonSubTypes.Type(value = OrderingAnswerData.class, name = "OrderingAnswerData"),
  @JsonSubTypes.Type(value = TextureClickAnswerData.class, name = "TextureClickAnswerData")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractAnswerData {
    String questionId;
    QuestionTypeEnum type;
}

