package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import cz.uhk.zlesak.threejslearningapp.components.containers.ModelSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelTextureAreaSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.TextureQuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Editor for texture click questions.
 */
public class TextureClickQuestionEditor extends QuestionEditorBase<TextureQuestionOption> {

    private final ModelSelectContainer modelSelectContainer;
    private final ModelTextureAreaSelectContainer modelTextureAreaSelectContainer;

    /**
     * Constructor for TextureClickQuestionEditor.
     * @param loadModelDataConsumer consumer to load model data
     */
    public TextureClickQuestionEditor(Consumer<Map<String, QuickModelEntity>> loadModelDataConsumer) {
        super(QuestionTypeEnum.TEXTURE_CLICK);
        modelTextureAreaSelectContainer = new ModelTextureAreaSelectContainer();
        this.modelSelectContainer = new ModelSelectContainer(
                "Select Model",
                "model-select",
                false,
                loadModelDataConsumer
        );
        actionsLayout.remove(addOptionButton);
        actionsLayout.addComponentAtIndex(1, modelSelectContainer);

        optionsLayout.add(modelTextureAreaSelectContainer);
    }

    /**
     * Adds an option to the question.
     */
    @Override
    public void addOption() {
        throw new NotImplementedException("Adding options is not supported for TextureClickQuestionEditor. Directed by ModelTextureAreaColorSelect container.");
    }

    /**
     * @param index the index of the option
     * @return the created TextureQuestionOption
     */
    @Override
    protected TextureQuestionOption createOption(int index) {
        return new TextureQuestionOption(index, "quiz.option.label");
    }

    /**
     * Removes an option from the question.
     *
     * @param id the ID of the option to remove
     */
    @Override
    public void removeOption(String id) {
        throw new NotImplementedException("Removing options is not supported for TextureClickQuestionEditor. Directed by ModelTextureAreaColorSelect container.");
    }

    /**
     * Updates the correct answer group based on current options.
     */
    @Override
    void updateCorrectAnswerGroup() {
        throw new NotImplementedException("Updating correct answer group is not supported for TextureClickQuestionEditor. Directed by ModelTextureAreaColorSelect container.");
    }

    /**
     * Gets the question data.
     * @return the question data
     */
    @Override
    public AbstractQuestionData getQuestionData() {
        return TextureClickQuestionData.builder()
                .questionId(questionId)
                .type(questionType)
                .questionText(getQuestionText())
                .points(getPoints())
                .modelId(modelTextureAreaSelectContainer.getModelListingSelect().getValue().id())
                .textureId(modelTextureAreaSelectContainer.getTextureListingSelect().getValue().textureId())
                .build();

    }

    /**
     * Gets the answer data for the question.
     * @return the answer data
     */
    @Override
    public AbstractAnswerData getAnswerData() {
        return TextureClickAnswerData.builder()
                .questionId(questionId)
                .type(questionType)
                .modelId(modelTextureAreaSelectContainer.getModelListingSelect().getValue().id())
                .textureId(modelTextureAreaSelectContainer.getTextureListingSelect().getValue().textureId())
                .hexColor(modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().hexColor())
                .build();
    }

    /**
     * Validates the question data.
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (modelTextureAreaSelectContainer.getModelListingSelect().getValue() == null || modelTextureAreaSelectContainer.getModelListingSelect().getValue().id().isEmpty()) {
            return false;
        }
        if (modelTextureAreaSelectContainer.getTextureListingSelect().getValue() == null || modelTextureAreaSelectContainer.getTextureListingSelect().getValue().textureId().isEmpty()) {
            return false;
        }
        return modelTextureAreaSelectContainer.getTextureAreaSelect().getValue() != null && !modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().textureId().isEmpty();
    }

    /**
     * Gets the selected model from the model select container.
     *
     * @return The selected QuickModelEntity, or null if none is selected
     */
    public QuickModelEntity getSelectedModel() {
        return modelSelectContainer.getSelect().getValue();
    }
}

