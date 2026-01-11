package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelTextureAreaSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.TextureQuestionOption;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.AbstractAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.answer.TextureClickAnswerData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.AbstractQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Editor for texture click questions.
 */
public class TextureClickQuestionEditor extends QuestionEditorBase<TextureQuestionOption> {

    private final ModelTextureAreaSelectContainer modelTextureAreaSelectContainer = new ModelTextureAreaSelectContainer();

    /**
     * Constructor for TextureClickQuestionEditor.
     */
    public TextureClickQuestionEditor() {
        super(QuestionTypeEnum.TEXTURE_CLICK);
        modelTextureAreaSelectContainer.setQuestionId(questionId);
        ModelSelectContainer modelSelectContainer = new ModelSelectContainer(
                "Select Model",
                questionId,
                false,
                false
        );
        actionsLayout.remove(addOptionButton);
        actionsLayout.addComponentAtIndex(1, modelSelectContainer);

        optionsLayout.add(modelTextureAreaSelectContainer);
    }

    /**
     * Adds an option to the question.
     */
    @Override
    public void addOption(String... value) {
        throw new NotImplementedException("Adding options is not supported for TextureClickQuestionEditor. Directed by ModelTextureAreaColorSelect container.");
    }

    /**
     * @param index the index of the option
     * @return the created TextureQuestionOption
     */
    @Override
    protected TextureQuestionOption createOption(int index, String... value) {
        return new TextureQuestionOption(index, "quiz.option.label", value);
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

    @Override //TODO
    public void initialize(AbstractQuestionData questionData) {
        super.initialize(questionData);
        if (questionData instanceof TextureClickQuestionData data) {
            var quickModelEntity = QuickModelEntity.builder()
                    .model(ModelFileEntity.builder().id(data.getModelId()).build())
                    .isAdvanced(true)
                    .build();

            ComponentUtil.fireEvent(UI.getCurrent(), new ModelLoadEvent(UI.getCurrent(), quickModelEntity, data.getQuestionId()));
        }
    }

    @Override
    public void setAnswerData(AbstractAnswerData answerData) {
        if (answerData instanceof TextureClickAnswerData data) {
            ComponentUtil.fireEvent(UI.getCurrent(),
                    new ThreeJsActionEvent(UI.getCurrent(), data.getModelId(), data.getTextureId(), ThreeJsActions.APPLY_MASK_TO_TEXTURE, false, data.getQuestionId(), data.getHexColor())); //TODO CHECK
        }
    }

    /**
     * Gets the question data.
     *
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
     *
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
     *
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

    public String getSelectedModelId() {
        if (modelTextureAreaSelectContainer.getModelListingSelect().getValue() == null) {
            return null;
        }
        return modelTextureAreaSelectContainer.getModelListingSelect().getValue().id();
    }

    public String getSelectedTextureId() {
        if (modelTextureAreaSelectContainer.getTextureAreaSelect().getValue() == null) {
            return null;
        }
        return modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().textureId();

    }

    public String getSelectedAreaId() {
        if (modelTextureAreaSelectContainer.getTextureAreaSelect().getValue() == null) {
            return null;
        }
        return modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().hexColor();

    }
}
