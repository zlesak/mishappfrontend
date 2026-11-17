package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.service.FileStorageService;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizAuthoringValidator;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizGradingService;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers.AbstractQuestionRendererComponent;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MatchingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives a question from the real editor, through the real validator, into the real grading
 * service and back out through the real renderer the student answers in.
 *
 * <p>The existing suites stop at each layer's own edge: the editor tests assert what the editor
 * produces, the backend tests feed the validator and grading hand-written data. Several index
 * conventions disagreed across that seam without a single test noticing, because the hand-written
 * data happened to use the backend's convention rather than the editor's. These tests exist to
 * hold the two ends together.
 */
class QuizEditorBackendContractTest {

    private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
    private final QuizAuthoringValidator validator = new QuizAuthoringValidator(fileStorageService);
    private final QuizGradingService grading = new QuizGradingService();

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void acceptsTheLastMultipleChoiceOptionTheEditorOffers() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Která je poslední?");
        editor.addOptions(List.of("a", "b", "c"));
        selectInEditor(editor, 3);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();

        assertThatCode(() -> validate(question, answer)).doesNotThrowAnyException();
    }

    @Test
    void gradesTheLastMultipleChoiceOptionAsCorrectWhenTheStudentPicksIt() {
        MultipleChoiceQuestionEditor editor = new MultipleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Která je poslední?");
        editor.addOptions(List.of("a", "b", "c"));
        selectInEditor(editor, 3);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);

        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        CheckboxGroup<Integer> boxes = intCheckboxGroup(VaadinTestSupport.findFirst(renderer, CheckboxGroup.class));
        boxes.setValue(Set.of(2));

        assertThat(score(question, answer, renderer.getSubmissionData())).isEqualTo(question.getPoints());
    }

    @Test
    void gradesAMatchingPairingThatIsNotItsOwnInverse() {
        // L0-R1, L1-R2, L2-R0: deliberately not an involution, so an inverted map scores zero
        // instead of quietly passing the way a symmetric pairing would.
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        editor.initialize(MatchingQuestionData.builder()
                .questionText("Spoj dvojice")
                .type(QuestionTypeEnum.MATCHING)
                .points(5)
                .leftItems(List.of("L0", "L1", "L2"))
                .rightItems(List.of("R0", "R1", "R2"))
                .build());

        // One select per right item, each naming the left item it belongs to, 1-based in the UI.
        List<Select<Integer>> rightItemSelects = editorSelects(editor);
        rightItemSelects.get(0).setValue(3);
        rightItemSelects.get(1).setValue(1);
        rightItemSelects.get(2).setValue(2);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);

        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        List<Select<Integer>> leftItemSelects = VaadinTestSupport.findAll(renderer, Select.class).stream()
                .map(QuizEditorBackendContractTest::intSelect)
                .toList();
        leftItemSelects.get(0).setValue(1);
        leftItemSelects.get(1).setValue(2);
        leftItemSelects.get(2).setValue(0);

        assertThat(score(question, answer, renderer.getSubmissionData())).isEqualTo(5);
    }

    @Test
    void refusesAMatchingQuestionThatUsesTheSameLeftItemTwice() {
        // The answer key is keyed by the left item, so a left item chosen for two right items
        // would overwrite its own earlier pair and leave a question nobody can answer correctly.
        MatchingQuestionEditor editor = matchingEditor(List.of("L0", "L1", "L2"), List.of("R0", "R1", "R2"));
        List<Select<Integer>> rightItemSelects = editorSelects(editor);
        rightItemSelects.get(0).setValue(1);
        rightItemSelects.get(1).setValue(2);
        rightItemSelects.get(2).setValue(1);

        assertThat(editor.validate()).isFalse();
        // The backend's wording never reaches the author, because the editor refuses first; what
        // the editor says has to stand on its own.
        assertThat(editor.validationMessage()).contains("jen jednou");

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        assertThatThrownBy(() -> validate(question, answer))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("levého sloupce právě jednou");
    }

    @Test
    void refusesAMatchingQuestionWhoseColumnsAreOfDifferentLengths() {
        // Three left items but two right ones: the student is asked for three pairs and the key
        // can only hold two, so the question can never be answered correctly.
        MatchingQuestionEditor editor = matchingEditor(List.of("L0", "L1", "L2"), List.of("R0", "R1"));
        List<Select<Integer>> rightItemSelects = editorSelects(editor);
        rightItemSelects.get(0).setValue(1);
        rightItemSelects.get(1).setValue(2);

        assertThat(editor.validate()).isFalse();
        assertThat(editor.validationMessage()).contains("stejný počet položek");

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        assertThatThrownBy(() -> validate(question, answer))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("levého sloupce právě jednou");
    }

    @Test
    void saysNothingMoreWhenAMatchingQuestionIsFine() {
        MatchingQuestionEditor editor = matchingEditor(List.of("L0", "L1", "L2"), List.of("R0", "R1", "R2"));
        List<Select<Integer>> rightItemSelects = editorSelects(editor);
        rightItemSelects.get(0).setValue(3);
        rightItemSelects.get(1).setValue(1);
        rightItemSelects.get(2).setValue(2);

        assertThat(editor.validationMessage()).isNull();
    }

    @Test
    void tellsTheAuthorWhichPairingRuleWasBroken() {
        // Every message the editor can give is a sentence the author can act on, not a key and
        // not the word "invalid".
        MatchingQuestionEditor missingPair = matchingEditor(List.of("L0", "L1"), List.of("R0", "R1"));

        assertThat(missingPair.validationMessage())
                .isNotNull()
                .doesNotStartWith("quiz.")
                .contains("vyberte položku vlevo");
    }

    @Test
    void acceptsAMatchingQuestionThatPairsEveryItemExactlyOnce() {
        MatchingQuestionEditor editor = matchingEditor(List.of("L0", "L1", "L2"), List.of("R0", "R1", "R2"));
        List<Select<Integer>> rightItemSelects = editorSelects(editor);
        rightItemSelects.get(0).setValue(3);
        rightItemSelects.get(1).setValue(1);
        rightItemSelects.get(2).setValue(2);

        assertThat(editor.validate()).isTrue();
    }

    private MatchingQuestionEditor matchingEditor(List<String> leftItems, List<String> rightItems) {
        MatchingQuestionEditor editor = new MatchingQuestionEditor();
        editor.initialize(MatchingQuestionData.builder()
                .questionText("Spoj dvojice")
                .type(QuestionTypeEnum.MATCHING)
                .points(5)
                .leftItems(leftItems)
                .rightItems(rightItems)
                .build());
        return editor;
    }

    @Test
    void keepsQuestionIdsWhenTheAuthorSavesTheQuizAgain() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Otázka");
        editor.addOptions(List.of("a", "b"));
        intSelect(VaadinTestSupport.findFirst(editor, Select.class)).setValue(2);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);
        String idHandedToTheStudent = question.getQuestionId();

        // The author saves an edit while a student already has the quiz open.
        validate(question, answer);

        assertThat(question.getQuestionId()).isEqualTo(idHandedToTheStudent);
        assertThat(answer.getQuestionId()).isEqualTo(idHandedToTheStudent);
    }

    @Test
    void stillGradesAnAttemptStartedBeforeTheQuizWasEdited() {
        SingleChoiceQuestionEditor editor = new SingleChoiceQuestionEditor();
        editor.getQuestionTextField().setValue("Otázka");
        editor.addOptions(List.of("a", "b"));
        intSelect(VaadinTestSupport.findFirst(editor, Select.class)).setValue(2);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);

        // The student's player was built from the quiz as it stood a moment ago.
        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        intRadioGroup(VaadinTestSupport.findFirst(renderer, RadioButtonGroup.class)).setValue(1);
        AbstractSubmissionData submitted = renderer.getSubmissionData();

        validate(question, answer);

        assertThat(score(question, answer, submitted)).isEqualTo(question.getPoints());
    }

    @Test
    void keepsTheAuthorsCapitalisationOfAnAcceptedOpenAnswer() {
        OpenTextQuestionEditor editor = openTextEditor("Mozek", true);

        AbstractQuestionData question = editor.getQuestionData();
        OpenTextAnswerData answer = (OpenTextAnswerData) editor.getAnswerData();
        validate(question, answer);

        assertThat(answer.getAcceptableAnswers()).containsExactly("Mozek");
    }

    @Test
    void marksAnExactOpenAnswerWrongWhenTheCapitalisationDiffers() {
        OpenTextQuestionEditor editor = openTextEditor("Mozek", true);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);

        assertThat(score(question, answer, openTextSubmission(question, "mozek"))).isZero();
        assertThat(score(question, answer, openTextSubmission(question, " Mozek ")))
                .isEqualTo(question.getPoints());
    }

    @Test
    void ignoresCapitalisationOfAnOpenAnswerTheAuthorDidNotMarkExact() {
        OpenTextQuestionEditor editor = openTextEditor("Mozek", false);

        AbstractQuestionData question = editor.getQuestionData();
        AbstractAnswerData answer = editor.getAnswerData();
        validate(question, answer);

        assertThat(score(question, answer, openTextSubmission(question, "mozek")))
                .isEqualTo(question.getPoints());
    }

    private OpenTextQuestionEditor openTextEditor(String acceptedAnswer, boolean exactMatch) {
        OpenTextQuestionEditor editor = new OpenTextQuestionEditor();
        editor.getQuestionTextField().setValue("Co to je?");
        editor.addOptions(List.of(acceptedAnswer));
        VaadinTestSupport.findFirst(editor, Checkbox.class).setValue(exactMatch);
        return editor;
    }

    private AbstractSubmissionData openTextSubmission(AbstractQuestionData question, String typedText) {
        AbstractQuestionRendererComponent renderer = AbstractQuestionRendererComponent.create(question);
        VaadinTestSupport.findFirst(renderer, TextArea.class)
                .setValue(typedText);
        return renderer.getSubmissionData();
    }

    private void validate(AbstractQuestionData question, AbstractAnswerData answer) {
        validator.validate(new ArrayList<>(List.of(question)), new ArrayList<>(List.of(answer)));
    }

    private int score(AbstractQuestionData question, AbstractAnswerData answer, AbstractSubmissionData submission) {
        QuizEntity quiz = QuizEntity.builder()
                .id("quiz-1")
                .name("Kvíz")
                .questions(new ArrayList<>(List.of(question)))
                .answers(new ArrayList<>(List.of(answer)))
                .build();
        return grading.grade(quiz, List.of(submission)).totalScore();
    }

    private void selectInEditor(MultipleChoiceQuestionEditor editor, Integer... oneBasedOptions) {
        MultiSelectComboBox<Integer> correct = intComboBox(VaadinTestSupport.findFirst(editor, MultiSelectComboBox.class));
        correct.setValue(Set.of(oneBasedOptions));
    }

    private List<Select<Integer>> editorSelects(MatchingQuestionEditor editor) {
        return VaadinTestSupport.findAll(editor, Select.class).stream()
                .map(QuizEditorBackendContractTest::intSelect)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Select<Integer> intSelect(Select<?> select) {
        return (Select<Integer>) select;
    }

    @SuppressWarnings("unchecked")
    private static CheckboxGroup<Integer> intCheckboxGroup(CheckboxGroup<?> group) {
        return (CheckboxGroup<Integer>) group;
    }

    @SuppressWarnings("unchecked")
    private static MultiSelectComboBox<Integer> intComboBox(MultiSelectComboBox<?> comboBox) {
        return (MultiSelectComboBox<Integer>) comboBox;
    }

    @SuppressWarnings("unchecked")
    private static RadioButtonGroup<Integer> intRadioGroup(RadioButtonGroup<?> group) {
        return (RadioButtonGroup<Integer>) group;
    }
}
