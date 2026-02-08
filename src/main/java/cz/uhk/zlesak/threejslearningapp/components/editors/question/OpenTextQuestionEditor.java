package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.checkbox.Checkbox;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.QuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Editor for open text questions.
 */
public class OpenTextQuestionEditor extends QuestionEditorBase<QuestionOption> {
    private final Checkbox exactMatchCheckbox;

    /**
     * Constructor for OpenTextQuestionEditor.
     */
    public OpenTextQuestionEditor() {
        super(QuestionTypeEnum.OPEN_TEXT);

        exactMatchCheckbox = new Checkbox(text("quiz.openText.exactMatch"));
        exactMatchCheckbox.setValue(true);

        actionsLayout.addComponentAtIndex(1, exactMatchCheckbox);
    }

    /**
     * Creates a QuestionOption.
     *
     * @param index the index of the option
     * @return the created QuestionOption
     */
    @Override
    protected QuestionOption createOption(int index, String... value) {
        return new QuestionOption(index, "quiz.option.label", value);
    }

    /**
     * Updates the correct answer group.
     */
    @Override
    void updateCorrectAnswerGroup() {
        indices.clear();
        for (QuestionOption option : options) {
            int index = indices.stream().max(Integer::compareTo).orElse(0) + 1;
            indices.add(index);
            option.update(index);
        }
    }

    @Override
    public void initialize(AbstractQuestionData questionData) {
        super.initialize(questionData);
    }

    @Override
    public void setAnswerData(AbstractAnswerData answerData) {
        if (answerData instanceof OpenTextAnswerData data) {
            exactMatchCheckbox.setValue(data.getExactMatch());
            addOptions(data.getAcceptableAnswers());
        }
    }

    /**
     * Gets the question data for OpenTextQuestion as OpenTextQuestionData.
     *
     * @return OpenTextQuestionData
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        return OpenTextQuestionData.builder()
                .questionId(questionId)
                .placeholder("")
                .questionText(getQuestionText())
                .type(questionType)
                .points(getPoints())
                .build();
    }

    /**
     * Gets the answer data for OpenTextQuestion as OpenTextAnswerData.
     *
     * @return OpenTextAnswerData
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        List<String> acceptableAnswers = options.stream()
                .map(option -> option.getOptionField().getValue())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());

        return OpenTextAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .acceptableAnswers(acceptableAnswers)
                .exactMatch(exactMatchCheckbox.getValue())
                .build();
    }

    /**
     * Validates the question data.
     *
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (options.isEmpty()) {
            return false;
        }
        for (QuestionOption field : options) {
            if (field.getOptionField().getValue() == null || field.getOptionField().getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
