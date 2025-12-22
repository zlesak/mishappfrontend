package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.QuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OrderingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Editor for ordering questions.
 */
public class OrderingQuestionEditor extends QuestionEditorBase<QuestionOption> {

    /**
     * Constructor for OrderingQuestionEditor.
     */
    public OrderingQuestionEditor() {
        super(QuestionTypeEnum.ORDERING);
    }

    /**
     * Creates a QuestionOption.
     *
     * @param index the index of the option
     * @return the created QuestionOption
     */
    @Override
    protected QuestionOption createOption(int index) {
        return new QuestionOption(index, "quiz.option.label");
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

    /**
     * Gets the question data.
     * @return the question data
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        List<String> items = options.stream()
                .map(option -> option.getOptionField().getValue())
                .collect(Collectors.toList());

        return OrderingQuestionData.builder()
                .questionId(questionId)
                .questionText(getQuestionText())
                .type(questionType)
                .points(getPoints())
                .items(items)
                .build();
    }

    /**
     * Gets the answer data.
     * @return the answer data
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        List<Integer> correctOrder = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            correctOrder.add(i);
        }

        return OrderingAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .correctOrder(correctOrder)
                .build();
    }

    /**
     * Validates the question.
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (options.size() < 2) {
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

