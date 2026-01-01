package cz.uhk.zlesak.threejslearningapp.domain.quiz;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuickQuizResult extends AbstractEntity {
    String quizId;
    String userId;
    String name;
    String chapterName;
    Integer maxScore;
    Integer totalScore;
    Double percentage;
}
