package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.*;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void openTextEditorHasNonNullQuestionTextField() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void openTextEditorDefaultQuestionTextIsEmpty() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void openTextEditorDefaultPointsIsOne() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    void openTextEditorHasExactMatchCheckbox() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertFalse(VaadinTestSupport.findAll(editor, Checkbox.class).isEmpty());
    }

    @Test
    void openTextEditorExactMatchDefaultIsTrue() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        Checkbox checkbox = VaadinTestSupport.findAll(editor, Checkbox.class).get(0);
        assertTrue(checkbox.getValue());
    }

    @Test
    void openTextEditorAddOptionIncreasesOptionsCount() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.addOption("answer1");
        assertEquals(1, editor.getOptions().size());
    }

    @Test
    void openTextEditorSetAnswerDataPopulatesAcceptableAnswers() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        OpenTextAnswerData answerData = OpenTextAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .acceptableAnswers(List.of("answer1", "answer2"))
                .exactMatch(true)
                .build();
        editor.setAnswerData(answerData);
        assertEquals(2, editor.getOptions().size());
    }

    @Test
    void openTextEditorSetAnswerDataSetsExactMatchFalse() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        OpenTextAnswerData answerData = OpenTextAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .acceptableAnswers(List.of())
                .exactMatch(false)
                .build();
        editor.setAnswerData(answerData);
        Checkbox checkbox = VaadinTestSupport.findAll(editor, Checkbox.class).get(0);
        assertFalse(checkbox.getValue());
    }

    @Test
    void openTextEditorValidateReturnsFalseWhenQuestionTextEmpty() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertFalse(editor.validate());
    }

    @Test
    void openTextEditorValidateReturnsFalseWhenNoOptions() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.getQuestionTextField().setValue("What is X?");
        assertFalse(editor.validate());
    }

    @Test
    void openTextEditorValidateReturnsTrueWhenValid() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.getQuestionTextField().setValue("What is X?");
        editor.addOption("X is 42");
        assertTrue(editor.validate());
    }

    @Test
    void openTextEditorGetQuestionDataReturnsOpenTextType() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.getQuestionTextField().setValue("Test");
        assertInstanceOf(OpenTextQuestionData.class, editor.getQuestionData());
    }

    @Test
    void openTextEditorGetQuestionDataContainsQuestionText() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.getQuestionTextField().setValue("Sample question");
        assertEquals("Sample question", editor.getQuestionData().getQuestionText());
    }

    @Test
    void openTextEditorInitializePopulatesQuestionText() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        OpenTextQuestionData data = OpenTextQuestionData.builder()
                .questionId("q1")
                .questionText("What is X?")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .points(3)
                .build();
        editor.initialize(data);
        assertEquals("What is X?", editor.getQuestionTextField().getValue());
    }

    @Test
    void openTextEditorInitializePopulatesPoints() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        OpenTextQuestionData data = OpenTextQuestionData.builder()
                .questionId("q1")
                .questionText("What is X?")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .points(5)
                .build();
        editor.initialize(data);
        assertEquals(5, editor.getPointsField().getValue());
    }

    @Test
    void openTextEditorGetAnswerDataReturnsOpenTextAnswerType() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        assertInstanceOf(OpenTextAnswerData.class, editor.getAnswerData());
    }

    @Test
    void openTextEditorGetAnswerDataExactMatchDefaultIsTrue() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        OpenTextAnswerData data = (OpenTextAnswerData) editor.getAnswerData();
        assertTrue(data.getExactMatch());
    }

    @Test
    void openTextEditorGetAnswerDataReturnsAcceptableAnswers() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.addOption("foo");
        editor.addOption("bar");
        OpenTextAnswerData data = (OpenTextAnswerData) editor.getAnswerData();
        assertEquals(2, data.getAcceptableAnswers().size());
    }

    @Test
    void openTextEditorAddMultipleOptionsUpdatesCount() {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.addOptions(List.of("a", "b", "c"));
        assertEquals(3, editor.getOptions().size());
    }

    @Test
    void singleChoiceEditorHasNonNullQuestionTextField() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void singleChoiceEditorDefaultQuestionTextIsEmpty() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void singleChoiceEditorDefaultPointsIsOne() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void singleChoiceEditorHasSelectComponent() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertFalse(VaadinTestSupport.findAll(editor, Select.class).isEmpty());
    }

    @Test
    void singleChoiceEditorAddOptionIncreasesOptionsCount() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.addOption("Option A");
        assertEquals(1, editor.getOptions().size());
    }

    @Test
    void singleChoiceEditorInitializePopulatesOptions() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        SingleChoiceQuestionData data = SingleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Pick one")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("A", "B", "C"))
                .build();
        editor.initialize(data);
        assertEquals(3, editor.getOptions().size());
    }

    @Test
    void singleChoiceEditorInitializePopulatesQuestionText() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        SingleChoiceQuestionData data = SingleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Pick one")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("A", "B"))
                .build();
        editor.initialize(data);
        assertEquals("Pick one", editor.getQuestionTextField().getValue());
    }

    @Test
    void singleChoiceEditorGetQuestionDataReturnsCorrectType() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertInstanceOf(SingleChoiceQuestionData.class, editor.getQuestionData());
    }

    @Test
    void singleChoiceEditorGetQuestionDataContainsOptions() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("A");
        editor.addOption("B");
        SingleChoiceQuestionData data = (SingleChoiceQuestionData) editor.getQuestionData();
        assertEquals(2, data.getOptions().size());
    }

    @Test
    void singleChoiceEditorValidateReturnsFalseWhenEmpty() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        assertFalse(editor.validate());
    }

    @Test
    void singleChoiceEditorValidateReturnsFalseWithOneOption() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("A");
        assertFalse(editor.validate());
    }

    @Test
    void singleChoiceEditorGetAnswerDataReturnsCorrectType() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        SingleChoiceQuestionData questionData = SingleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Q")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("A", "B"))
                .build();
        editor.initialize(questionData);
        editor.setAnswerData(SingleChoiceAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .correctIndex(0)
                .build());
        assertInstanceOf(SingleChoiceAnswerData.class, editor.getAnswerData());
    }

    @Test
    void singleChoiceEditorSetAnswerDataUpdatesCorrectIndex() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        SingleChoiceQuestionData questionData = SingleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Q")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("A", "B", "C"))
                .build();
        editor.initialize(questionData);
        editor.setAnswerData(SingleChoiceAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .correctIndex(1)
                .build());
        SingleChoiceAnswerData result = (SingleChoiceAnswerData) editor.getAnswerData();
        assertEquals(1, result.getCorrectIndex());
    }

    @Test
    void singleChoiceEditorGetQuestionDataContainsQuestionText() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("My question");
        assertEquals("My question", editor.getQuestionData().getQuestionText());
    }

    @Test
    void multipleChoiceEditorHasNonNullQuestionTextField() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void multipleChoiceEditorDefaultQuestionTextIsEmpty() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void multipleChoiceEditorDefaultPointsIsOne() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void multipleChoiceEditorHasMultiSelectComboBox() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertFalse(VaadinTestSupport.findAll(editor, MultiSelectComboBox.class).isEmpty());
    }

    @Test
    void multipleChoiceEditorAddOptionIncreasesOptionsCount() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.addOption("Choice A");
        assertEquals(1, editor.getOptions().size());
    }

    @Test
    void multipleChoiceEditorInitializePopulatesOptions() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        MultipleChoiceQuestionData data = MultipleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Choose all")
                .type(QuestionTypeEnum.MULTIPLE_CHOICE)
                .points(2)
                .options(List.of("A", "B", "C"))
                .build();
        editor.initialize(data);
        assertEquals(3, editor.getOptions().size());
    }

    @Test
    void multipleChoiceEditorInitializePopulatesQuestionText() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        MultipleChoiceQuestionData data = MultipleChoiceQuestionData.builder()
                .questionId("q1")
                .questionText("Choose all that apply")
                .type(QuestionTypeEnum.MULTIPLE_CHOICE)
                .points(2)
                .options(List.of("A", "B"))
                .build();
        editor.initialize(data);
        assertEquals("Choose all that apply", editor.getQuestionTextField().getValue());
    }

    @Test
    void multipleChoiceEditorGetQuestionDataReturnsCorrectType() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertInstanceOf(MultipleChoiceQuestionData.class, editor.getQuestionData());
    }

    @Test
    void multipleChoiceEditorGetQuestionDataContainsOptions() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("A");
        editor.addOption("B");
        MultipleChoiceQuestionData data = (MultipleChoiceQuestionData) editor.getQuestionData();
        assertEquals(2, data.getOptions().size());
    }

    @Test
    void multipleChoiceEditorGetQuestionDataContainsQuestionText() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("My multi question");
        assertEquals("My multi question", editor.getQuestionData().getQuestionText());
    }

    @Test
    void multipleChoiceEditorValidateReturnsFalseWhenEmpty() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertFalse(editor.validate());
    }

    @Test
    void multipleChoiceEditorValidateReturnsFalseWithOneOption() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("A");
        assertFalse(editor.validate());
    }

    @Test
    void multipleChoiceEditorGetAnswerDataReturnsCorrectType() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        assertInstanceOf(MultipleChoiceAnswerData.class, editor.getAnswerData());
    }

    @Test
    void multipleChoiceEditorGetAnswerDataReturnsEmptyListWhenNoSelection() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        MultipleChoiceAnswerData data = (MultipleChoiceAnswerData) editor.getAnswerData();
        assertTrue(data.getCorrectItems().isEmpty());
    }

    @Test
    void multipleChoiceEditorSetAnswerDataDoesNotThrow() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        MultipleChoiceAnswerData answerData = MultipleChoiceAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.MULTIPLE_CHOICE)
                .correctItems(List.of())
                .build();
        assertDoesNotThrow(() -> editor.setAnswerData(answerData));
    }

    @Test
    void orderingEditorHasNonNullQuestionTextField() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void orderingEditorDefaultQuestionTextIsEmpty() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void orderingEditorDefaultPointsIsOne() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    void orderingEditorAddOptionIncreasesOptionsCount() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.addOption("Item 1");
        assertEquals(1, editor.getOptions().size());
    }

    @Test
    void orderingEditorInitializePopulatesItems() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        OrderingQuestionData data = OrderingQuestionData.builder()
                .questionId("q1")
                .questionText("Order these")
                .type(QuestionTypeEnum.ORDERING)
                .points(2)
                .items(List.of("First", "Second", "Third"))
                .build();
        editor.initialize(data);
        assertEquals(3, editor.getOptions().size());
    }

    @Test
    void orderingEditorInitializePopulatesQuestionText() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        OrderingQuestionData data = OrderingQuestionData.builder()
                .questionId("q1")
                .questionText("Order these items")
                .type(QuestionTypeEnum.ORDERING)
                .points(2)
                .items(List.of("First", "Second"))
                .build();
        editor.initialize(data);
        assertEquals("Order these items", editor.getQuestionTextField().getValue());
    }

    @Test
    void orderingEditorGetQuestionDataReturnsCorrectType() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertInstanceOf(OrderingQuestionData.class, editor.getQuestionData());
    }

    @Test
    void orderingEditorGetQuestionDataContainsItems() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("First");
        editor.addOption("Second");
        OrderingQuestionData data = (OrderingQuestionData) editor.getQuestionData();
        assertEquals(2, data.getItems().size());
    }

    @Test
    void orderingEditorGetQuestionDataContainsQuestionText() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.getQuestionTextField().setValue("Put in order");
        assertEquals("Put in order", editor.getQuestionData().getQuestionText());
    }

    @Test
    void orderingEditorGetAnswerDataReturnsCorrectType() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertInstanceOf(OrderingAnswerData.class, editor.getAnswerData());
    }

    @Test
    void orderingEditorGetAnswerDataCorrectOrderMatchesOptionCount() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.addOption("First");
        editor.addOption("Second");
        editor.addOption("Third");
        OrderingAnswerData data = (OrderingAnswerData) editor.getAnswerData();
        assertEquals(3, data.getCorrectOrder().size());
    }

    @Test
    void orderingEditorValidateReturnsFalseWhenEmpty() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        assertFalse(editor.validate());
    }

    @Test
    void orderingEditorValidateReturnsFalseWithOneItem() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("First");
        assertFalse(editor.validate());
    }

    @Test
    void orderingEditorValidateReturnsTrueWhenValid() {
        OrderingQuestionEditor editor = new OrderingQuestionEditor();
        editor.getQuestionTextField().setValue("Order these items");
        editor.addOption("First");
        editor.addOption("Second");
        assertTrue(editor.validate());
    }

    @Test
    void matchingEditorHasNonNullQuestionTextField() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void matchingEditorDefaultQuestionTextIsEmpty() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void matchingEditorDefaultPointsIsOne() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    void matchingEditorHasAnswersLayout() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertNotNull(editor.answersLayout);
    }

    @Test
    void matchingEditorHasAccordion() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertNotNull(editor.qAaAccordion);
    }

    @Test
    void matchingEditorDefaultOptionsIsEmpty() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertTrue(editor.getOptions().isEmpty());
    }

    @Test
    void matchingEditorInitializePopulatesLeftItems() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        MatchingQuestionData data = MatchingQuestionData.builder()
                .questionId("q1")
                .questionText("Match these")
                .type(QuestionTypeEnum.MATCHING)
                .points(2)
                .leftItems(List.of("A", "B", "C"))
                .rightItems(List.of("1", "2", "3"))
                .build();
        editor.initialize(data);
        assertEquals(3, editor.getOptions().size());
    }

    @Test
    void matchingEditorInitializePopulatesQuestionText() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        MatchingQuestionData data = MatchingQuestionData.builder()
                .questionId("q1")
                .questionText("Match these")
                .type(QuestionTypeEnum.MATCHING)
                .points(2)
                .leftItems(List.of("A", "B"))
                .rightItems(List.of("1", "2"))
                .build();
        editor.initialize(data);
        assertEquals("Match these", editor.getQuestionTextField().getValue());
    }

    @Test
    void matchingEditorGetQuestionDataReturnsCorrectType() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertInstanceOf(MatchingQuestionData.class, editor.getQuestionData());
    }

    @Test
    void matchingEditorGetQuestionDataContainsLeftItems() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        MatchingQuestionData questionData = MatchingQuestionData.builder()
                .questionId("q1")
                .questionText("Q")
                .type(QuestionTypeEnum.MATCHING)
                .points(1)
                .leftItems(List.of("A", "B"))
                .rightItems(List.of("1", "2"))
                .build();
        editor.initialize(questionData);
        MatchingQuestionData result = (MatchingQuestionData) editor.getQuestionData();
        assertEquals(2, result.getLeftItems().size());
    }

    @Test
    void matchingEditorGetQuestionDataContainsQuestionText() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        editor.getQuestionTextField().setValue("Match items");
        assertEquals("Match items", editor.getQuestionData().getQuestionText());
    }

    @Test
    void matchingEditorGetAnswerDataReturnsCorrectType() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertInstanceOf(MatchingAnswerData.class, editor.getAnswerData());
    }

    @Test
    void matchingEditorGetAnswerDataReturnsEmptyMapWhenNoAnswers() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        MatchingAnswerData data = (MatchingAnswerData) editor.getAnswerData();
        assertTrue(data.getCorrectMatches().isEmpty());
    }

    @Test
    void matchingEditorValidateReturnsFalseWhenEmpty() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        assertFalse(editor.validate());
    }

    @Test
    void matchingEditorValidateReturnsFalseWithInsufficientItems() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        editor.getQuestionTextField().setValue("Q");
        editor.addOption("A");
        assertFalse(editor.validate());
    }

    @Test
    void matchingEditorSetAnswerDataDoesNotThrow() {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        MatchingAnswerData answerData = MatchingAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.MATCHING)
                .correctMatches(Map.of())
                .build();
        assertDoesNotThrow(() -> editor.setAnswerData(answerData));
    }

    @Test
    void textureClickEditorHasNonNullQuestionTextField() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertNotNull(editor.getQuestionTextField());
    }

    @Test
    void textureClickEditorDefaultQuestionTextIsEmpty() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertEquals("", editor.getQuestionTextField().getValue());
    }

    @Test
    void textureClickEditorDefaultPointsIsOne() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertEquals(1, editor.getPointsField().getValue());
    }

    @Test
    void textureClickEditorGetSelectedModelIdReturnsNullInitially() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertNull(editor.getSelectedModelId());
    }

    @Test
    void textureClickEditorGetSelectedTextureIdReturnsNullInitially() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertNull(editor.getSelectedTextureId());
    }

    @Test
    void textureClickEditorGetSelectedAreaIdReturnsNullInitially() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertNull(editor.getSelectedAreaId());
    }

    @Test
    void textureClickEditorGetQuestionDataReturnsCorrectType() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertInstanceOf(TextureClickQuestionData.class, editor.getQuestionData());
    }

    @Test
    void textureClickEditorGetAnswerDataReturnsCorrectType() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertInstanceOf(TextureClickAnswerData.class, editor.getAnswerData());
    }

    @Test
    void textureClickEditorValidateReturnsFalseWhenEmpty() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertFalse(editor.validate());
    }

    @Test
    void textureClickEditorInitializeStoresQuestionText() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        TextureClickQuestionData data = TextureClickQuestionData.builder()
                .questionId("q1")
                .questionText("Click the texture")
                .type(QuestionTypeEnum.TEXTURE_CLICK)
                .points(1)
                .modelId("model-1")
                .textureId("tex-1")
                .build();
        editor.initialize(data);
        assertEquals("Click the texture", editor.getQuestionTextField().getValue());
    }

    @Test
    void textureClickEditorGetQuestionDataContainsQuestionText() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        editor.getQuestionTextField().setValue("Texture question");
        assertEquals("Texture question", editor.getQuestionData().getQuestionText());
    }

    @Test
    void textureClickEditorSetAnswerDataDoesNotThrow() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        TextureClickAnswerData answerData = TextureClickAnswerData.builder()
                .questionId("q1")
                .type(QuestionTypeEnum.TEXTURE_CLICK)
                .modelId("model-1")
                .textureId("tex-1")
                .hexColor("#FF0000")
                .build();
        assertDoesNotThrow(() -> editor.setAnswerData(answerData));
    }

    @Test
    void textureClickEditorAddOptionThrowsNotImplemented() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertThrows(NotImplementedException.class, () -> editor.addOption("test"));
    }

    @Test
    void textureClickEditorRemoveOptionThrowsNotImplemented() {
        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(
                modelId -> CompletableFuture.completedFuture(null));
        assertThrows(NotImplementedException.class, () -> editor.removeOption("test-id"));
    }
}

