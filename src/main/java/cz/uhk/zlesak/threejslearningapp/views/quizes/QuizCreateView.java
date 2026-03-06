package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.*;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateQuizForm;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ChapterSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View for creating new quizzes or editing existing ones.
 */
@Slf4j
@Route("createQuiz/:quizId?")
@Tag("create-quiz")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class QuizCreateView extends AbstractQuizView {
    private final ChapterService chapterService;
    private final CreateQuizForm quizForm;
    private final List<QuestionEditorBase<?>> questionEditors = new ArrayList<>();
    private boolean isEditMode = false;
    private QuizEntity loadedQuiz;
    private final ModelService modelService;

    /**
     * Constructor for QuizCreateView.
     *
     * @param quizService    quiz service for handling quiz operations
     * @param chapterService chapter service for handling chapter operations
     */
    @Autowired
    public QuizCreateView(QuizService quizService, ChapterService chapterService, ModelService modelService) {
        super("page.title.createQuizView", false, quizService);
        this.chapterService = chapterService;
        this.modelService = modelService;

        quizForm = new CreateQuizForm();
        quizForm.setAddQuestionListener(this::addQuestion);
        quizForm.setAccordionOpenedChangeListener(this::onAccordionPanelOpened);

        quizForm.setHeightFull();
        quizForm.getScroller().setHeightFull();
        quizForm.getScroller().getStyle().set("overflow", "auto");
        entityContent.add(quizForm);

        modelDiv.modelTextureAreaSelectContainer.setEnabled(false);
    }

    /**
     * Handles when an accordion panel is opened.
     *
     * @param component The opened component
     */
    private void onAccordionPanelOpened(Component component) {
        if (component instanceof TextureClickQuestionEditor textureEditor) {
            String selectedModel = textureEditor.getSelectedModelId();
            String selectedTexture = textureEditor.getSelectedTextureId();
            String selectedArea = textureEditor.getSelectedAreaId();
            if (selectedModel != null) {
                ComponentUtil.fireEvent(UI.getCurrent(),
                        new ThreeJsActionEvent(UI.getCurrent(), selectedModel, selectedTexture, ThreeJsActions.APPLY_MASK_TO_TEXTURE, true, textureEditor.getQuestionId(), selectedArea));
            }
        }
    }

    /**
     * Adds a new question of the specified type.
     *
     * @param questionType The type of question to add
     * @return The created QuestionEditorBase instance
     */
    private QuestionEditorBase<?> addQuestion(QuestionTypeEnum questionType) {
        QuestionEditorBase<?> editor = createQuestionEditor(questionType);
        editor.getRemoveButton().addClickListener(e -> removeQuestion(editor));
        editor.getValidateButton().addClickListener(e -> {
            if (editor.validate()) {
                new SuccessNotification(text("quiz.question.validated"));
            } else {
                new ErrorNotification(text("quiz.question.validation.failed"));
            }
        });

        questionEditors.add(editor);
        quizForm.questionsContainer.add(quizForm.getQuestionTypeLabel(questionType), editor);
        quizForm.questionsContainer.open((int) (quizForm.questionsContainer.getChildren().count() - 1));
        return editor;
    }

    /**
     * Adds a question with the provided data and answer data.
     *
     * @param questionData The question data
     * @param answerData   The answer data
     */
    private void addQuestion(AbstractQuestionData questionData, AbstractAnswerData answerData) {
        QuestionTypeEnum questionType = questionData.getType();

        QuestionEditorBase<?> editor = addQuestion(questionType);
        editor.initialize(questionData);
        editor.setAnswerData(answerData);
    }

    /**
     * Creates a QuestionEditorBase instance based on the question type.
     *
     * @param questionType The type of question
     * @return The created QuestionEditorBase instance
     */
    private QuestionEditorBase<?> createQuestionEditor(QuestionTypeEnum questionType) {
        return switch (questionType) {
            case SINGLE_CHOICE -> new SingleChoiceQuestionEditor();
            case MULTIPLE_CHOICE -> new MultipleChoiceQuestionEditor();
            case OPEN_TEXT -> new OpenTextQuestionEditor();
            case MATCHING -> new MatchingQuestionEditor();
            case ORDERING -> new OrderingQuestionEditor();
            case TEXTURE_CLICK -> new TextureClickQuestionEditor();
        };
    }

    /**
     * Removes the specified question editor.
     *
     * @param editor The question editor to remove
     */
    private void removeQuestion(QuestionEditorBase<?> editor) {
        questionEditors.remove(editor);
        quizForm.questionsContainer.remove(editor);
        int index = 0;
        if (quizForm.questionsContainer.getChildren().findAny().isPresent()) {
            index = (int) (quizForm.questionsContainer.getChildren().count() - 1);
        }
        quizForm.questionsContainer.open(index);
    }

    /**
     * Saves the quiz by validating input and creating or updating the quiz entity.
     */
    private void saveQuiz() {
        try {
            String name = quizForm.getName();
            if (name == null || name.isEmpty()) {
                throw new ApplicationContextException(text("quiz.validation.name.required"));
            }

            if (questionEditors.isEmpty()) {
                throw new ApplicationContextException(text("quiz.validation.questions.required"));
            }

            for (QuestionEditorBase<?> editor : questionEditors) {
                if (!editor.validate()) {
                    throw new ApplicationContextException(text("quiz.validation.question.invalid"));
                }
            }

            List<AbstractQuestionData> questionData = questionEditors.stream()
                    .map(QuestionEditorBase::getQuestionData)
                    .collect(Collectors.toList());
            List<AbstractAnswerData> answerData = questionEditors.stream()
                    .map(QuestionEditorBase::getAnswerData)
                    .collect(Collectors.toList());

            String savedQuizId = service.saveQuiz(
                    quizId,
                    isEditMode,
                    loadedQuiz,
                    name,
                    quizForm.getDescription(),
                    quizForm.getTimeLimit(),
                    quizForm.getSelectedChapter(),
                    questionData,
                    answerData
            );

            if (isEditMode) {
                new SuccessNotification(text("quiz.update.success"));
                log.info("Quiz updated with ID: {}", savedQuizId);
            } else {
                new SuccessNotification(text("quiz.create.success"));
                log.info("Quiz created with ID: {}", savedQuizId);
            }
            skipBeforeLeaveDialog = true;
            UI.getCurrent().navigate(QuizListingView.class);

        } catch (Exception e) {
            log.error("Error saving quiz", e);
            service.clearQuestionsAndAnswers();
            new ErrorNotification(text(isEditMode ? "quiz.update.error" : "quiz.create.error") + ": " + e.getMessage());
        }
    }

    /**
     * Overridden onAttach function to set up event listeners when the component is attached.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(UI.getCurrent(), CreateQuizEvent.class, e -> saveQuiz()));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelSelectedFromDialogEvent.class,
                event -> {
                    QuickModelEntity quickModelEntity = modelService.read(event.getSelectedModel().getMetadataId());
                    loadSingleModelWithTextures(quickModelEntity, event.getBlockId(), event.getSelectedModel().getModel().getId(), true);
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ChapterSelectedFromDialogEvent.class,
                event -> {
                    if ("quiz-chapter-select".equals(event.getBlockId())) {
                        quizForm.getChapterSelect().setItems(event.getSelectedChapter());
                        quizForm.getChapterSelect().setValue(event.getSelectedChapter());
                    }
                }
        ));
    }

    /**
     * Overridden beforeEnter function to load quiz data if quizId parameter is present.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        quizId = parameters.get("quizId").orElse(null);

        if (quizId != null) {
            isEditMode = true;
            
            try {
                loadedQuiz = service.getQuizWithAnswers(quizId);
                if (loadedQuiz == null) {
                    log.error("Quiz not found for editing, quizId: {}", quizId);
                } else {
                    ChapterEntity chapterEntity = null;
                    if (loadedQuiz.getChapterId() != null) {
                        chapterEntity = chapterService.read(loadedQuiz.getChapterId());
                    }
                    quizForm.setQuizData(loadedQuiz.getName(), loadedQuiz.getDescription(), loadedQuiz.getTimeLimit(), chapterEntity);

                    var answersMap = loadedQuiz.getAnswers().stream()
                            .collect(Collectors.toMap(AbstractAnswerData::getQuestionId, answer -> answer));

                    for (var question : loadedQuiz.getQuestions()) {
                        var answer = answersMap.get(question.getQuestionId());
                        addQuestion(question, answer);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting quiz for editing, quizId: {}", quizId, e);
                skipBeforeLeaveDialog = true;
                throw new NotFoundException("Error initializing quiz editing");
            }
        }
    }
}
