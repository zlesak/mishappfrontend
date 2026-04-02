package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.clients.QuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuizResultServiceTest {
    private QuizResultApiClient quizResultApiClient;
    private QuizResultService quizResultService;

    @BeforeEach
    void setUp() {
        quizResultApiClient = mock(QuizResultApiClient.class);
        quizResultService = new QuizResultService(quizResultApiClient);
    }

    @Test
    void create_shouldReturnCreatedIdentifier() throws Exception {
        QuizValidationResult createEntity = QuizValidationResult.builder().name("Attempt").build();
        when(quizResultApiClient.create(any(QuizValidationResult.class)))
                .thenReturn(QuickQuizResult.builder().id("result-1").build());

        String id = quizResultService.create(createEntity);

        assertEquals("result-1", id);
    }

    @Test
    void read_shouldCacheEntityById() throws Exception {
        QuizValidationResult expected = QuizValidationResult.builder().id("result-1").name("Attempt").build();
        when(quizResultApiClient.read("result-1")).thenReturn(expected);

        QuizValidationResult first = quizResultService.read("result-1");
        QuizValidationResult second = quizResultService.read("result-1");

        assertSame(expected, first);
        assertSame(first, second);
        verify(quizResultApiClient).read("result-1");
    }

    @Test
    void readQuick_shouldCacheQuickEntityById() throws Exception {
        QuickQuizResult expected = QuickQuizResult.builder().id("result-quick-1").name("Quick").build();
        when(quizResultApiClient.readQuick("result-quick-1")).thenReturn(expected);

        QuickQuizResult first = quizResultService.readQuick("result-quick-1");
        QuickQuizResult second = quizResultService.readQuick("result-quick-1");

        assertSame(expected, first);
        assertSame(first, second);
        verify(quizResultApiClient).readQuick("result-quick-1");
    }

    @Test
    void readEntities_shouldReturnPageResult() throws Exception {
        FilterParameters<QuizResultFilter> filterParameters = new FilterParameters<>();
        filterParameters.setPageRequest(PageRequest.of(1, 5));
        PageResult<QuickQuizResult> expected = new PageResult<>(
                List.of(QuickQuizResult.builder().id("result-1").build()),
                1L,
                1
        );
        when(quizResultApiClient.readEntities(filterParameters)).thenReturn(expected);

        PageResult<QuickQuizResult> actual = quizResultService.readEntities(filterParameters);

        assertSame(expected, actual);
    }

    @Test
    void update_shouldReturnIdentifierFromUpdatedEntity() throws Exception {
        QuizValidationResult updateEntity = QuizValidationResult.builder().name("Attempt").build();
        when(quizResultApiClient.update(eq("result-1"), any(QuizValidationResult.class)))
                .thenReturn(QuizValidationResult.builder().id("result-1").build());

        String id = quizResultService.update("result-1", updateEntity);

        assertEquals("result-1", id);
    }

    @Test
    void delete_shouldDelegateToApiClient() throws Exception {
        when(quizResultApiClient.delete("result-1")).thenReturn(true);

        boolean deleted = quizResultService.delete("result-1");

        assertTrue(deleted);
    }

    @Test
    void read_shouldRejectBlankIdentifier() {
        assertThrows(RuntimeException.class, () -> quizResultService.read(""));
    }
}
