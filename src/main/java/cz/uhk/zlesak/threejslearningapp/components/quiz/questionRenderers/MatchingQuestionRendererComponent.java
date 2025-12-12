package cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers;

import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.MatchingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.MatchingSubmissionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer for matching questions during quiz taking.
 */
public class MatchingQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private final Map<Integer, Select<Integer>> matchSelects = new HashMap<>();
    private final MatchingQuestionData question;

    /**
     * Constructor for MatchingQuestionRendererComponent.
     * @param question the matching question data
     */
    MatchingQuestionRendererComponent(MatchingQuestionData question) {
        this.question = question;

        for (int i = 0; i < question.getLeftItems().size(); i++) {
            String leftItem = question.getLeftItems().get(i);
            Select<Integer> select = new Select<>();
            select.setLabel(leftItem);
            select.setItems(getIndices(question.getRightItems().size()));
            select.setItemLabelGenerator(idx -> question.getRightItems().get(idx));
            select.setPlaceholder(text("quiz.matching.select"));

            select.addValueChangeListener(e -> {
                if (answerChangedListener != null) {
                    answerChangedListener.accept(getSubmissionData());
                }
            });

            matchSelects.put(i, select);
            add(select);
        }
    }

    /**
     * Gets the submission data for the matching question.
     * @return the matching submission data
     */
    @Override
    public MatchingSubmissionData getSubmissionData() {
        Map<Integer, Integer> matches = new HashMap<>();
        matchSelects.forEach((leftIdx, select) -> {
            if (select.getValue() != null) {
                matches.put(leftIdx, select.getValue());
            }
        });
        return MatchingSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .matches(matches)
                .build();
    }

    /**
     * Generates a list of indices from 0 to count-1.
     * @param count the number of indices to generate
     * @return the list of indices
     */
    private List<Integer> getIndices(int count) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        return indices;
    }
}
