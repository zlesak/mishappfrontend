package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.components.buttons.CreateQuizButton;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ChapterListDialog;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.QuestionEditorBase;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Form for creating/editing quiz metadata (name, description, time limit).
 */
@Getter
public class CreateQuizForm extends VerticalLayout implements I18nAware {
    private final TextField nameField;
    private final TextArea descriptionField;
    private final IntegerField timeLimitField;
    private final Select<ChapterEntity> chapterSelect;
    private final Button chooseChapterButton;
    private final Button addQuestionButton;
    private final Select<QuestionTypeEnum> questionTypeSelect;
    public final Accordion questionsContainer;
    public final Scroller scroller;
    private final CreateQuizButton saveQuizButton;
    private final List<QuestionEditorBase<?>> questionEditors = new ArrayList<>();

    /**
     * Constructs a new QuizForm.
     */
    public CreateQuizForm() {
        super();
        setSpacing(true);
        setPadding(false);

        nameField = new TextField(text("quiz.name.label"));
        nameField.setPlaceholder(text("quiz.name.placeholder"));
        nameField.setWidthFull();
        nameField.setRequired(true);

        descriptionField = new TextArea(text("quiz.description.label"));
        descriptionField.setPlaceholder(text("quiz.description.placeholder"));
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);

        timeLimitField = new IntegerField();
        timeLimitField.setHelperText(text("quiz.timeLimit.label"));
        timeLimitField.setStepButtonsVisible(true);
        timeLimitField.setValue(0);
        timeLimitField.setMin(0);
        timeLimitField.setStep(1);

        chapterSelect = new Select<>();
        chapterSelect.setHelperText(text("quiz.chapterId.label"));
        chapterSelect.setItemLabelGenerator(ch -> ch == null ? "" : ch.getName());
        chapterSelect.setWidth("260px");
        chapterSelect.setReadOnly(true);
        chapterSelect.setWidthFull();

        chooseChapterButton = new Button(text("quiz.chapter.selectButton.label"));
        chooseChapterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        chooseChapterButton.addClickListener(e -> openChapterSelectionDialog());

        questionTypeSelect = new Select<>();
        questionTypeSelect.setHelperText(text("quiz.questionType.label"));
        questionTypeSelect.setItems(QuestionTypeEnum.values());
        questionTypeSelect.setItemLabelGenerator(this::getQuestionTypeLabel);
        questionTypeSelect.setValue(QuestionTypeEnum.SINGLE_CHOICE);
        questionTypeSelect.setWidthFull();

        addQuestionButton = new Button(text("quiz.addQuestion.label"), new Icon(VaadinIcon.PLUS));
        addQuestionButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        saveQuizButton = new CreateQuizButton();

        questionsContainer = new Accordion();
        questionsContainer.setWidthFull();

        HorizontalLayout horizontalLayout = new HorizontalLayout(timeLimitField, chapterSelect, chooseChapterButton);
        HorizontalLayout horizontalLayout2 = new HorizontalLayout(questionTypeSelect, addQuestionButton, saveQuizButton);
        horizontalLayout.setWidthFull();
        horizontalLayout2.setWidthFull();

        VerticalLayout verticalLayout = new VerticalLayout(nameField, descriptionField, horizontalLayout, questionsContainer);

        scroller = new Scroller(verticalLayout, Scroller.ScrollDirection.VERTICAL);
        scroller.setWidthFull();

        add(scroller, horizontalLayout2);
        setHeightFull();
    }

    /**
     * Opens the chapter selection dialog.
     */
    private void openChapterSelectionDialog() {
        ChapterListDialog dialog = new ChapterListDialog(new ChapterListingView());
        dialog.setBlockId("quiz-chapter-select");
        dialog.open();
    }

    /**
     * Sets the listener for adding a new question.
     *
     * @param listener Consumer that accepts the selected question type
     */
    public void setAddQuestionListener(Consumer<QuestionTypeEnum> listener) {
        addQuestionButton.addClickListener(e -> {
            QuestionTypeEnum selectedType = questionTypeSelect.getValue();
            if (selectedType != null) {
                listener.accept(selectedType);
            }
        });
    }

    /**
     * Sets the listener for when accordion panel is opened.
     *
     * @param listener Consumer that accepts the opened component
     */
    public void setAccordionOpenedChangeListener(Consumer<Component> listener) {
        questionsContainer.addOpenedChangeListener(event ->
                event.getOpenedPanel().flatMap(panel -> panel.getContent().findFirst()).ifPresent(listener)
        );
    }

    /**
     * Gets the localized label for a given question type.
     *
     * @param type The question type enum
     * @return The localized label
     */
    public String getQuestionTypeLabel(QuestionTypeEnum type) {
        return switch (type) {
            case SINGLE_CHOICE -> text("quiz.questionType.singleChoice");
            case MULTIPLE_CHOICE -> text("quiz.questionType.multipleChoice");
            case OPEN_TEXT -> text("quiz.questionType.openText");
            case MATCHING -> text("quiz.questionType.matching");
            case ORDERING -> text("quiz.questionType.ordering");
            case TEXTURE_CLICK -> text("quiz.questionType.textureClick");
        };
    }

    /**
     * Gets the quiz name.
     *
     * @return The quiz name
     */
    public String getName() {
        return nameField.getValue();
    }

    /**
     * Gets the quiz description.
     *
     * @return The quiz description
     */
    public String getDescription() {
        return descriptionField.getValue();
    }

    /**
     * Gets the time limit in minutes.
     *
     * @return The time limit
     */
    public int getTimeLimit() {
        return timeLimitField.getValue() != null ? timeLimitField.getValue() : 0;
    }

    /**
     * Gets the selected chapter.
     *
     * @return The selected chapter
     */
    public String getSelectedChapter() {
        return chapterSelect.getValue() != null ? chapterSelect.getValue().getId() : null;
    }

    public void setQuizData(String name, String description, Integer timeLimit, ChapterEntity chapter) {
        nameField.setValue(name != null ? name : "");
        descriptionField.setValue(description != null ? description : "");
        timeLimitField.setValue(timeLimit != null ? timeLimit : 0);
        if (chapter != null) {
            chapterSelect.setItems(chapter);
            chapterSelect.setValue(chapter);
        }
    }

}

