package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MatchingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MultipleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OrderingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MatchingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Checks a quiz before it is stored: that every question is answerable, that its answer refers to
 * options that exist, and that questions and answers stay linked by a shared question id.
 * Questions and answers arrive paired by position from the editor; this is where that pairing is
 * turned into an explicit id so grading no longer depends on ordering.
 */
@Component
@RequiredArgsConstructor
public class QuizAuthoringValidator {

    private final FileStorageService fileStorageService;

    /**
     * Validates and links a quiz's questions and answers in place.
     *
     * @param questions questions in editor order.
     * @param answers   answers in the same order.
     */
    public void validate(List<AbstractQuestionData> questions, List<AbstractAnswerData> answers) {
        if (questions == null || questions.isEmpty()) {
            throw new BackendException.Validation("Kvíz musí obsahovat alespoň jednu otázku.");
        }
        if (answers == null || answers.size() != questions.size()) {
            throw new BackendException.Validation("Kvíz musí obsahovat odpověď ke každé otázce.");
        }

        Set<String> assignedIds = new HashSet<>();
        for (int index = 0; index < questions.size(); index++) {
            AbstractQuestionData question = questions.get(index);
            AbstractAnswerData answer = answers.get(index);
            if (question == null || answer == null) {
                throw new BackendException.Validation("Otázka i odpověď musí být vyplněny.");
            }

            String questionId = keepOrMint(question.getQuestionId(), assignedIds);
            question.setQuestionId(questionId);
            answer.setQuestionId(questionId);

            validateCommon(question, answer, index);
            validateByType(question, answer, index);
        }
    }

    /**
     * Keeps a question's existing id across edits, because an attempt that is already open was
     * handed those ids and is graded by them: re-minting on every save would silently score a
     * quiz edited mid-attempt as zero. A fresh id is minted only when there is none yet, or when
     * the quiz carries the same id twice, which would otherwise collapse two questions into one.
     *
     * @param existing    id the question arrived with, may be {@code null}.
     * @param assignedIds ids already used by earlier questions of this quiz.
     * @return the id to use, recorded as taken.
     */
    private String keepOrMint(String existing, Set<String> assignedIds) {
        String questionId = existing != null && !existing.isBlank() && !assignedIds.contains(existing)
                ? existing
                : UUID.randomUUID().toString();
        assignedIds.add(questionId);
        return questionId;
    }

    private void validateCommon(AbstractQuestionData question, AbstractAnswerData answer, int index) {
        if (question.getPoints() == null || question.getPoints() <= 0) {
            throw new BackendException.Validation(at(index) + " musí mít kladný počet bodů.");
        }
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            throw new BackendException.Validation(at(index) + " musí mít zadané znění.");
        }
        if (question.getType() == null || question.getType() != answer.getType()) {
            throw new BackendException.Validation(at(index) + " má odpověď jiného typu, než je typ otázky.");
        }
        if (!matchesDeclaredType(question, answer)) {
            throw new BackendException.Validation(at(index) + " neodpovídá deklarovanému typu " + question.getType() + ".");
        }
    }

    private void validateByType(AbstractQuestionData question, AbstractAnswerData answer, int index) {
        switch (question) {
            case MultipleChoiceQuestionData typed -> {
                int options = requireOptions(typed.getOptions(), index);
                List<Integer> correct = ((MultipleChoiceAnswerData) answer).getCorrectItems();
                requireNotEmpty(correct, at(index) + " musí mít označenou alespoň jednu správnou možnost.");
                requireDistinct(correct, at(index) + " má správné možnosti uvedené vícekrát.");
                requireInRange(correct, options, index);
            }
            case SingleChoiceQuestionData typed -> {
                int options = requireOptions(typed.getOptions(), index);
                Integer correct = ((SingleChoiceAnswerData) answer).getCorrectIndex();
                if (correct == null) {
                    throw new BackendException.Validation(at(index) + " musí mít označenou správnou možnost.");
                }
                requireInRange(List.of(correct), options, index);
            }
            case MatchingQuestionData typed -> {
                int left = requireOptions(typed.getLeftItems(), index);
                int right = requireOptions(typed.getRightItems(), index);
                Map<Integer, Integer> matches = ((MatchingAnswerData) answer).getCorrectMatches();
                if (matches == null || matches.isEmpty()) {
                    throw new BackendException.Validation(at(index) + " musí mít vyplněné správné dvojice.");
                }
                requireInRange(matches.keySet(), left, index);
                requireInRange(matches.values(), right, index);
                // The student answers by choosing a partner for every left item, and grading
                // compares the two maps whole. So the answer key has to name every left item
                // exactly once: a key short of that is a question nobody can get right, however
                // the gap arose -- a left item picked twice in the editor, or columns of
                // different lengths.
                if (matches.size() != left) {
                    throw new BackendException.Validation(
                            at(index) + " musí spojit každou položku levého sloupce právě jednou.");
                }
                if (matches.values().stream().distinct().count() != matches.size()) {
                    throw new BackendException.Validation(
                            at(index) + " má jednu položku pravého sloupce přiřazenou k více položkám levého.");
                }
            }
            case OrderingQuestionData typed -> {
                int items = requireOptions(typed.getItems(), index);
                List<Integer> order = ((OrderingAnswerData) answer).getCorrectOrder();
                requireNotEmpty(order, at(index) + " musí mít určené správné pořadí.");
                if (order.size() != items) {
                    throw new BackendException.Validation(at(index) + " musí mít v pořadí všechny položky právě jednou.");
                }
                requireDistinct(order, at(index) + " má v pořadí některou položku vícekrát.");
                requireInRange(order, items, index);
            }
            case OpenTextQuestionData ignored -> {
                OpenTextAnswerData typed = (OpenTextAnswerData) answer;
                requireNotEmpty(typed.getAcceptableAnswers(), at(index) + " musí mít alespoň jednu uznávanou odpověď.");
                typed.setAcceptableAnswers(trimmedDistinctAnswers(typed.getAcceptableAnswers()));
                requireNotEmpty(typed.getAcceptableAnswers(), at(index) + " musí mít alespoň jednu uznávanou odpověď.");
            }
            case TextureClickQuestionData typed -> {
                if (!fileStorageService.exists(typed.getModelId())) {
                    throw new BackendException.NotFound(at(index) + " odkazuje na model, který neexistuje.");
                }
                if (!fileStorageService.exists(typed.getTextureId())) {
                    throw new BackendException.NotFound(at(index) + " odkazuje na texturu, která neexistuje.");
                }
                String hexColor = ((TextureClickAnswerData) answer).getHexColor();
                if (hexColor == null || hexColor.isBlank()) {
                    throw new BackendException.Validation(at(index) + " musí mít vybranou oblast na textuře.");
                }
            }
            default -> throw new BackendException.Validation(at(index) + " má neznámý typ otázky.");
        }
    }

    /**
     * Guards the cast performed by {@link #validateByType}: the declared enum, the question class and
     * the answer class must all describe the same question type.
     */
    private boolean matchesDeclaredType(AbstractQuestionData question, AbstractAnswerData answer) {
        return switch (question.getType()) {
            case MULTIPLE_CHOICE -> question instanceof MultipleChoiceQuestionData && answer instanceof MultipleChoiceAnswerData;
            case SINGLE_CHOICE -> question instanceof SingleChoiceQuestionData && answer instanceof SingleChoiceAnswerData;
            case OPEN_TEXT -> question instanceof OpenTextQuestionData && answer instanceof OpenTextAnswerData;
            case MATCHING -> question instanceof MatchingQuestionData && answer instanceof MatchingAnswerData;
            case ORDERING -> question instanceof OrderingQuestionData && answer instanceof OrderingAnswerData;
            case TEXTURE_CLICK -> question instanceof TextureClickQuestionData && answer instanceof TextureClickAnswerData;
        };
    }

    /**
     * Tidies the accepted answers of an open question without destroying them: surrounding space
     * goes, blanks go, and spellings that differ only in case collapse onto the first one the
     * author wrote. The case itself is kept, because an answer marked as an exact match is graded
     * against it literally.
     *
     * @param answers accepted answers as the author typed them.
     * @return the tidied answers, in the author's order.
     */
    private List<String> trimmedDistinctAnswers(List<String> answers) {
        Map<String, String> byComparisonKey = new LinkedHashMap<>();
        for (String text : answers) {
            if (text != null && !text.isBlank()) {
                byComparisonKey.putIfAbsent(text.trim().toLowerCase(Locale.ROOT), text.trim());
            }
        }
        return List.copyOf(byComparisonKey.values());
    }

    private int requireOptions(List<String> options, int index) {
        if (options == null || options.size() < 2) {
            throw new BackendException.Validation(at(index) + " musí nabízet alespoň dvě možnosti.");
        }
        return options.size();
    }

    private void requireNotEmpty(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new BackendException.Validation(message);
        }
    }

    private void requireDistinct(List<Integer> values, String message) {
        if (values.size() != values.stream().distinct().count()) {
            throw new BackendException.Validation(message);
        }
    }

    private void requireInRange(Collection<Integer> values, int size, int index) {
        for (Integer value : values) {
            if (value == null || value < 0 || value >= size) {
                throw new BackendException.Validation(at(index) + " odkazuje na možnost, která neexistuje.");
            }
        }
    }

    private String at(int index) {
        return "Otázka č. " + (index + 1);
    }
}
