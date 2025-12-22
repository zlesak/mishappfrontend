package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.AbstractOption;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base editor for all question types.
 * Provides common fields and methods for editing questions.
 */
@Getter
public abstract class QuestionEditorBase<QO extends AbstractOption> extends VerticalLayout implements I18nAware {
    protected final String questionId;
    protected final QuestionTypeEnum questionType;
    protected final TextArea questionTextField;
    protected final IntegerField pointsField;
    protected final Button removeButton, validateButton, addOptionButton;
    protected final VerticalLayout optionsLayout;
    protected final HorizontalLayout actionsLayout;

    protected final List<QO> options = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();

    /**
     * Constructor for QuestionEditorBase.
     *
     * @param questionType the type of the question
     */
    protected QuestionEditorBase(QuestionTypeEnum questionType) {
        super();
        setPadding(true);
        setSpacing(true);
        this.questionId = UUID.randomUUID().toString();
        this.questionType = questionType;
        addClassName("question-editor");
        getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        getStyle().set("border-radius", "var(--lumo-border-radius-m)");


        questionTextField = new TextArea(text("quiz.question.text"));
        questionTextField.setPlaceholder(text("quiz.question.placeholder"));
        questionTextField.setWidthFull();
        questionTextField.setRequired(true);

        pointsField = new IntegerField();
        pointsField.setHelperText(text("quiz.question.points"));
        pointsField.setValue(1);
        pointsField.setMin(1);
        pointsField.setStepButtonsVisible(true);
        pointsField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        optionsLayout = new VerticalLayout();
        optionsLayout.setSpacing(true);
        optionsLayout.setPadding(false);

        validateButton = new Button(new Icon(VaadinIcon.CHECK));
        validateButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        removeButton = new Button(new Icon(VaadinIcon.TRASH));
        removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        addOptionButton = new Button(text("quiz.singleChoice.addOption"), new Icon(VaadinIcon.PLUS_CIRCLE));
        addOptionButton.addClickListener(e -> addOption());
        addOptionButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Span spacer = new Span();
        spacer.setWidthFull();

        actionsLayout = new HorizontalLayout(pointsField, addOptionButton, spacer, validateButton, removeButton);
        actionsLayout.setSpacing(true);
        actionsLayout.setWidthFull();

        add(questionTextField, actionsLayout, optionsLayout);
    }

    /**
     * Adds an option to the question.
     */
    void addOption() {
        int index = indices.stream().max(Integer::compareTo).orElse(0) + 1;
        indices.add(index);
        QO optionLayout = createOption(index);
        optionLayout.getRemoveOptionButton().addClickListener(e -> removeOption(optionLayout.getQuestionId()));

        options.add(optionLayout);
        optionsLayout.add(optionLayout);

        updateCorrectAnswerGroup();
    }

    /**
     * Creates a QuestionOption.
     *
     * @param index the index of the option
     * @return the created QuestionOption
     */
    protected abstract QO createOption(int index);

    /**
     * Removes an option from the question.
     */
    void removeOption(String id) {
        options.stream().filter(option -> option.getQuestionId().equals(id)).findFirst().ifPresent(field -> {
            optionsLayout.remove(field);
            options.remove(field);
            indices.remove(field.getIndex() - 1);
            updateCorrectAnswerGroup();
        });
    }

    /**
     * Updates the correct answer group based on current options.
     */
    abstract void updateCorrectAnswerGroup();

    /**
     * Gets the question data from this editor.
     *
     * @return QuestionData object
     */
    public abstract AbstractQuestionData getQuestionData();

    /**
     * Gets the answer data from this editor.
     *
     * @return AnswerData object
     */
    public abstract AbstractAnswerData getAnswerData();

    /**
     * Validates the question data.
     *
     * @return true if valid, false otherwise
     */
    public abstract boolean validate();

    /**
     * Gets the question text.
     *
     * @return question text as String
     */
    protected String getQuestionText() {
        return questionTextField.getValue();
    }

    /**
     * Gets the points assigned to the question.
     *
     * @return points as Integer
     */
    protected Integer getPoints() {
        return pointsField.getValue();
    }
}

