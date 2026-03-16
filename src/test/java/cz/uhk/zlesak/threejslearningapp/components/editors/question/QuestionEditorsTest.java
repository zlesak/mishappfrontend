package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class QuestionEditorsTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void shouldInstantiateBasicQuestionEditors() {
        assertDoesNotThrow(SingleChoiceQuestionEditor::new);
        assertDoesNotThrow(MultipleChoiceQuestionEditor::new);
        assertDoesNotThrow(MatchingQuestionEditor::new);
        assertDoesNotThrow(OrderingQuestionEditor::new);
        assertDoesNotThrow(OpenTextQuestionEditor::new);
    }

    @Test
    void shouldInstantiateTextureClickEditor() {
        assertDoesNotThrow(() -> new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null)
        ));
    }
}
