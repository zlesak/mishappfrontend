package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

/**
 * A chapter with everything a detail screen needs: the editor.js content on top of the listing data.
 *
 * @see QuickChapterEntity for the listing form.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "chapters")
@TypeAlias("Chapter")
public class ChapterEntity extends QuickChapterEntity {

    /** editor.js document, stored as JSON text. */
    String content;

    /** Maps an editor heading id to the model shown alongside it; used while editing. */
    @Transient
    Map<String, QuickModelEntity> modelHeaderMap;

    /** Quizzes belonging to this chapter, loaded for the detail screen. */
    @Transient
    List<QuickQuizEntity> quizzes;
}
