package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.NoItemInfoComponent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PaginationComponent;
import cz.uhk.zlesak.threejslearningapp.components.listItems.QuizResultListItem;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizResultsHistoryPanelTest {
    private QuizResultService quizResultService;

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
        quizResultService = mock(QuizResultService.class);
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void loadPage_withSynchronousSessionAccess_shouldRenderErrorPathInUiAccess() throws Exception {
        makeSynchronousSessionAccess();

        when(quizResultService.readEntities(any())).thenThrow(new RuntimeException("api error"));
        QuizResultsHistoryPanel errorPanel = new QuizResultsHistoryPanel(quizResultService, "quiz-1");
        UI.getCurrent().add(errorPanel);
        invoke(errorPanel, "loadPage", new Class[]{int.class}, 0);

        assertFalse(findAll(errorPanel, NoItemInfoComponent.class).isEmpty());
    }

    @Test
    void loadPage_withSynchronousSessionAccess_shouldRenderResultsOnSuccess() throws Exception {
        if (UI.getCurrent() == null) {
            VaadinTestSupport.clearCurrentUi();
            VaadinTestSupport.setCurrentUi();
        }
        makeSynchronousSessionAccess();

        when(quizResultService.readEntities(any())).thenReturn(
                new PageResult<>(List.of(result("r-1"), result("r-2")), 2L, 0));
        QuizResultsHistoryPanel panel = new QuizResultsHistoryPanel(quizResultService, "quiz-1");
        UI.getCurrent().add(panel);
        invoke(panel, "loadPage", new Class[]{int.class}, 0);

        assertEquals(2, findAll(panel, QuizResultListItem.class).size());
    }

    private void makeSynchronousSessionAccess() {
        VaadinSession session = VaadinSession.getCurrent();
        doAnswer(invocation -> {
            ((Command) invocation.getArgument(0)).execute();
            return null;
        }).when(session).access(any());
    }

    private QuickQuizResult result(String id) {
        return QuickQuizResult.builder()
                .id(id)
                .quizId("quiz-1")
                .name("Result " + id)
                .maxScore(10)
                .totalScore(7)
                .percentage(70.0)
                .build();
    }

    private Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private void flushUi() {
        UI current = UI.getCurrent();
        if (current != null) {
            current.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        }
    }
}

