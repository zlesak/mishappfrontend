package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.OrderingQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.OrderingSubmissionData;

import java.util.*;

/**
 * OrderingQuestionRendererComponent Class - Renders an ordering question using select dropdowns.
 * Items are shuffled to prevent users from seeing the correct order.
 */
public class OrderingQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private final List<Select<Integer>> orderSelects = new ArrayList<>();
    private final OrderingQuestionData question;
    private final Map<Integer, Integer> shuffledIndexMap = new HashMap<>();

    /**
     * Constructor for OrderingQuestionRendererComponent.
     * @param question the ordering question data to be rendered
     */
    OrderingQuestionRendererComponent(OrderingQuestionData question) {
        this.question = question;

        List<Integer> shuffledIndices = new ArrayList<>();
        for (int i = 0; i < question.getItems().size(); i++) {
            shuffledIndices.add(i);
        }
        Collections.shuffle(shuffledIndices);

        for (int shuffledPos = 0; shuffledPos < shuffledIndices.size(); shuffledPos++) {
            int originalIndex = shuffledIndices.get(shuffledPos);
            shuffledIndexMap.put(shuffledPos, originalIndex);

            String item = question.getItems().get(originalIndex);
            Select<Integer> select = new Select<>();
            select.setLabel(item);
            select.setItems(getIndices(question.getItems().size()));
            select.setItemLabelGenerator(idx -> text("quiz.ordering.position") + " " + (idx + 1));
            select.setPlaceholder(text("quiz.ordering.selectPosition"));

            select.addValueChangeListener(e -> {
                if (answerChangedListener != null) {
                    answerChangedListener.accept(getSubmissionData());
                }
            });

            orderSelects.add(select);
            add(select);
        }
    }

    /**
     * Gets the submission data for the ordering question.
     * Maps the shuffled positions back to original indices.
     * @return the ordering submission data
     */
    @Override
    public OrderingSubmissionData getSubmissionData() {
        Integer[] order = new Integer[orderSelects.size()];

        for (int shuffledPos = 0; shuffledPos < orderSelects.size(); shuffledPos++) {
            Select<Integer> select = orderSelects.get(shuffledPos);
            int originalIndex = shuffledIndexMap.get(shuffledPos);

            if (select.getValue() != null) {
                order[originalIndex] = select.getValue();
            } else {
                order[originalIndex] = -1;
            }
        }

        return OrderingSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .order(Arrays.asList(order))
                .build();
    }

    /**
     * Generates a list of indices from 0 to count - 1.
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
