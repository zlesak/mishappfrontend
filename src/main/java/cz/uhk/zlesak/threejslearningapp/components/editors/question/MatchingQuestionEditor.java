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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private void addAnswer(String... value) {
        int index = answerIndices.stream().max(Integer::compareTo).orElse(0) + 1;
        answerIndices.add(index);
        MatchQuestionOption optionLayout = new MatchQuestionOption(index, "quiz.matching.rightItem", indices, value);
        optionLayout.getRemoveOptionButton().addClickListener(e -> removeAnswer(optionLayout.getQuestionId()));

        answers.add(optionLayout);
        answersLayout.add(optionLayout);

        updateCorrectAnswerGroup();
    }

    @Override
    public void initialize(AbstractQuestionData questionData) {
        super.initialize(questionData);
        if (questionData instanceof MatchingQuestionData data) {
            addOptions(data.getLeftItems());
            if (data.getRightItems() != null) {
                for (String rightItem : data.getRightItems()) {
                    addAnswer(rightItem);
                }
            }
        }
    }

    @Override
    public void setAnswerData(AbstractAnswerData answerData) {
        if (answerData instanceof MatchingAnswerData data) {
            Map<Integer, Integer> matches = data.getCorrectMatches();
            if (matches != null) {
                // The stored map is keyed by the left item, while the editor asks the author to
                // pick a left item on each right one, so the pair is looked up by its value.
                for (MatchQuestionOption answer : answers) {
                    Integer rightIdx = answer.getIndex() - 1;
                    matches.entrySet().stream()
                            .filter(match -> rightIdx.equals(match.getValue()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .ifPresent(leftIdx -> answer.getOptionSelect().setValue(leftIdx + 1));
                }
            }
        }
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
        // Keyed by the left item, which is the direction the student's submission and the
        // validator's range checks both use; the editor collects the pairs the other way round
        // because the select sits on the right item.
        Map<Integer, Integer> correctMatches = new HashMap<>();
        answers.forEach(answer -> {
            Integer leftIndex = answer.getOptionSelect().getValue() - 1;
            correctMatches.put(leftIndex, answer.getIndex() - 1);
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
        Set<Integer> chosenLeftItems = new HashSet<>();
        for (MatchQuestionOption field : answers) {
            if (field.getOptionField().getValue() == null || field.getOptionField().getValue().isEmpty()) {
                return false;
            }
            if(field.getOptionSelect().getValue()  == null ) {
                return false;
            }
            // Each left item belongs to exactly one right item. Every select offers all of them, so
            // picking one twice is an easy slip, and the answer is keyed by the left item: the
            // second pair would overwrite the first and vanish without a word.
            if (!chosenLeftItems.add(field.getOptionSelect().getValue())) {
                return false;
            }
        }
        return answers.size() == options.size();
    }
}
