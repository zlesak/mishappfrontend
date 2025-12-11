package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.*;
import cz.uhk.zlesak.threejslearningapp.components.forms.QuizForm;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.InfoNotification;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.services.TextureService;
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

/**
 * View for creating new quizzes.
 */
@Slf4j
@Route("createQuiz")
@Tag("create-quiz")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class QuizCreateView extends AbstractQuizView {
    private final QuizService quizService;
    private final ModelService modelService;
    private final TextureService textureService;
    private final QuizForm quizForm;
    private final List<QuestionEditorBase<?>> questionEditors = new ArrayList<>();

    @Autowired
    public QuizCreateView(QuizService quizService, ModelService modelService, TextureService textureService) {
        super("page.title.createQuizView", false);
        this.quizService = quizService;
        this.modelService = modelService;
        this.textureService = textureService;

        quizForm = new QuizForm();
        quizForm.setAddQuestionListener(this::addQuestion);
        quizForm.setAccordionOpenedChangeListener(this::onAccordionPanelOpened);

        quizForm.setHeightFull();
        quizForm.getScroller().setHeightFull();
        quizForm.getScroller().getStyle().set("overflow", "auto");
        entityContent.add(quizForm);
    }

    /**
     * Handles when an accordion panel is opened.
     *
     * @param component The opened component
     */
    private void onAccordionPanelOpened(Component component) {
        if (component instanceof TextureClickQuestionEditor textureEditor) {
            QuickModelEntity selectedModel = textureEditor.getSelectedModel();
            if (selectedModel != null) {
                modelDiv.renderer.showModel(selectedModel.getModel().getId());
            }
        }
    }

    private void addQuestion(QuestionTypeEnum questionType) {
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
    }

    private QuestionEditorBase<?> createQuestionEditor(QuestionTypeEnum questionType) {
        return switch (questionType) {
            case SINGLE_CHOICE -> new SingleChoiceQuestionEditor();
            case MULTIPLE_CHOICE -> new MultipleChoiceQuestionEditor();
            case OPEN_TEXT -> new OpenTextQuestionEditor();
            case MATCHING -> new MatchingQuestionEditor();
            case ORDERING -> new OrderingQuestionEditor();
            case TEXTURE_CLICK -> new TextureClickQuestionEditor(modelDiv.renderer, quickModelEntity -> {
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
                quizService.addQuestion(editor.getQuestionData());
                quizService.addAnswer(editor.getAnswerData());
            }

            String quizId = quizService.create(
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
            quizService.clearQuestionsAndAnswers();
            new ErrorNotification(text("quiz.created.error") + ": " + e.getMessage(), 5000);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(UI.getCurrent(), CreateQuizEvent.class, e -> saveQuiz()));
    }

    private void loadModelsIntoRenderer(Map<String, QuickModelEntity> quickModelEntityMap) throws IOException {
        for (QuickModelEntity quickModelEntity : quickModelEntityMap.values()) {
            loadSingleModelWithTextures(quickModelEntity);
        }
    }

    private void loadSingleModelWithTextures(QuickModelEntity quickModelEntity) throws IOException {
        String modelUrl = modelService.getModelFileBeEndpointUrl(
                quickModelEntity.getModel().getId()
        );

        String textureUrl = getMainTextureUrl(quickModelEntity);
        modelDiv.renderer.loadModel(modelUrl, textureUrl, quickModelEntity.getModel().getId());

        if (textureUrl != null) {
            loadOtherTextures(quickModelEntity);
        }
    }

    private String getMainTextureUrl(QuickModelEntity quickModelEntity) {
        if (quickModelEntity.getMainTexture() != null) {
            return textureService.getTextureFileBeEndpointUrl(
                    quickModelEntity.getMainTexture().getTextureFileId()
            );
        }
        return null;
    }

    private void loadOtherTextures(QuickModelEntity quickModelEntity) throws IOException {
        List<QuickTextureEntity> allTextures = new ArrayList<>(quickModelEntity.getOtherTextures());
        Map<String, String> texturesMap = TextureMapHelper.otherTexturesMap(allTextures, textureService);
        modelDiv.renderer.addOtherTextures(texturesMap, quickModelEntity.getModel().getId());
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) { }

    @Override
    protected void afterNavigationActions() { }
}
