package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Location;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ListingViewsKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        when(quizService.readEntities(any())).thenReturn(new PageResult<>(List.of(quiz()), 1L, 0));
        when(modelService.readEntities(any())).thenReturn(new PageResult<>(List.of(model()), 1L, 0));
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void quizListingShouldLoadItems() {
        QuizListingView view = new QuizListingView(quizService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("quizes"));
        flushUi();

        verify(quizService, atLeastOnce()).readEntities(any());
    }

    @Test
    void modelListingShouldLoadItems() {
        ModelListingView view = new ModelListingView(modelService);
        UI.getCurrent().add(view);
        view.afterNavigation(mockNavigationEvent("models"));
        flushUi();

        verify(modelService, atLeastOnce()).readEntities(any());
    }

    private QuickQuizEntity quiz() {
        return QuickQuizEntity.builder()
                .id("quiz-1")
                .name("Test quiz")
                .chapterId("chapter-1")
                .build();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder()
                .metadataId("model-1")
                .model(ModelFileEntity.builder().id("file-1").name("Model").build())
                .description("data:image/png;base64,AAA")
                .build();
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }

    private AfterNavigationEvent mockNavigationEvent(String path) {
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        Location location = mock(Location.class);
        when(location.getPath()).thenReturn(path);
        when(event.getLocation()).thenReturn(location);
        return event;
    }
}
