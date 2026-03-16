package cz.uhk.zlesak.threejslearningapp.views.quizes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.OpenTextQuestionEditor;
import cz.uhk.zlesak.threejslearningapp.components.editors.question.TextureClickQuestionEditor;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateQuizForm;
import cz.uhk.zlesak.threejslearningapp.components.quizComponents.QuizPlayerComponent;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.AbstractSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OpenTextSubmissionData;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ChapterSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class QuizViewsKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private QuizResultService quizResultService;

    @MockitoBean
    private ModelService modelService;

    @MockitoBean
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void quizDetailViewShouldForwardWithoutQuizId() {
        QuizDetailView view = new QuizDetailView(quizResultService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters());

        view.beforeEnter(event);

        verify(event).forwardTo(QuizListingView.class);
    }

    @Test
    void quizDetailViewShouldRenderQuizDetailAndHistoryListing() {
        when(quizService.readQuick("quiz-1")).thenReturn(quickQuiz());
        when(quizResultService.readEntities(org.mockito.ArgumentMatchers.any())).thenReturn(
                new cz.uhk.zlesak.threejslearningapp.domain.common.PageResult<>(List.of(result()), 1L, 0)
        );

        QuizDetailView view = new QuizDetailView(quizResultService);
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("quiz-1"));
        view.afterNavigation(null);
        flushUi();
        verify(quizService, org.mockito.Mockito.timeout(1000)).readQuick("quiz-1");
    }

    @Test
    void quizResultViewShouldForwardWithoutRequiredParams() {
        QuizResultView view = new QuizResultView(quizResultService);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters("quizId", "quiz-1"));

        view.beforeEnter(event);

        verify(event).forwardTo(QuizListingView.class);
    }

    @Test
    void quizResultViewShouldRenderResultSummaryAndDetails() {
        when(quizResultService.read("result-1")).thenReturn(validationResult());
        when(quizService.getQuizForStudent("quiz-1")).thenReturn(quizEntity());
        when(quizService.calculatePossibleScore("quiz-1")).thenReturn(10);

        QuizResultView view = new QuizResultView(quizResultService);
        UI.getCurrent().add(view);
        view.beforeEnter(resultBeforeEnterEvent("result-1", "quiz-1"));
        view.afterNavigation(null);
        flushUi();
        verify(quizResultService, org.mockito.Mockito.timeout(1000)).read("result-1");
        verify(quizService, org.mockito.Mockito.timeout(1000)).getQuizForStudent("quiz-1");
    }

    @Test
    void quizResultsListingShouldPropagateQuizIdIntoFilterAndRenderItems() {
        when(quizResultService.readEntities(any())).thenReturn(new PageResult<>(List.of(result()), 1L, 0));

        QuizResultsListingView view = new QuizResultsListingView(quizResultService);
        UI.getCurrent().add(view);
        view.afterNavigation(afterNavigationEvent("quiz-1"));
        flushUi();
        verify(quizResultService, org.mockito.Mockito.timeout(1000).atLeastOnce()).readEntities(any());

        var captor = org.mockito.ArgumentCaptor.forClass(cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters.class);
        verify(quizResultService, org.mockito.Mockito.atLeastOnce()).readEntities(captor.capture());
        cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter filter =
                (cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter) captor.getAllValues().getLast().getFilter();
        assertEquals("quiz-1", filter.getQuizId());
    }

    @Test
    void quizPlayerViewShouldForwardWithoutQuizId() {
        QuizPlayerView view = new QuizPlayerView();
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters());

        view.beforeEnter(event);

        verify(event).forwardTo(QuizListingView.class);
    }

    @Test
    void quizPlayerViewShouldRenderPlayerAndStickyTimer() {
        when(quizService.getQuizForStudent("quiz-1")).thenReturn(quizEntity().toBuilder().timeLimit(5).build());

        QuizPlayerView view = new QuizPlayerView();
        UI.getCurrent().add(view);
        view.beforeEnter(beforeEnterEvent("quiz-1"));
        view.afterNavigation(null);
        flushUi();
        verify(quizService, org.mockito.Mockito.timeout(1000)).getQuizForStudent("quiz-1");
    }

    @Test
    void quizPlayerSubmitShouldShowResultWhenValidationSucceeds() throws Exception {
        QuizPlayerView view = new QuizPlayerView();
        UI.getCurrent().add(view);
        setField(view, "quizId", "quiz-1");
        setField(view, "loadedQuiz", quizEntity());
        setField(view, "loadedQuizPossibleScore", 10);

        QuizPlayerComponent playerComponent = mock(QuizPlayerComponent.class);
        HashMap<String, AbstractSubmissionData> answers = new HashMap<>();
        answers.put("q1", OpenTextSubmissionData.builder().questionId("q1").type(QuestionTypeEnum.OPEN_TEXT).text("Femur").build());
        when(playerComponent.getAnswers()).thenReturn(answers);
        setField(view, "playerComponent", playerComponent);

        when(quizService.validateAnswers(eq("quiz-1"), any())).thenReturn(validationResult());
        when(quizService.getQuizForStudent("quiz-1")).thenReturn(quizEntity());
        when(quizService.calculatePossibleScore("quiz-1")).thenReturn(10);

        invoke(view, "submitQuiz");
        flushUi();

        verify(playerComponent).disable();
        verify(playerComponent, never()).enable();
        verify(quizService, org.mockito.Mockito.timeout(1000)).validateAnswers(eq("quiz-1"), any());
    }

    @Test
    void quizPlayerSubmitShouldReEnablePlayerWhenValidationFails() throws Exception {
        QuizPlayerView view = new QuizPlayerView();
        UI.getCurrent().add(view);
        setField(view, "quizId", "quiz-1");
        setField(view, "loadedQuiz", quizEntity());
        setField(view, "loadedQuizPossibleScore", 10);

        QuizPlayerComponent playerComponent = mock(QuizPlayerComponent.class);
        when(playerComponent.getAnswers()).thenReturn(new HashMap<>());
        setField(view, "playerComponent", playerComponent);
        when(quizService.validateAnswers(eq("quiz-1"), any())).thenThrow(new RuntimeException("boom"));

        invoke(view, "submitQuiz");
        flushUi();

        verify(playerComponent).disable();
        verify(quizService, org.mockito.Mockito.timeout(1000)).validateAnswers(eq("quiz-1"), any());
    }

    @Test
    void quizCreateViewShouldRenderLoadedQuizInEditMode() throws Exception {
        QuizEntity loadedQuiz = quizEntity().toBuilder()
                .answers(List.of(OpenTextAnswerData.builder()
                        .questionId("q1")
                        .type(QuestionTypeEnum.OPEN_TEXT)
                        .acceptableAnswers(List.of("Femur"))
                        .exactMatch(true)
                        .build()))
                .timeLimit(12)
                .chapterId("chapter-1")
                .build();
        when(quizService.getQuizWithAnswers("quiz-1")).thenReturn(loadedQuiz);
        when(chapterService.read("chapter-1")).thenReturn(chapter());
        when(chapterService.getChaptersModels("chapter-1")).thenReturn(Map.of("model-1", quickModel()));

        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        view.beforeEnter(beforeEnterEvent("quiz-1"));
        view.afterNavigation(null);
        flushUi();

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        List<?> editors = getField(view, "questionEditors", List.class);

        assertEquals("Upravit kvíz", form.getSaveQuizButton().getText());
        verify(quizService).getQuizWithAnswers("quiz-1");
        assertNotNull(editors);
    }

    @Test
    void quizCreateViewShouldThrowWhenEditTargetDoesNotExist() {
        when(quizService.getQuizWithAnswers("missing")).thenReturn(null);

        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.beforeEnter(beforeEnterEvent("missing")));
        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();
    }

    @Test
    void quizCreateViewShouldApplySelectedChapterEventToForm() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        ComponentUtil.fireEvent(UI.getCurrent(), new ChapterSelectedFromDialogEvent(UI.getCurrent(), false, chapter(), "quiz-chapter-select"));

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        assertEquals("chapter-1", form.getSelectedChapter());
    }

    @Test
    void quizCreateViewShouldSaveValidQuizOnCreateEvent() {
        when(quizService.saveQuiz(eq(null), eq(false), eq(null), eq("Novy kviz"), eq("Popis"), eq(7), eq("chapter-1"), any(), any()))
                .thenReturn("quiz-2");

        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Novy kviz");
        form.getDescriptionField().setValue("Popis");
        form.getTimeLimitField().setValue(7);
        form.getChapterSelect().setItems(chapter());
        form.getChapterSelect().setValue(chapter());

        OpenTextQuestionEditor editor = addOpenTextQuestion(view, "Jaká kost je největší?", "Femur");

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));
        flushUi();

        verify(quizService, org.mockito.Mockito.timeout(1000)).saveQuiz(eq(null), eq(false), eq(null), eq("Novy kviz"), eq("Popis"), eq(7), eq("chapter-1"), any(), any());
        assertTrue(editor.validate());
    }

    @Test
    void quizCreateViewShouldRejectSaveWhenQuestionIsInvalid() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Novy kviz");
        form.getDescriptionField().setValue("Popis");

        addOpenTextQuestion(view, "", "");

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(quizService).clearQuestionsAndAnswers();
    }

    @Test
    void quizCreateViewShouldRejectSaveWhenNameIsMissing() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getDescriptionField().setValue("Popis");
        form.getChapterSelect().setItems(chapter());
        form.getChapterSelect().setValue(chapter());
        addOpenTextQuestion(view, "Otazka", "Femur");

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(quizService).clearQuestionsAndAnswers();
    }

    @Test
    void quizCreateViewShouldRejectSaveWhenThereAreNoQuestions() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        CreateQuizForm form = getField(view, "quizForm", CreateQuizForm.class);
        form.getNameField().setValue("Novy kviz");
        form.getDescriptionField().setValue("Popis");
        form.getChapterSelect().setItems(chapter());
        form.getChapterSelect().setValue(chapter());

        ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent()));

        verify(quizService, never()).saveQuiz(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verify(quizService).clearQuestionsAndAnswers();
    }

    @Test
    void quizCreateViewShouldResolveModelFromChapterCacheFirst() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        setField(view, "chapterModelsByModelId", new HashMap<>(Map.of("file-1", quickModel())));
        setField(view, "modelMetadataByModelId", new HashMap<String, String>());
        when(modelService.read("model-meta-1")).thenReturn(modelEntity());

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        Object value = future.join();

        assertNotNull(value);
        verify(modelService).read("model-meta-1");
        verify(modelService, never()).readEntities(any());
    }

    @Test
    void quizCreateViewShouldResolveModelViaPaginatedLookup() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        setField(view, "chapterModelsByModelId", new HashMap<String, QuickModelEntity>());
        setField(view, "modelMetadataByModelId", new HashMap<String, String>());

        when(modelService.readEntities(any())).thenReturn(new PageResult<>(List.of(quickModel()), 1L, 0));
        when(modelService.read("model-meta-1")).thenReturn(modelEntity());

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        Object value = future.join();

        assertNotNull(value);
        verify(modelService).readEntities(any());
        verify(modelService).read("model-meta-1");
    }

    @Test
    void quizCreateViewShouldFailResolvingUnknownModel() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        setField(view, "chapterModelsByModelId", new HashMap<String, QuickModelEntity>());
        setField(view, "modelMetadataByModelId", new HashMap<String, String>());
        when(modelService.readEntities(any())).thenReturn(new PageResult<>(List.of(), 0L, 0));

        CompletableFuture<?> future = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "missing-model");
        CompletionException exception = assertThrows(CompletionException.class, future::join);

        assertInstanceOf(ApplicationContextException.class, rootCause(exception));
    }

    @Test
    void quizCreateViewShouldWrapUnexpectedErrorsDuringEditInitialization() {
        when(quizService.getQuizWithAnswers("quiz-1")).thenThrow(new RuntimeException("broken"));

        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.beforeEnter(beforeEnterEvent("quiz-1")));
        assertDoesNotThrow(() -> view.afterNavigation(null));
        flushUi();
    }

    @Test
    void quizCreateViewShouldApplySelectedModelEventByLoadingFullModel() {
        when(modelService.read("model-meta-1")).thenReturn(modelEntity());

        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        ComponentUtil.fireEvent(UI.getCurrent(), new ModelSelectedFromDialogEvent(UI.getCurrent(), false, quickModel(), "quiz-model"));
        flushUi();

        verify(modelService, times(1)).read("model-meta-1");
    }

    @Test
    void quizCreateViewShouldEmitMaskEventWhenTextureQuestionAccordionOpens() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        UI.getCurrent().add(view);

        TextureClickQuestionEditor editor = new TextureClickQuestionEditor(modelId -> CompletableFuture.completedFuture(quickModel()));
        editor.initialize(cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData.builder()
                .questionId("q-texture")
                .questionText("Klikni")
                .type(QuestionTypeEnum.TEXTURE_CLICK)
                .points(1)
                .modelId("file-1")
                .textureId("texture-1")
                .build());
        editor.setAnswerData(cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData.builder()
                .questionId("q-texture")
                .type(QuestionTypeEnum.TEXTURE_CLICK)
                .modelId("file-1")
                .textureId("texture-1")
                .hexColor("#112233")
                .build());

        ArrayList<ThreeJsActionEvent> actions = new ArrayList<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, actions::add);

        invokeWithResult(view, "onAccordionPanelOpened", new Class[]{Component.class}, editor);

        assertEquals(1, actions.size());
        assertEquals(ThreeJsActions.APPLY_MASK_TO_TEXTURE, actions.getFirst().getAction());
        assertEquals("#112233", actions.getFirst().getMaskColor());
    }

    @Test
    void quizCreateViewShouldReuseCachedMetadataLookup() {
        QuizCreateView view = new QuizCreateView(quizService, chapterService, modelService);
        setField(view, "chapterModelsByModelId", new HashMap<String, QuickModelEntity>());
        setField(view, "modelMetadataByModelId", new HashMap<String, String>());
        when(modelService.readEntities(any())).thenReturn(new PageResult<>(List.of(quickModel()), 1L, 0));
        when(modelService.read("model-meta-1")).thenReturn(modelEntity());

        CompletableFuture<?> first = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        first.join();
        CompletableFuture<?> second = (CompletableFuture<?>) invokeWithResult(view, "resolveModelForTextureQuestionAsync", new Class[]{String.class}, "file-1");
        second.join();

        verify(modelService, times(1)).readEntities(any());
        verify(modelService, times(1)).read("model-meta-1");
    }

    private BeforeEnterEvent beforeEnterEvent(String quizId) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("quizId", quizId));
        return event;
    }

    private BeforeEnterEvent resultBeforeEnterEvent(String resultId, String back) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters(
                new RouteParam("quizId", resultId),
                new RouteParam("back", back)
        ));
        return event;
    }

    private AfterNavigationEvent afterNavigationEvent(String quizId) {
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("quizId", quizId));
        return event;
    }

    private QuickQuizEntity quickQuiz() {
        return QuickQuizEntity.builder()
                .id("quiz-1")
                .name("Kosti")
                .description("Popis kvízu")
                .chapterId("chapter-1")
                .timeLimit(0)
                .created(Instant.parse("2025-01-01T10:00:00Z"))
                .updated(Instant.parse("2025-01-02T11:30:00Z"))
                .build();
    }

    private QuickQuizResult result() {
        return QuickQuizResult.builder()
                .id("result-1")
                .name("Výsledek")
                .maxScore(10)
                .totalScore(7)
                .percentage(70.0)
                .build();
    }

    private QuizValidationResult validationResult() {
        return QuizValidationResult.builder()
                .id("result-1")
                .totalScore(7)
                .maxScore(10)
                .percentage(70.0)
                .questionResults(List.of(new QuizValidationQuestion(
                        "Jaká kost je největší?",
                        true,
                        7,
                        OpenTextSubmissionData.builder()
                                .questionId("q1")
                                .type(QuestionTypeEnum.OPEN_TEXT)
                                .text("Femur")
                                .build()
                )))
                .build();
    }

    private QuizEntity quizEntity() {
        return QuizEntity.builder()
                .id("quiz-1")
                .name("Kosti")
                .description("Popis kvízu")
                .timeLimit(0)
                .chapterId("chapter-1")
                .questions(List.of(OpenTextQuestionData.builder()
                        .questionId("q1")
                        .questionText("Jaká kost je největší?")
                        .type(QuestionTypeEnum.OPEN_TEXT)
                        .points(10)
                        .placeholder("Napiš odpověď")
                        .build()))
                .answers(List.of())
                .build();
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

    private cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity modelEntity() {
        return cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity.builder()
                .metadataId("model-meta-1")
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").related(List.of()).build())
                .otherTextures(List.of())
                .build();
    }

    private OpenTextQuestionEditor addOpenTextQuestion(QuizCreateView view, String question, String answer) {
        OpenTextQuestionEditor editor = (OpenTextQuestionEditor) invokeWithResult(view, "addQuestion", new Class[]{QuestionTypeEnum.class}, QuestionTypeEnum.OPEN_TEXT);
        editor.getQuestionTextField().setValue(question);
        editor.addOption();
        editor.getOptions().getFirst().getOptionField().setValue(answer);
        return editor;
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

    private void invoke(Object target, String name) {
        invokeWithResult(target, name, new Class[]{});
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }
}
