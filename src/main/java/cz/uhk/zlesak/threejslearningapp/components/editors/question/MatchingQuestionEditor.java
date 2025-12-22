package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.MatchQuestionOption;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.QuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MatchingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MatchingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Editor for matching questions.
 */
public class MatchingQuestionEditor extends QuestionEditorBase<QuestionOption> {
    private final List<MatchQuestionOption> answers = new ArrayList<>();
    List<Integer> answerIndices = new ArrayList<>();
    protected final VerticalLayout answersLayout = new VerticalLayout();
    protected final Accordion qAaAccordion = new Accordion();

    /**
     * Constructor for MatchingQuestionEditor.
     */
    public MatchingQuestionEditor() {
        super(QuestionTypeEnum.MATCHING);


        Button addRightItemButton = new Button(text("quiz.matching.addRightItem"), new Icon(VaadinIcon.PLUS_CIRCLE));
        addRightItemButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addRightItemButton.addClickListener(e -> addAnswer());

        answersLayout.setPadding(false);

        qAaAccordion.setWidthFull();
        qAaAccordion.add(new AccordionPanel(new Span(text("quiz.matching.leftItem")), optionsLayout));
        qAaAccordion.add(new AccordionPanel(new Span(text("quiz.matching.rightItem")), answersLayout));

        actionsLayout.addComponentAtIndex(2, addRightItemButton);

        add(qAaAccordion);
        updateCorrectAnswerGroup();
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

        answerIndices.clear();
        for (MatchQuestionOption answer : answers) {
            int index = answerIndices.stream().max(Integer::compareTo).orElse(0) + 1;
            answerIndices.add(index);
            answer.update(index, indices);
        }
    }

    /**
     * Adds an answer to the question.
     */
    private void addAnswer() {
        int index = answerIndices.stream().max(Integer::compareTo).orElse(0) + 1;
        answerIndices.add(index);
        MatchQuestionOption optionLayout = new MatchQuestionOption(index, "quiz.matching.rightItem", indices);
        optionLayout.getRemoveOptionButton().addClickListener(e -> removeAnswer(optionLayout.getQuestionId()));

        answers.add(optionLayout);
        answersLayout.add(optionLayout);

        updateCorrectAnswerGroup();
    }

    /**
     * Removes an answer from the question.
     * @param id the ID of the answer to remove
     */
    void removeAnswer(String id) {
        answers.stream().filter(answers -> answers.getQuestionId().equals(id)).findFirst().ifPresent(field -> {
            answersLayout.remove(field);
            answers.remove(field);
            answerIndices.remove(field.getIndex()-1);
            updateCorrectAnswerGroup();
        });
    }

    /**
     * Gets the question data for MatchingQuestion as MatchingQuestionData.
     * @return MatchingQuestionData
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        List<String> leftItems = options.stream()
                .map(option -> option.getOptionField().getValue())
                .collect(Collectors.toList());

        List<String> rightItems = answers.stream()
                .map(option -> option.getOptionField().getValue())
                .collect(Collectors.toList());

        return MatchingQuestionData.builder()
                .questionId(questionId)
                .questionText(getQuestionText())
                .type(questionType)
                .points(getPoints())
                .leftItems(leftItems)
                .rightItems(rightItems)
                .build();
    }

    /**
     * Gets the answer data for MatchingQuestion as MatchingAnswerData.
     * @return MatchingAnswerData
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        Map<Integer, Integer> correctMatches = new HashMap<>();
        answers.forEach(answer -> {
            Integer rightIndex = answer.getOptionSelect().getValue() - 1;
            correctMatches.put(answer.getIndex() - 1, rightIndex);
        });

        return MatchingAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .correctMatches(correctMatches)
                .build();
    }

    /**
     * Validates the question data.
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (options.size() < 2 || answerIndices.size() < 2) {
            return false;
        }
        for (QuestionOption field : options) {
            if (field.getOptionField().getValue() == null || field.getOptionField().getValue().isEmpty()) {
                return false;
            }
        }
        for (MatchQuestionOption field : answers) {
            if (field.getOptionField().getValue() == null || field.getOptionField().getValue().isEmpty()) {
                return false;
            }
            if(field.getOptionSelect().getValue()  == null ) {
                return false;
            }
        }
        return true;
    }
}

