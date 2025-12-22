package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * ChapterEntity Class - Represents a detailed chapter entity with models, quizzes, and content.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChapterEntity extends QuickChapterEntity {
    Map<String, QuickModelEntity> modelHeaderMap;
    String content;
    List<QuickModelEntity> models;
    List<QuickQuizEntity> quizzes;
}