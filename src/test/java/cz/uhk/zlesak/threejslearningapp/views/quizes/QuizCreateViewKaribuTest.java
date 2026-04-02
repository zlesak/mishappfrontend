package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.*;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateQuizForm;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class QuizCreateViewKaribuTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private ChapterService chapterService;

    @MockitoBean
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void createModeView_shouldHaveEmptyQuestionEditorList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        List<?> editors = getField(view, "questionEditors", List.class);
        assertNotNull(editors);
        assertTrue(editors.isEmpty());
    }

    @Test
    void createModeView_shouldNotEnterEditModeWhenNoQuizIdParam() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters());
        view.beforeEnter(event);

        assertFalse(getField(view, "isEditMode", Boolean.class));
        verify(quizService, never()).getQuizWithAnswers(any());
    }

    @Test
    void createModeView_afterNavigationShouldBeNoopWhenNotInEditMode() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.afterNavigation(null));
        verify(quizService, never()).getQuizWithAnswers(any());
    }

    @Test
    void saveQuiz_inEditModeBeforeDataLoaded_shouldShowError() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        setField(view, "isEditMode", true);
        setField(view, "quizEditLoaded", false);

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addQuestion_singleChoiceType_shouldAddEditorToList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.SINGLE_CHOICE);

        assertNotNull(editor);
        assertInstanceOf(SingleChoiceQuestionEditor.class, editor);
        List<?> editors = getField(view, "questionEditors", List.class);
        assertEquals(1, editors.size());
    }

    @Test
    void addQuestion_multipleChoiceType_shouldAddEditorToList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.MULTIPLE_CHOICE);

        assertNotNull(editor);
        assertInstanceOf(MultipleChoiceQuestionEditor.class, editor);
    }

    @Test
    void addQuestion_matchingType_shouldAddEditorToList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.MATCHING);

        assertNotNull(editor);
        assertInstanceOf(MatchingQuestionEditor.class, editor);
    }

    @Test
    void addQuestion_orderingType_shouldAddEditorToList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.ORDERING);

        assertNotNull(editor);
        assertInstanceOf(OrderingQuestionEditor.class, editor);
    }

    @Test
    void addQuestion_openTextType_shouldAddEditorToList() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);

        assertNotNull(editor);
        assertInstanceOf(OpenTextQuestionEditor.class, editor);
    }

    @Test
    void removeQuestion_shouldDecreaseEditorListSize() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);
        List<?> editors = getField(view, "questionEditors", List.class);
        assertEquals(1, editors.size());

        invokeWithResult(view, "removeQuestion", new Class[]{QuestionEditorBase.class}, editor);

        assertEquals(0, editors.size());
    }

    @Test
    void removeQuestion_shouldLeaveRemainingEditors_whenMultiplePresent() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor1 = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);
        invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.SINGLE_CHOICE);

        invokeWithResult(view, "removeQuestion", new Class[]{QuestionEditorBase.class}, editor1);

        List<?> editors = getField(view, "questionEditors", List.class);
        assertEquals(1, editors.size());
    }

    @Test
    void onAccordionPanelOpened_withNonTextureEditor_shouldNotEmitAction() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        List<Object> events = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, events::add);

        OpenTextQuestionEditor otEditor = new OpenTextQuestionEditor();
        invokeWithResult(view, "onAccordionPanelOpened", new Class[]{Component.class}, otEditor);

        assertTrue(events.isEmpty());
    }

    @Test
    void resolveModelForTextureQuestion_withNullModelId_shouldReturnFailedFuture() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, (Object) null);

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void resolveModelForTextureQuestion_withBlankModelId_shouldReturnFailedFuture() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "   ");

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void resolveModelForTextureQuestion_withCachedModel_shouldReturnCachedResult() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuickModelEntity cachedModel = quickModel();
        setField(view, "resolvedModelsByModelId", new HashMap<>(Map.of("file-1", cachedModel)));

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");

        assertEquals(cachedModel, future.join());
        verify(modelService, never()).read(any());
    }

    @Test
    void findModelMetadataIdByModelId_shouldPaginateThroughAllPagesUntilFound() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuickModelEntity firstPageModel = QuickModelEntity.builder()
                .metadataId("wrong-meta")
                .model(ModelFileEntity.builder().id("other-file").name("Other").related(List.of()).build())
                .build();
        QuickModelEntity secondPageModel = quickModel();

        List<QuickModelEntity> fullPage = List.of(
                firstPageModel, firstPageModel, firstPageModel, firstPageModel, firstPageModel,
                firstPageModel, firstPageModel, firstPageModel, firstPageModel, firstPageModel
        );

        when(modelService.readEntities(any()))
                .thenReturn(new PageResult<>(fullPage, 10L, 0))
                .thenReturn(new PageResult<>(List.of(secondPageModel), 1L, 1));
        when(modelService.read("model-meta-1")).thenReturn(modelEntity());

        setField(view, "chapterModelsByModelId", new HashMap<String, QuickModelEntity>());
        setField(view, "modelMetadataByModelId", new HashMap<String, String>());

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        Object result = future.join();

        assertNotNull(result);
        verify(modelService, times(2)).readEntities(any());
    }

    @Test
    void validateButton_onValidEditor_shouldNotThrow() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuestionEditorBase<?> editor = (QuestionEditorBase<?>) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);

        assertDoesNotThrow(() -> editor.getValidateButton().click());
    }

    @Test
    void quizForm_shouldHaveQuestionTypeSelectAndNameField() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        assertNotNull(form);
        assertNotNull(form.getNameField());
        assertNotNull(form.getQuestionTypeSelect());
    }

    @Test
    void saveQuiz_withEmptyName_shouldShowErrorAndNotCallService() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveQuiz_withEmptyEditorList_shouldShowErrorAndNotCallService() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Test Quiz");

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveQuiz_withInvalidEditor_shouldShowErrorAndNotCallService() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Test Quiz");

        invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.SINGLE_CHOICE);

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saveQuiz_inCreateMode_withValidEditor_shouldCallSaveService() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Test Quiz");

        OpenTextQuestionEditor editor = (OpenTextQuestionEditor) invokeWithResult(
                view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);
        editor.getQuestionTextField().setValue("What is this?");
        editor.addOption("Answer A");

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addQuestion_withSingleChoiceDataAndAnswer_shouldInitializeEditorFromData() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        SingleChoiceQuestionData qData = SingleChoiceQuestionData.builder()
                .questionId("q-1").type(QuestionTypeEnum.SINGLE_CHOICE)
                .questionText("Which one?").points(1).options(List.of("A", "B")).build();
        SingleChoiceAnswerData aData = SingleChoiceAnswerData.builder()
                .questionId("q-1").type(QuestionTypeEnum.SINGLE_CHOICE).correctIndex(0).build();

        invokeWithResult(view, "addQuestion",
                new Class[]{AbstractQuestionData.class, AbstractAnswerData.class}, qData, aData);

        List<?> editors = getField(view, "questionEditors", List.class);
        assertEquals(1, editors.size());
        assertInstanceOf(SingleChoiceQuestionEditor.class, editors.get(0));
    }

    @Test
    void addQuestion_withTextureClickDataAndAnswer_shouldAddTextureEditor() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        TextureClickQuestionData qData = TextureClickQuestionData.builder()
                .questionId("q-tc").type(QuestionTypeEnum.TEXTURE_CLICK)
                .questionText("Click on?").points(2).modelId(null).textureId(null).build();
        TextureClickAnswerData aData = TextureClickAnswerData.builder()
                .questionId("q-tc").type(QuestionTypeEnum.TEXTURE_CLICK)
                .modelId(null).textureId(null).hexColor("#FF0000").build();

        invokeWithResult(view, "addQuestion",
                new Class[]{AbstractQuestionData.class, AbstractAnswerData.class}, qData, aData);

        List<?> editors = getField(view, "questionEditors", List.class);
        assertEquals(1, editors.size());
        assertInstanceOf(TextureClickQuestionEditor.class, editors.get(0));
    }

    @Test
    void beforeEnter_withQuizIdParam_shouldSetEditMode() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        view.beforeEnter(beforeEnterEvent("quiz-123"));

        assertTrue(getField(view, "isEditMode", Boolean.class));
    }

    @Test
    void afterNavigation_inEditMode_shouldCallGetQuizWithAnswers() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuizEntity quiz = QuizEntity.builder()
                .id("quiz-1").name("Quiz 1").chapterId(null)
                .questions(List.of()).answers(List.of()).build();
        when(quizService.getQuizWithAnswers("quiz-1")).thenReturn(quiz);

        setField(view, "quizId", "quiz-1");
        setField(view, "isEditMode", true);

        view.afterNavigation(null);
        flushUi();

        verify(quizService).getQuizWithAnswers("quiz-1");
    }

    @Test
    void afterNavigation_inEditMode_whenServiceThrows_shouldNotThrow() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        when(quizService.getQuizWithAnswers(any())).thenThrow(new RuntimeException("load error"));

        setField(view, "quizId", "quiz-err");
        setField(view, "isEditMode", true);

        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();

        verify(quizService).getQuizWithAnswers("quiz-err");
    }

    @Test
    void afterNavigation_inEditMode_withEmptyQuiz_shouldSetEditLoaded() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuizEntity quiz = QuizEntity.builder()
                .id("quiz-2").name("Empty Quiz").chapterId(null)
                .questions(List.of()).answers(List.of()).build();
        when(quizService.getQuizWithAnswers("quiz-2")).thenReturn(quiz);

        setField(view, "quizId", "quiz-2");
        setField(view, "isEditMode", true);

        view.afterNavigation(null);
        MockVaadin.clientRoundtrip(false);

        assertTrue(getField(view, "quizEditLoaded", Boolean.class));
    }

    @Test
    void onAccordionPanelOpened_withTextureEditorAndNoSelection_shouldNotFireThreeJsEvent() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        List<Object> events = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(),
                ThreeJsActionEvent.class, events::add);

        TextureClickQuestionEditor textureEditor = (TextureClickQuestionEditor) invokeWithResult(
                view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.TEXTURE_CLICK);
        invokeWithResult(view, "onAccordionPanelOpened", new Class[]{Component.class}, textureEditor);

        assertTrue(events.isEmpty());
    }

    @Test
    void resolveModelForTextureQuestion_withChapterModel_shouldCallModelServiceRead() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        QuickModelEntity chapterModel = QuickModelEntity.builder()
                .metadataId("meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Skull").related(List.of()).build())
                .build();
        setField(view, "chapterModelsByModelId", new HashMap<>(Map.of("file-1", chapterModel)));

        when(modelService.read("meta-1")).thenReturn(modelEntity());

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(
                view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        Object result = future.join();

        assertNotNull(result);
        verify(modelService).read("meta-1");
    }

    private BeforeEnterEvent beforeEnterEvent(String quizId) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("quizId", quizId));
        return event;
    }

    private ChapterEntity chapter() {
        return ChapterEntity.builder()
                .id("chapter-1")
                .name("Kapitola 1")
                .build();
    }

    private QuickModelEntity quickModel() {
        return QuickModelEntity.builder()
                .metadataId("model-meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").related(List.of()).build())
                .build();
    }

    private ModelEntity modelEntity() {
        return ModelEntity.builder()
                .metadataId("model-meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").related(List.of()).build())
                .otherTextures(List.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeWithResult(Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method method = findMethod(target.getClass(), name, types);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found in " + clazz.getName());
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>[] types) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, types);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name + " not found in " + clazz.getName());
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }
}
