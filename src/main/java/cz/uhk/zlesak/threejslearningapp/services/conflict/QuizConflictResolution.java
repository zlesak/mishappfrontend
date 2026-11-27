package cz.uhk.zlesak.threejslearningapp.services.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizEntity;
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
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.SingleChoiceQuestionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Compares and combines two versions of a quiz: name, description, time limit, chapter, and the
 * questions.
 *
 * <p>Questions and their correct answers are one field. Answers are matched to questions by id, so
 * one version's questions with the other's answers would grade against answers to different
 * questions.
 */
public class QuizConflictResolution implements ConflictResolution<QuizEntity> {

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TIME_LIMIT = "timeLimit";
    public static final String CHAPTER = "chapter";
    public static final String QUESTIONS = "questions";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Translator translator;

    public QuizConflictResolution(Translator translator) {
        this.translator = translator;
    }

    @Override
    public List<ConflictField> differences(QuizEntity mine, QuizEntity theirs) {
        List<ConflictField> fields = new ArrayList<>();
        if (!Objects.equals(mine.getName(), theirs.getName())) {
            fields.add(field(NAME, mine.getName(), theirs.getName()));
        }
        if (!Objects.equals(blankToNull(mine.getDescription()), blankToNull(theirs.getDescription()))) {
            fields.add(field(DESCRIPTION, mine.getDescription(), theirs.getDescription()));
        }
        if (!Objects.equals(limit(mine), limit(theirs))) {
            fields.add(field(TIME_LIMIT, timeLimit(mine), timeLimit(theirs)));
        }
        if (!Objects.equals(blankToNull(mine.getChapterId()), blankToNull(theirs.getChapterId()))) {
            fields.add(field(CHAPTER, chapter(mine), chapter(theirs)));
        }
        if (!Objects.equals(comparable(mine), comparable(theirs))) {
            fields.add(field(QUESTIONS, questions(mine), questions(theirs)));
        }
        return fields;
    }

    @Override
    public QuizEntity merge(QuizEntity mine, QuizEntity theirs, Map<String, ConflictChoice> choices) {
        QuizEntity name = ConflictResolution.takesTheirs(choices, NAME) ? theirs : mine;
        QuizEntity description = ConflictResolution.takesTheirs(choices, DESCRIPTION) ? theirs : mine;
        QuizEntity timeLimit = ConflictResolution.takesTheirs(choices, TIME_LIMIT) ? theirs : mine;
        QuizEntity chapter = ConflictResolution.takesTheirs(choices, CHAPTER) ? theirs : mine;
        QuizEntity questions = ConflictResolution.takesTheirs(choices, QUESTIONS) ? theirs : mine;

        return QuizEntity.builder()
                .id(theirs.getId())
                .name(name.getName())
                .description(description.getDescription())
                .timeLimit(timeLimit.getTimeLimit())
                .chapterId(chapter.getChapterId())
                .chapterName(chapter.getChapterName())
                .questions(questions.getQuestions())
                .answers(questions.getAnswers())
                .creatorId(theirs.getCreatorId())
                .created(theirs.getCreated())
                .updated(theirs.getUpdated())
                .build();
    }

    private ConflictField field(String key, String mine, String theirs) {
        return new ConflictField(key, translator.text("conflict.quiz.field." + key), mine, theirs);
    }

    private static Integer limit(QuizEntity quiz) {
        return quiz.getTimeLimit() == null ? 0 : quiz.getTimeLimit();
    }

    private String timeLimit(QuizEntity quiz) {
        int minutes = limit(quiz);
        return minutes == 0 ? translator.text("conflict.quiz.noTimeLimit") : translator.text("conflict.quiz.minutes", minutes);
    }

    private String chapter(QuizEntity quiz) {
        if (blankToNull(quiz.getChapterId()) == null) {
            return translator.text("conflict.quiz.noChapter");
        }
        return quiz.getChapterName() != null && !quiz.getChapterName().isBlank() ? quiz.getChapterName() : quiz.getChapterId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Questions and answers as data, without their ids: an editor may assign new ids to questions it
     * rebuilt, and that alone is not a change the author made.
     */
    private static JsonNode comparable(QuizEntity quiz) {
        ObjectNode node = JSON.createObjectNode();
        node.set("questions", withoutIds(JSON.valueToTree(quiz.getQuestions() == null ? List.of() : quiz.getQuestions())));
        node.set("answers", withoutIds(JSON.valueToTree(quiz.getAnswers() == null ? List.of() : quiz.getAnswers())));
        return node;
    }

    private static JsonNode withoutIds(JsonNode items) {
        items.forEach(item -> {
            if (item instanceof ObjectNode object) {
                object.remove("questionId");
            }
        });
        return items;
    }

    /** One block per question: its wording, type and points, then what counts as correct. */
    private String questions(QuizEntity quiz) {
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            return "";
        }
        Map<String, AbstractAnswerData> answers = quiz.getAnswers() == null ? Map.of()
                : quiz.getAnswers().stream().filter(Objects::nonNull).filter(answer -> answer.getQuestionId() != null)
                .collect(Collectors.toMap(AbstractAnswerData::getQuestionId, answer -> answer, (first, second) -> first));

        List<String> blocks = new ArrayList<>();
        int number = 1;
        for (AbstractQuestionData question : quiz.getQuestions()) {
            StringBuilder block = new StringBuilder()
                    .append(number++).append(". ").append(Objects.toString(question.getQuestionText(), ""))
                    .append(" (").append(typeLabel(question.getType()))
                    .append(", ").append(translator.text("conflict.quiz.points", question.getPoints() == null ? 0 : question.getPoints()))
                    .append(')');
            String correct = correct(question, answers.get(question.getQuestionId()));
            if (!correct.isBlank()) {
                block.append('\n').append(correct);
            }
            blocks.add(block.toString());
        }
        return String.join("\n\n", blocks);
    }

    private String correct(AbstractQuestionData question, AbstractAnswerData answer) {
        if (question instanceof SingleChoiceQuestionData single) {
            Integer index = answer instanceof SingleChoiceAnswerData data ? data.getCorrectIndex() : null;
            return options(single.getOptions(), index == null ? List.of() : List.of(index));
        }
        if (question instanceof MultipleChoiceQuestionData multiple) {
            List<Integer> indices = answer instanceof MultipleChoiceAnswerData data && data.getCorrectItems() != null
                    ? data.getCorrectItems() : List.of();
            return options(multiple.getOptions(), indices);
        }
        if (answer instanceof OpenTextAnswerData open) {
            return translator.text("conflict.quiz.accepted",
                    String.join(", ", open.getAcceptableAnswers() == null ? List.of() : open.getAcceptableAnswers()));
        }
        if (question instanceof OrderingQuestionData ordering && answer instanceof OrderingAnswerData order) {
            List<String> items = ordering.getItems() == null ? List.of() : ordering.getItems();
            List<Integer> sequence = order.getCorrectOrder() == null ? List.of() : order.getCorrectOrder();
            return translator.text("conflict.quiz.order", sequence.stream()
                    .map(index -> index != null && index >= 0 && index < items.size() ? items.get(index) : String.valueOf(index))
                    .collect(Collectors.joining(" → ")));
        }
        if (question instanceof MatchingQuestionData matching && answer instanceof MatchingAnswerData match) {
            List<String> left = matching.getLeftItems() == null ? List.of() : matching.getLeftItems();
            List<String> right = matching.getRightItems() == null ? List.of() : matching.getRightItems();
            return (match.getCorrectMatches() == null ? Map.<Integer, Integer>of() : match.getCorrectMatches())
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(pair -> item(left, pair.getKey()) + " → " + item(right, pair.getValue()))
                    .collect(Collectors.joining("\n"));
        }
        if (answer instanceof TextureClickAnswerData area) {
            return translator.text("conflict.quiz.area", Objects.toString(area.getHexColor(), ""));
        }
        return "";
    }

    private String options(List<String> options, List<Integer> correct) {
        if (options == null) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            lines.add("- " + options.get(index) + (correct.contains(index) ? " " + translator.text("conflict.quiz.correct") : ""));
        }
        return String.join("\n", lines);
    }

    private static String item(List<String> items, Integer index) {
        return index != null && index >= 0 && index < items.size() ? items.get(index) : String.valueOf(index);
    }

    private String typeLabel(QuestionTypeEnum type) {
        if (type == null) {
            return "";
        }
        return translator.text(switch (type) {
            case SINGLE_CHOICE -> "quiz.questionType.singleChoice";
            case MULTIPLE_CHOICE -> "quiz.questionType.multipleChoice";
            case OPEN_TEXT -> "quiz.questionType.openText";
            case MATCHING -> "quiz.questionType.matching";
            case ORDERING -> "quiz.questionType.ordering";
            case TEXTURE_CLICK -> "quiz.questionType.textureClick";
        });
    }
}
