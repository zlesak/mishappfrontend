package cz.uhk.zlesak.threejslearningapp.components.editors.question;

import com.vaadin.flow.component.ComponentUtil;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelTextureAreaSelectContainer;
import cz.uhk.zlesak.threejslearningapp.components.inputs.quizes.TextureQuestionOption;
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

import java.util.function.Function;

/**
 * Editor for texture click questions.
 */
public class TextureClickQuestionEditor extends QuestionEditorBase<TextureQuestionOption> {

    private final ModelTextureAreaSelectContainer modelTextureAreaSelectContainer = new ModelTextureAreaSelectContainer();
    private final Function<String, QuickModelEntity> modelResolver;
    private TextureClickQuestionData pendingModelLoadData;
    private TextureClickAnswerData pendingAnswerData;
    private String savedModelId;
    private String savedTextureId;
    private String savedHexColor;

    /**
     * Constructor for TextureClickQuestionEditor.
     */
    public TextureClickQuestionEditor(Function<String, QuickModelEntity> modelResolver) {
        super(QuestionTypeEnum.TEXTURE_CLICK);
        this.modelResolver = modelResolver;
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

    /**
     * Initializes the question editor with the provided question data, setting up the model and texture selections based on the saved state.
     *
     * @param questionData the question data to initialize the editor with
     */
    @Override
    public void initialize(AbstractQuestionData questionData) {
        super.initialize(questionData);
        modelTextureAreaSelectContainer.setQuestionId(questionId);
        if (questionData instanceof TextureClickQuestionData data) {
            pendingModelLoadData = data;
            savedModelId = data.getModelId();
            savedTextureId = data.getTextureId();
        }
    }

    /**
     * Sets the answer data for the question, storing it as pending data to be dispatched to the UI when it's ready.
     *
     * @param answerData AnswerData object
     */
    @Override
    public void setAnswerData(AbstractAnswerData answerData) {
        if (answerData instanceof TextureClickAnswerData data) {
            pendingAnswerData = data;
            if (savedModelId == null || savedModelId.isBlank()) {
                savedModelId = data.getModelId();
            }
            if (savedTextureId == null || savedTextureId.isBlank()) {
                savedTextureId = data.getTextureId();
            }
            savedHexColor = data.getHexColor();
        }
    }

    /**
     * Loads the saved selection on demand, dispatching any pending model load or answer data to the UI.
     */
    public void loadSavedSelectionOnDemand() {
        dispatchPendingData();
    }

    /**
     * Dispatches any pending model load or answer data to the UI, ensuring that the necessary events are fired to update the question editor's state.
     * This method is designed to be called when the UI is ready to receive updates, allowing for deferred loading of models and application of answer data without blocking the initialization process.
     * It checks for the presence of pending model load data and answer data, and if found, it fires the appropriate events to load the model and apply the answer data to the question editor.
     * After dispatching the data, it clears the pending data to prevent duplicate processing.
     */
    private void dispatchPendingData() {
        getUI().ifPresent(ui -> ui.beforeClientResponse(this, context -> {
            if (pendingModelLoadData != null) {
                QuickModelEntity quickModelEntity = modelResolver.apply(pendingModelLoadData.getModelId());
                ComponentUtil.fireEvent(ui, new ModelLoadEvent(ui, quickModelEntity, pendingModelLoadData.getQuestionId()));
                if (pendingModelLoadData.getTextureId() != null && !pendingModelLoadData.getTextureId().isBlank()) {
                    ComponentUtil.fireEvent(
                            ui,
                            new ThreeJsActionEvent(
                                    ui,
                                    pendingModelLoadData.getModelId(),
                                    pendingModelLoadData.getTextureId(),
                                    ThreeJsActions.SWITCH_OTHER_TEXTURE,
                                    true,
                                    pendingModelLoadData.getQuestionId()
                            )
                    );
                }
                pendingModelLoadData = null;
            }
            if (pendingAnswerData != null) {
                ComponentUtil.fireEvent(
                        ui,
                        new ThreeJsActionEvent(
                                ui,
                                pendingAnswerData.getModelId(),
                                pendingAnswerData.getTextureId(),
                                ThreeJsActions.APPLY_MASK_TO_TEXTURE,
                                true,
                                pendingAnswerData.getQuestionId(),
                                pendingAnswerData.getHexColor()
                        )
                );
                pendingAnswerData = null;
            }
        }));
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
                .modelId(resolveModelIdForSave())
                .textureId(resolveTextureIdForSave())
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
                .modelId(resolveModelIdForSave())
                .textureId(resolveTextureIdForSave())
                .hexColor(resolveHexColorForSave())
                .build();
    }

    /**
     * Validates the question data.
     * The validation checks ensure that the question text is not null or empty, and that valid selections for model ID, texture ID, and hex color are made.
     *
     * @return true if valid, false otherwise
     */
    @Override
    public boolean validate() {
        if (getQuestionText() == null || getQuestionText().isEmpty()) {
            return false;
        }
        if (isBlank(resolveModelIdForSave())) {
            return false;
        }
        if (isBlank(resolveTextureIdForSave())) {
            return false;
        }
        return !isBlank(resolveHexColorForSave());
    }

    public String getSelectedModelId() {
        return resolveModelIdForSave();
    }

    public String getSelectedTextureId() {
        return resolveTextureIdForSave();
    }

    public String getSelectedAreaId() {
        return resolveHexColorForSave();
    }

    /**
     * Resolves the model ID for saving, prioritizing the currently selected model ID from the UI.
     * If no new selection is made, it returns the saved model ID.
     *
     * @return the model ID to save, or null if no model ID is selected and no saved model ID exists
     */
    private String resolveModelIdForSave() {
        String selectedModelId = getSelectedModelIdRaw();
        if (!isBlank(selectedModelId)) {
            return selectedModelId;
        }
        return savedModelId;
    }

    /**
     * Resolves the texture ID for saving, prioritizing the currently selected texture ID from the UI.
     * If no new selection is made, it checks if the model selection has changed compared to the saved state.
     * If a new model is selected, it returns null to indicate that the texture ID should not be saved.
     * Otherwise, it returns the saved texture ID.
     *
     * @return the texture ID to save, or null if the model selection has changed and the texture ID should not be saved
     */
    private String resolveTextureIdForSave() {
        String selectedTextureId = getSelectedTextureIdRaw();
        if (!isBlank(selectedTextureId)) {
            return selectedTextureId;
        }

        String selectedModelId = getSelectedModelIdRaw();
        if (!isBlank(selectedModelId) && !selectedModelId.equals(savedModelId)) {
            return null;
        }
        return savedTextureId;
    }

    /**
     * Resolves the hex color for saving, prioritizing the currently selected hex color, then checking if the model or texture selection has changed compared to the saved state.
     * If a new model or texture is selected, it returns null to indicate that the hex color should not be saved.
     * Otherwise, it returns the saved hex color.
     *
     * @return the hex color to save, or null if the selection has changed and the hex color should not be saved
     */
    private String resolveHexColorForSave() {
        String selectedHexColor = getSelectedHexColorRaw();
        if (!isBlank(selectedHexColor)) {
            return selectedHexColor;
        }

        String selectedModelId = getSelectedModelIdRaw();
        if (!isBlank(selectedModelId) && !selectedModelId.equals(savedModelId)) {
            return null;
        }

        String selectedTextureId = getSelectedTextureIdRaw();
        if (!isBlank(selectedTextureId) && !selectedTextureId.equals(savedTextureId)) {
            return null;
        }
        return savedHexColor;
    }

    /**
     * Gets the selected model ID from the model listing select component.
     *
     * @return the selected model ID, or null if no model is selected
     */
    private String getSelectedModelIdRaw() {
        if (modelTextureAreaSelectContainer.getModelListingSelect().getValue() == null) {
            return null;
        }
        return modelTextureAreaSelectContainer.getModelListingSelect().getValue().id();
    }

    /**
     * Gets the selected texture ID from the texture area select component.
     *
     * @return the selected texture ID, or null if no texture is selected
     */
    private String getSelectedTextureIdRaw() {
        if (modelTextureAreaSelectContainer.getTextureAreaSelect().getValue() != null) {
            return modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().textureId();
        }
        if (modelTextureAreaSelectContainer.getTextureListingSelect().getValue() != null) {
            return modelTextureAreaSelectContainer.getTextureListingSelect().getValue().textureId();
        }
        return null;
    }

    /**
     * Gets the selected hex color from the texture area select component.
     *
     * @return the selected hex color, or null if no color is selected
     */
    private String getSelectedHexColorRaw() {
        if (modelTextureAreaSelectContainer.getTextureAreaSelect().getValue() == null) {
            return null;
        }
        return modelTextureAreaSelectContainer.getTextureAreaSelect().getValue().hexColor();
    }

    /**
     * Checks if a string is blank (null, empty, or only whitespace).
     *
     * @param value the string to check
     * @return true if the string is blank, false otherwise
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
