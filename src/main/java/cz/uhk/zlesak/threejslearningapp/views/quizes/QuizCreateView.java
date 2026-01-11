package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.*;
import cz.uhk.zlesak.threejslearningapp.components.forms.QuizForm;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.InfoNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractQuizView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * View for creating new quizzes.
 */
@Slf4j
@Route("createQuiz/:quizId?")
@Tag("create-quiz")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class QuizCreateView extends AbstractQuizView {
    private final ChapterService chapterService;
    private final QuizForm quizForm;
    private final List<QuestionEditorBase<?>> questionEditors = new ArrayList<>();

    @Autowired
    public QuizCreateView(QuizService quizService, ChapterService chapterService) {
        super("page.title.createQuizView", false, quizService);
        this.chapterService = chapterService;

        quizForm = new QuizForm();
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

    private QuestionEditorBase<?> addQuestion(QuestionTypeEnum questionType) {
        QuestionEditorBase<?> editor = createQuestionEditor(questionType);
        editor.getRemoveButton().addClickListener(e -> removeQuestion(editor));
        editor.getValidateButton().addClickListener(e -> {
            if (editor.validate()) {
                new InfoNotification(text("quiz.question.validated"));
            } else {
                new ErrorNotification(text("quiz.question.validation.failed"), 3000);
            }
        });

        questionEditors.add(editor);
        quizForm.questionsContainer.add(quizForm.getQuestionTypeLabel(questionType), editor);
        quizForm.questionsContainer.open((int) (quizForm.questionsContainer.getChildren().count() - 1));
        return editor;
    }

    private void addQuestion(AbstractQuestionData questionData, AbstractAnswerData answerData) {
        QuestionTypeEnum questionType = questionData.getType();

        QuestionEditorBase<?> editor = addQuestion(questionType);
        editor.initialize(questionData);
        editor.setAnswerData(answerData);
    }

    private QuestionEditorBase<?> createQuestionEditor(QuestionTypeEnum questionType) {
        return switch (questionType) {
            case SINGLE_CHOICE -> new SingleChoiceQuestionEditor();
            case MULTIPLE_CHOICE -> new MultipleChoiceQuestionEditor();
            case OPEN_TEXT -> new OpenTextQuestionEditor();
            case MATCHING -> new MatchingQuestionEditor();
            case ORDERING -> new OrderingQuestionEditor();
            case TEXTURE_CLICK -> new TextureClickQuestionEditor(quickModelEntity -> {
                try {
                    loadModelsIntoRenderer(quickModelEntity);
                } catch (IOException e) {
                    log.error("Error loading model with textures", e);
                    new ErrorNotification(text("model.load.error") + ": " + e.getMessage(), 3000);
                }
            });
        };
    }

    private void removeQuestion(QuestionEditorBase<?> editor) {
        questionEditors.remove(editor);
        quizForm.questionsContainer.remove(editor);
        int index = 0;
        if (quizForm.questionsContainer.getChildren().findAny().isPresent()) {
            index = (int) (quizForm.questionsContainer.getChildren().count() - 1);
        }
        quizForm.questionsContainer.open(index);
    }

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
                service.addQuestion(editor.getQuestionData());
                service.addAnswer(editor.getAnswerData());
            }

            String quizId = service.create(
                    QuizEntity.builder()
                            .name(name)
                            .description(quizForm.getDescription())
                            .timeLimit(quizForm.getTimeLimit())
                            .chapterId(quizForm.getSelectedChapter())
                            .build()
            ).getId();

            new InfoNotification(text("quiz.created.success"));
            log.info("Quiz created with ID: {}", quizId);
            skipBeforeLeaveDialog = true;
            UI.getCurrent().navigate("quiz/" + quizId);

        } catch (Exception e) {
            log.error("Error creating quiz", e);
            service.clearQuestionsAndAnswers();
            new ErrorNotification(text("quiz.created.error") + ": " + e.getMessage(), 5000);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(UI.getCurrent(), CreateQuizEvent.class, e -> saveQuiz()));
    }

    private void loadModelsIntoRenderer(Map<String, QuickModelEntity> quickModelEntityMap) throws IOException {
        for (Map.Entry<String, QuickModelEntity> entry : quickModelEntityMap.entrySet()) {
            QuickModelEntity quickModelEntity = entry.getValue();
            loadSingleModelWithTextures(quickModelEntity, entry.getKey(), null, true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        quizId = parameters.get("quizId").orElse(null);

        if (quizId != null) {
            try {
                var entity = service.getQuizWithAnswers(quizId);
                if (entity == null) {
                    log.error("Quiz not found for editing, quizId: {}", quizId);
                } else {
                    ChapterEntity chapterEntity = null;
                    if (entity.getChapterId() != null) {
                        chapterEntity = chapterService.read(entity.getChapterId());
                    }
                    quizForm.setQuizData(entity.getName(), entity.getDescription(), entity.getTimeLimit(), chapterEntity);

                    var answersMap = entity.getAnswers().stream()
                            .collect(Collectors.toMap(AbstractAnswerData::getQuestionId, answer -> answer));

                    for (var question : entity.getQuestions()) {
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
