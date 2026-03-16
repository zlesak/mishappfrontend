package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.*;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.TextureClickedEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class QuestionRendererComponentsTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void factoryShouldCreateRendererForEveryQuestionType() {
        assertInstanceOf(SingleChoiceQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(singleChoiceQuestion()));
        assertInstanceOf(MultipleChoiceQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(multipleChoiceQuestion()));
        assertInstanceOf(OpenTextQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(openTextQuestion()));
        assertInstanceOf(MatchingQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(matchingQuestion()));
        assertInstanceOf(OrderingQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(orderingQuestion()));
        assertInstanceOf(TextureClickQuestionRendererComponent.class, AbstractQuestionRendererComponent.create(textureClickQuestion()));
    }

    @Test
    void singleChoiceRendererShouldExposeSelectedIndexAndNotifyListener() {
        SingleChoiceQuestionRendererComponent component = new SingleChoiceQuestionRendererComponent(singleChoiceQuestion());
        AtomicReference<SingleChoiceSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((SingleChoiceSubmissionData) data));

        RadioButtonGroup<Integer> radioGroup = typed(VaadinTestSupport.findFirst(component, RadioButtonGroup.class));
        radioGroup.setValue(1);

        SingleChoiceSubmissionData submission = component.getSubmissionData();
        assertEquals("q-single", submission.getQuestionId());
        assertEquals(1, submission.getSelectedIndex());
        assertEquals(1, captured.get().getSelectedIndex());
    }

    @Test
    void multipleChoiceRendererShouldConvertToOneBasedIndices() {
        MultipleChoiceQuestionRendererComponent component = new MultipleChoiceQuestionRendererComponent(multipleChoiceQuestion());
        AtomicReference<MultipleChoiceSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((MultipleChoiceSubmissionData) data));

        CheckboxGroup<Integer> checkboxGroup = typed(VaadinTestSupport.findFirst(component, CheckboxGroup.class));
        checkboxGroup.setValue(java.util.Set.of(0, 2));

        MultipleChoiceSubmissionData submission = component.getSubmissionData();
        assertEquals(List.of(1, 3), submission.getSelectedItems().stream().sorted().toList());
        assertEquals(List.of(1, 3), captured.get().getSelectedItems().stream().sorted().toList());
    }

    @Test
    void openTextRendererShouldReturnTypedTextAndNotifyListener() {
        OpenTextQuestionRendererComponent component = new OpenTextQuestionRendererComponent(openTextQuestion());
        AtomicReference<OpenTextSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((OpenTextSubmissionData) data));

        TextArea textArea = VaadinTestSupport.findFirst(component, TextArea.class);
        textArea.setValue("Ulna");

        OpenTextSubmissionData submission = component.getSubmissionData();
        assertEquals("Ulna", submission.getText());
        assertEquals("Ulna", captured.get().getText());
    }

    @Test
    void matchingRendererShouldReturnOnlySelectedPairs() {
        MatchingQuestionRendererComponent component = new MatchingQuestionRendererComponent(matchingQuestion());
        AtomicReference<MatchingSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((MatchingSubmissionData) data));

        List<Select<Integer>> selects = typedList(VaadinTestSupport.findAll(component, Select.class));
        selects.get(0).setValue(1);
        selects.get(1).setValue(0);

        MatchingSubmissionData submission = component.getSubmissionData();
        assertEquals(2, submission.getMatches().size());
        assertEquals(1, submission.getMatches().get(0));
        assertEquals(0, submission.getMatches().get(1));
        assertEquals(submission.getMatches(), captured.get().getMatches());
    }

    @Test
    void orderingRendererShouldReturnSelectedOrderAndDefaultMissingToMinusOne() {
        OrderingQuestionRendererComponent component = new OrderingQuestionRendererComponent(orderingQuestion());
        AtomicReference<OrderingSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((OrderingSubmissionData) data));

        List<Select<Integer>> selects = typedList(VaadinTestSupport.findAll(component, Select.class));
        assertEquals(3, selects.size());
        selects.get(0).setValue(2);
        selects.get(1).setValue(0);

        OrderingSubmissionData submission = component.getSubmissionData();
        assertEquals(3, submission.getOrder().size());
        assertTrue(submission.getOrder().contains(-1));
        assertEquals(submission.getOrder(), captured.get().getOrder());
    }

    @Test
    void textureClickRendererShouldFireModelLoadAndReturnClickedColor() {
        TextureClickQuestionRendererComponent component = new TextureClickQuestionRendererComponent(textureClickQuestion());
        AtomicReference<ModelLoadEvent> modelLoadEvent = new AtomicReference<>();
        AtomicReference<TextureClickSubmissionData> captured = new AtomicReference<>();
        component.setAnswerChangedListener(data -> captured.set((TextureClickSubmissionData) data));
        UI.getCurrent().add(component);
        ComponentUtil.addListener(UI.getCurrent(), ModelLoadEvent.class, modelLoadEvent::set);

        Button button = VaadinTestSupport.findFirst(component, Button.class);
        button.click();

        assertNotNull(modelLoadEvent.get());
        assertEquals("q-texture", modelLoadEvent.get().getQuestionId());
        assertEquals("model-1", modelLoadEvent.get().getQuickModelEntity().getMetadataId());
        assertEquals("texture-1", modelLoadEvent.get().getQuickModelEntity().getOtherTextures().getFirst().getId());

        ComponentUtil.fireEvent(
                UI.getCurrent(),
                new TextureClickedEvent(new ThreeJsComponent(), "q-texture", "model-1", "texture-1", "#AA11CC")
        );

        TextureClickSubmissionData submission = component.getSubmissionData();
        assertEquals("#AA11CC", submission.getHexColor());
        assertEquals("#AA11CC", captured.get().getHexColor());
    }

    private SingleChoiceQuestionData singleChoiceQuestion() {
        return SingleChoiceQuestionData.builder()
                .questionId("q-single")
                .questionText("Vyber jednu")
                .type(QuestionTypeEnum.SINGLE_CHOICE)
                .points(1)
                .options(List.of("Femur", "Tibia"))
                .build();
    }

    private MultipleChoiceQuestionData multipleChoiceQuestion() {
        return MultipleChoiceQuestionData.builder()
                .questionId("q-multi")
                .questionText("Vyber více")
                .type(QuestionTypeEnum.MULTIPLE_CHOICE)
                .points(2)
                .options(List.of("Femur", "Tibia", "Ulna"))
                .build();
    }

    private OpenTextQuestionData openTextQuestion() {
        return OpenTextQuestionData.builder()
                .questionId("q-open")
                .questionText("Napiš kost")
                .type(QuestionTypeEnum.OPEN_TEXT)
                .points(1)
                .build();
    }

    private MatchingQuestionData matchingQuestion() {
        return MatchingQuestionData.builder()
                .questionId("q-match")
                .questionText("Spáruj")
                .type(QuestionTypeEnum.MATCHING)
                .points(3)
                .leftItems(List.of("Femur", "Tibia"))
                .rightItems(List.of("Holenní kost", "Stehenní kost"))
                .build();
    }

    private OrderingQuestionData orderingQuestion() {
        return OrderingQuestionData.builder()
                .questionId("q-order")
                .questionText("Seřaď")
                .type(QuestionTypeEnum.ORDERING)
                .points(2)
                .items(List.of("Lebka", "Páteř", "Pánev"))
                .build();
    }

    private TextureClickQuestionData textureClickQuestion() {
        return TextureClickQuestionData.builder()
                .questionId("q-texture")
                .questionText("Klikni")
                .type(QuestionTypeEnum.TEXTURE_CLICK)
                .points(4)
                .modelId("model-1")
                .textureId("texture-1")
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T typed(Object value) {
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> typedList(List<?> values) {
        return (List<T>) values;
    }
}
