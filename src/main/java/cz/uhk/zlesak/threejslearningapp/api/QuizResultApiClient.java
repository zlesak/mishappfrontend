package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IQuizResultApiClient;
import cz.uhk.zlesak.threejslearningapp.backend.service.QuizResultBackendService;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizSubmissionRequest;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Quiz result access for the UI.
 * Results are only ever produced by grading a submission, so the generic create and update
 * operations are not available.
 */
@Component
@RequiredArgsConstructor
public class QuizResultApiClient implements IQuizResultApiClient {

    private final QuizResultBackendService quizResultBackendService;

    @Override
    public QuizValidationResult validateAnswers(QuizSubmissionRequest submissionRequest) {
        return quizResultBackendService.submit(submissionRequest);
    }

    @Override
    public QuickQuizResult create(QuizValidationResult entity) {
        throw new UnsupportedOperationException("Výsledek kvízu vzniká pouze odevzdáním kvízu.");
    }

    @Override
    public QuizValidationResult read(String id) {
        return quizResultBackendService.require(id);
    }

    @Override
    public QuickQuizResult readQuick(String id) {
        return quizResultBackendService.require(id);
    }

    @Override
    public PageResult<QuickQuizResult> readEntities(FilterParameters<QuizResultFilter> filterParameters) {
        return quizResultBackendService.list(filterParameters);
    }

    @Override
    public QuizValidationResult update(String id, QuizValidationResult entity) {
        throw new UnsupportedOperationException("Výsledek kvízu nelze upravovat.");
    }

    @Override
    public boolean delete(String id) {
        throw new UnsupportedOperationException("Výsledek kvízu nelze mazat.");
    }
}
