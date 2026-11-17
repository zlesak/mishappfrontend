package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.MultipleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OpenTextAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.OrderingAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.SingleChoiceAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MultipleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OpenTextQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuizAuthoringValidatorTest {

    private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
    private final QuizAuthoringValidator validator = new QuizAuthoringValidator(fileStorageService);

    @Test
    void linksEachQuestionToItsAnswerWithASharedId() {
        AbstractQuestionData first = singleChoiceQuestion(2);
        AbstractQuestionData second = singleChoiceQuestion(3);
        AbstractAnswerData firstAnswer = singleChoiceAnswer(0);
        AbstractAnswerData secondAnswer = singleChoiceAnswer(1);

        validator.validate(new ArrayList<>(List.of(first, second)), new ArrayList<>(List.of(firstAnswer, secondAnswer)));

        assertThat(first.getQuestionId()).isNotBlank().isEqualTo(firstAnswer.getQuestionId());
        assertThat(second.getQuestionId()).isNotBlank().isEqualTo(secondAnswer.getQuestionId());
        assertThat(first.getQuestionId()).isNotEqualTo(second.getQuestionId());
    }

    @Test
    void keepsTheIdsOfQuestionsThatAlreadyHaveOne() {
        AbstractQuestionData question = singleChoiceQuestion(2);
        question.setQuestionId("otazka-1");
        AbstractAnswerData answer = singleChoiceAnswer(0);

        validator.validate(new ArrayList<>(List.of(question)), new ArrayList<>(List.of(answer)));

        assertThat(question.getQuestionId()).isEqualTo("otazka-1");
        assertThat(answer.getQuestionId()).isEqualTo("otazka-1");
    }

    @Test
    void mintsAFreshIdWhenTwoQuestionsArriveUnderTheSameOne() {
        AbstractQuestionData first = singleChoiceQuestion(2);
        AbstractQuestionData second = singleChoiceQuestion(3);
        first.setQuestionId("duplicitni");
        second.setQuestionId("duplicitni");

        validator.validate(new ArrayList<>(List.of(first, second)),
                new ArrayList<>(List.of(singleChoiceAnswer(0), singleChoiceAnswer(1))));

        assertThat(first.getQuestionId()).isEqualTo("duplicitni");
        assertThat(second.getQuestionId()).isNotBlank().isNotEqualTo("duplicitni");
    }

    @Test
    void rejectsAQuizWithoutQuestions() {
        assertThatThrownBy(() -> validator.validate(List.of(), List.of()))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("alespoň jednu otázku");
    }

    @Test
    void rejectsAQuestionWithoutAMatchingAnswer() {
        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(singleChoiceQuestion(1))),
                new ArrayList<>()))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("odpověď ke každé otázce");
    }

    @Test
    void rejectsAnAnswerPointingAtAnOptionThatDoesNotExist() {
        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(singleChoiceQuestion(1))),
                new ArrayList<>(List.of(singleChoiceAnswer(5)))))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("možnost, která neexistuje");
    }

    @Test
    void rejectsQuestionsWorthNoPoints() {
        AbstractQuestionData question = singleChoiceQuestion(0);

        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(question)),
                new ArrayList<>(List.of(singleChoiceAnswer(0)))))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("kladný počet bodů");
    }

    @Test
    void rejectsAMultipleChoiceAnswerListingTheSameOptionTwice() {
        MultipleChoiceQuestionData question = new MultipleChoiceQuestionData();
        question.setQuestionText("Otázka");
        question.setType(QuestionTypeEnum.MULTIPLE_CHOICE);
        question.setPoints(1);
        question.setOptions(List.of("a", "b", "c"));

        MultipleChoiceAnswerData answer = new MultipleChoiceAnswerData();
        answer.setType(QuestionTypeEnum.MULTIPLE_CHOICE);
        answer.setCorrectItems(List.of(1, 1));

        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(question)),
                new ArrayList<>(List.of(answer))))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("vícekrát");
    }

    @Test
    void requiresAnOrderingAnswerToBeAFullPermutation() {
        OrderingQuestionData question = new OrderingQuestionData();
        question.setQuestionText("Otázka");
        question.setType(QuestionTypeEnum.ORDERING);
        question.setPoints(1);
        question.setItems(List.of("a", "b", "c"));

        OrderingAnswerData answer = new OrderingAnswerData();
        answer.setType(QuestionTypeEnum.ORDERING);
        answer.setCorrectOrder(List.of(0, 1));

        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(question)),
                new ArrayList<>(List.of(answer))))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("všechny položky právě jednou");
    }

    @Test
    void tidiesAcceptedOpenTextAnswersWithoutLosingTheAuthorsCapitalisation() {
        OpenTextQuestionData question = new OpenTextQuestionData();
        question.setQuestionText("Otázka");
        question.setType(QuestionTypeEnum.OPEN_TEXT);
        question.setPoints(1);

        OpenTextAnswerData answer = new OpenTextAnswerData();
        answer.setType(QuestionTypeEnum.OPEN_TEXT);
        answer.setAcceptableAnswers(new ArrayList<>(List.of("  Mozek ", "MOZEK", "mozeček")));

        validator.validate(new ArrayList<>(List.of(question)), new ArrayList<>(List.of(answer)));

        assertThat(answer.getAcceptableAnswers()).containsExactly("Mozek", "mozeček");
    }

    @Test
    void rejectsATextureClickQuestionWhoseModelIsGone() {
        Mockito.when(fileStorageService.exists("model-1")).thenReturn(false);

        TextureClickQuestionData question = new TextureClickQuestionData();
        question.setQuestionText("Otázka");
        question.setType(QuestionTypeEnum.TEXTURE_CLICK);
        question.setPoints(1);
        question.setModelId("model-1");
        question.setTextureId("texture-1");

        TextureClickAnswerData answer = new TextureClickAnswerData();
        answer.setType(QuestionTypeEnum.TEXTURE_CLICK);
        answer.setHexColor("#ffffff");

        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(question)),
                new ArrayList<>(List.of(answer))))
                .isInstanceOf(BackendException.NotFound.class)
                .hasMessageContaining("model, který neexistuje");
    }

    @Test
    void rejectsAnAnswerOfADifferentTypeThanItsQuestion() {
        AbstractQuestionData question = singleChoiceQuestion(1);
        MultipleChoiceAnswerData answer = new MultipleChoiceAnswerData();
        answer.setType(QuestionTypeEnum.MULTIPLE_CHOICE);
        answer.setCorrectItems(List.of(0));

        assertThatThrownBy(() -> validator.validate(
                new ArrayList<>(List.of(question)),
                new ArrayList<>(List.of(answer))))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("jiného typu");
    }

    private SingleChoiceQuestionData singleChoiceQuestion(int points) {
        SingleChoiceQuestionData question = new SingleChoiceQuestionData();
        question.setQuestionText("Otázka");
        question.setType(QuestionTypeEnum.SINGLE_CHOICE);
        question.setPoints(points);
        question.setOptions(List.of("a", "b"));
        return question;
    }

    private SingleChoiceAnswerData singleChoiceAnswer(int correctIndex) {
        SingleChoiceAnswerData answer = new SingleChoiceAnswerData();
        answer.setType(QuestionTypeEnum.SINGLE_CHOICE);
        answer.setCorrectIndex(correctIndex);
        return answer;
    }
}
