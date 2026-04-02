package cz.uhk.zlesak.threejslearningapp.components.inputs.quizes;

import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchQuestionOptionTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructorShouldInitializeIndexAndOptionSelect() {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(1, 2, 3));

        assertEquals(1, option.getIndex());
        assertNotNull(option.getOptionSelect());
        assertNotNull(option.getQuestionId());
    }

    @Test
    void constructorShouldInitializeOptionFieldWithProvidedValue() {
        MatchQuestionOption option = new MatchQuestionOption(2, "quiz.option.label", List.of(1, 2), "expected text");

        assertEquals("expected text", option.getOptionField().getValue());
        assertEquals(2, option.getIndex());
    }

    @Test
    void constructorShouldPopulateOptionSelectWithGivenIndices() {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(10, 20, 30));

        Select<Integer> select = option.getOptionSelect();
        assertNotNull(select);
        assertNull(select.getValue());
    }

    @Test
    void updateShouldChangeIndexAndLabel() {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(1, 2, 3));

        option.update(5, List.of(1, 2, 3, 4, 5));

        assertEquals(5, option.getIndex());
    }

    @Test
    void updateShouldPreserveSelectValueWhenStillInNewIndices() {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(1, 2, 3));
        option.getOptionSelect().setValue(2);

        option.update(2, List.of(1, 2, 3, 4));

        assertEquals(2, option.getOptionSelect().getValue());
    }

    @Test
    void updateShouldNotRestoreSelectValueWhenRemovedFromNewIndices() {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(1, 2, 3));
        option.getOptionSelect().setValue(3);

        option.update(1, List.of(1, 2));

        assertNull(option.getOptionSelect().getValue());
    }

    @Test
    void constructorShouldAddOptionSelectAtIndex1() throws Exception {
        MatchQuestionOption option = new MatchQuestionOption(1, "quiz.option.label", List.of(1, 2));

        Field selectField = MatchQuestionOption.class.getDeclaredField("optionSelect");
        selectField.setAccessible(true);
        Select<?> select = (Select<?>) selectField.get(option);

        assertTrue(option.getChildren().anyMatch(c -> c == select));
    }
}
