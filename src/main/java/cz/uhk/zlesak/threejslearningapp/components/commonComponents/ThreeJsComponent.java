package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.TextureClickedEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;
import cz.uhk.zlesak.threejslearningapp.security.AccessTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * This component integrates Three.js into a Vaadin application.
 * It allows for rendering 3D models and handling user interactions.
 * This class main purpose is to provide a bridge between the Java backend and the JavaScript Three.js library.
 */
@Slf4j
@JsModule("./js/threejs/three-javascript.ts")
@NpmPackage(value = "three", version = "0.182.0")
@Tag("canvas")
@Scope("prototype")
@org.springframework.stereotype.Component
public class ThreeJsComponent extends Component {

    private Runnable onDisposedCallback;
    protected final List<Registration> registrations = new ArrayList<>();

    /**
     * Default constructor for ThreeJsComponent.
     */
    public ThreeJsComponent() {
        addAttachListener(e -> init());
    }

    /**
     * Initializes the Three.js component by executing the JavaScript initialization function.
     * This method is called automatically when the component is created.
     * Further initialization is done in the JavaScript side, where the Three.js scene, camera, and renderer are set up.
     */
    private void init() {
        getElement().executeJs("""
                try {
                    if (typeof window.initThree === 'function') {
                        window.initThree($0);
                    }
                } catch (e) {
                    console.error('[JS] Error in initThree:', e);
                }
                """, getElement());
    }

    /**
     * Disposes of the Three.js component.
     * This is crucial for cleaning up resources and preventing memory leaks and memory blockages.
     * It calls the JavaScript function to dispose of the Three.js scene and renderer.
     * After the disposal is complete, it triggers a server-side callback to notify that the component has been disposed of.
     *
     * @param onDisposed a callback that will be executed after the component is disposed of.
     */
    public void dispose(SerializableRunnable onDisposed) {
        this.onDisposedCallback = onDisposed;
        getElement().executeJs("""
                window.disposeThree($0).then(() => {
                    $1.$server.notifyDisposed();
                })
                """, getElement(), this);
    }

    /**
     * This method is called from the JavaScript side to notify the server that the component has been disposed of.
     * It executes the onDisposedCallback if it is set, allowing for any additional cleanup or actions to be performed after disposal.
     * There aro no other cleanup actions set up that would be needed to be done to properly dispose of the component as of now.
     */
    @ClientCallable
    private void notifyDisposed() {
        if (this.onDisposedCallback != null) {
            this.onDisposedCallback.run();
        }
    }

    /**
     * Loads an advanced 3D model into the Three.js scene.
     * This method expects two base64 encoded strings: one for the object data and one for the texture data.
     * It calls the JavaScript function loadAdvancedModel to handle the loading process.
     * This method is used for models that require both an object file and a texture file.
     * This allows for models to be loaded into the scene with multiple textures.
     * This loading methods needs only the main texture, as other may not be provided.
     * Other textures can be added later using the addOtherTexture method.
     *
     * @param modelUrl the base64 encoded string of the model data.
     * @param modelId  id of the loaded model.
     */
    private void loadModel(String modelUrl, String modelId, boolean mainModel, boolean advanced, String... questionId) {
        getElement().executeJs("""
                try {
                    if (typeof window.loadModel === 'function') {
                        window.loadModel($0, $1, $2, $3, $4, $5).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in loadAdvancedModel:', e);
                }
                """, getElement(), modelUrl, modelId, mainModel, questionId.length > 0 ? questionId[0] : null, advanced);
    }

    private void removeModel(String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.removeModel === 'function') {
                        window.removeModel($0, $1);
                    }
                } catch (e) {
                    console.error('[JS] Error in clearModel:', e);
                }
                """, getElement(), modelId);
    }

    /**
     * Adds the main texture to the Three.js scene.
     * This method expects a base64 encoded string of the texture data.
     * It calls the JavaScript function addMainTexture to handle the addition of the main texture.
     * This is used to apply the main texture to models in the scene.
     *
     * @param mainTexture the base64 encoded string of the main texture data.
     * @param modelId     id of the loaded model.
     */
    private void addMainTexture(String mainTexture, String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.addMainTexture === 'function') {
                        window.addMainTexture($0, $1, $2).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTexture:', e);
                }
                """, getElement(), mainTexture, modelId);
    }

    /**
     * Removes the main texture from the Three.js scene.
     * This method calls the JavaScript function removeMainTexture to handle the removal process.
     * It is used to delete the main texture that is currently applied to the model in the scene.
     *
     * @param modelId id of the loaded model.
     */
    private void removeMainTexture(String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.removeMainTexture === 'function') {
                        window.removeMainTexture($0, $1).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTexture:', e);
                }
                """, getElement(), modelId);
    }

    /**
     * Adds a texture to the Three.js scene.
     * This method expects a base64 encoded string of the texture data.
     * It calls the JavaScript function addTexture to handle the addition of the texture.
     * This is used to apply textures to models in the scene.
     *
     * @param otherTextureUrl the base64 encoded string of the texture data.
     * @param textureId       identification of the texture to be added
     * @param modelId         identification of the model the texture belongs to
     */
    private void addOtherTexture(String otherTextureUrl, String textureId, String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.addOtherTexture === 'function') {
                        window.addOtherTexture($0, $1, $2, $3).then(_ =>{});
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTexture:', e);
                }
                """, getElement(), otherTextureUrl, textureId, modelId);
    }

    /**
     * Removes a texture from the Three.js scene based on its identifier.
     * This method calls the JavaScript function removeOtherTexture to handle the removal process.
     * It is used to delete textures that are no longer needed or to free up resources.
     *
     * @param textureId identification of the texture to be deleted
     * @param modelId   identification of the model the texture belongs to
     * @see ModelUploadForm for usage context
     */
    private void removeOtherTexture(String modelId, String textureId) {
        if (textureId.isEmpty() || modelId.isEmpty()) return;
        getElement().executeJs("""
                try {
                    if (typeof window.removeOtherTexture === 'function') {
                        window.removeOtherTexture($0, $1, $2).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in removeOtherTexture:', e);
                }
                """, getElement(), modelId, textureId);
        switchToMainTexture(modelId);
    }

    /**
     * Switches the currently displayed texture to another texture in the Three.js scene.
     * This method calls the JavaScript function switchOtherTexture to handle the switching process.
     * It is used to change the texture of the currently selected model or object in the scene.
     *
     * @param textureId identification of the texture to be switched to
     * @param modelId   identification of the model the texture belongs to
     */
    private void switchOtherTexture(String modelId, String textureId) {
        getElement().executeJs("""
                try {
                    if (typeof window.switchOtherTexture === 'function') {
                        window.switchOtherTexture($0, $1, $2).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in switchOtherTexture:', e);
                }
                """, getElement(), modelId, textureId);
    }

    /**
     * Switches the currently displayed 3D model in the Three.js scene.
     * This method calls the JavaScript function showModel to handle the model switching process.
     * It is used to change the model being displayed in the scene.
     *
     * @param modelId identification of the model to be displayed
     */
    private void showModel(String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.showModel === 'function') {
                        window.showModel($0, $1).then(_ => {});
//                        window.showModel($0, $1);
                    }
                } catch (e) {
                    console.error('[JS] Error in switchOtherModel:', e);
                }
                """, getElement(), modelId);
    }

    private void switchToMainTexture(String modelId) {
        getElement().executeJs("""
                try {
                    if (typeof window.switchToMainTexture === 'function') {
                        window.switchToMainTexture($0, $1).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in switchOtherTexture:', e);
                }
                """, getElement(), modelId);
    }

    /**
     * Applies a mask to the main texture of the currently displayed model in the Three.js scene.
     * This method calls the JavaScript function applyMaskToMainTexture to handle the masking process.
     * The maskColor parameter is expected to be a string representing the color to be applied as a mask.
     * This is used to visually modify the main texture by applying a color mask.
     * This is needed as the user can choose a color to be applied as a mask to the main texture based on the provided colors defining parts of the model.
     *
     * @param modelId   identification of the model to which the texture belongs.
     * @param textureId identification of the texture to which the mask will be applied.
     * @param maskColor the color to be applied as a mask to the main texture.
     *
     */
    private void applyMaskToMainTexture(String modelId, String textureId, String maskColor) {
        getElement().executeJs("""
                try {
                    if (typeof window.applyMaskToMainTexture === 'function') {
                        window.applyMaskToMainTexture($0, $1, $2, $3, $4).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in applyMaskToMainTexture:', e);
                }
                """, getElement(), modelId, textureId, maskColor);
    }

    /**
     * Clears the specified model from the Three.js memory.
     *
     * @param modelId identification of the model to be cleared
     */
    private void clearModel(String modelId, String questionId) {
        getElement().executeJs("""
                try {
                    if (typeof window.clearModel === 'function') {
                        window.clearModel($0, $1, $2, $3).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in clearModel:', e);
                }
                """, getElement(), modelId, questionId, false);
    }

    /**
     * This method is called from the JavaScript side when a color is picked by the user.
     * As of now it logs the selected color to the console and can be used to trigger further actions based on the chosen color.
     * This is a precondition to functionality of exercises
     *
     * @param modelId   the id of the model where the color was picked.
     * @param textureId the id of the texture where the color was picked.
     * @param hexColor  the selected color in hexadecimal format.
     */
    @ClientCallable
    public void onColorPicked(String modelId, String textureId, String hexColor, String questionId) {
        if (questionId == null || questionId.isBlank()) {
            fireEvent(new TextureClickedEvent(this, null, modelId, textureId, hexColor));
        } else {
            ComponentUtil.fireEvent(UI.getCurrent(), new TextureClickedEvent(this, questionId, modelId, textureId, hexColor));
        }
    }

    /**
     * This method is called from the JavaScript side when the renderer starts performing actions.
     */
    @ClientCallable
    public void doingActions(String actionDescription) {
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsDoingActions(this, actionDescription));
    }

    /**
     * This method is called from the JavaScript side when the renderer finishes performing actions.
     */
    @ClientCallable
    public void finishedActions() {
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsFinishedActions(this));
    }

    /**
     * This method is called from the JavaScript side to retrieve a valid access token for authentication.
     * Token is provided dynamicaly not saved in JS for security reasons
     *
     * @return a valid access token as a String.
     */
    @ClientCallable
    public String getToken() {
        return SpringContextUtils.getBean(AccessTokenProvider.class).getValidAccessToken();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                UploadFileEvent.class,
                event -> {
                    switch (event.getFileType()) {
                        case MODEL -> {
                            loadModel(event.getBase64File(), event.getModelId(), event.isMain(), event.isAdvanced(), event.getQuestionId());
                            if (event.isFromClient()){
                                showModel(event.getModelId());
                            }
                        }
                        case OTHER -> {
                            addOtherTexture(event.getBase64File(), event.getEntityId(), event.getModelId());
                            if (event.isFromClient()){
                                switchOtherTexture(event.getModelId(), event.getEntityId());
                            }
                        }
                        case MAIN -> {
                            addMainTexture(event.getBase64File(), event.getModelId());
                            if (event.isFromClient()){
                                switchToMainTexture(event.getModelId());
                            }
                        }
                        case CSV -> { /* CSV files are not handled in ThreeJs component */ }
                        default -> log.warn("Unsupported file type for upload: {}", event.getFileType());
                    }
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                RemoveFileEvent.class,
                event -> {
                    switch (event.getFileType()) {
                        case MODEL -> removeModel(event.getModelId());
                        case OTHER -> removeOtherTexture(event.getModelId(), event.getEntityId());
                        case MAIN -> removeMainTexture(event.getModelId());
                        case CSV -> { /* CSV files are not handled in ThreeJs component */ }
                        default -> log.warn("Unsupported file type for removal: {}", event.getFileType());
                    }
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsActionEvent.class,
                event -> {
                    if (!event.isFromClient()) return;
                    switch (event.getAction()) {
                        case SWITCH_OTHER_TEXTURE -> switchOtherTexture(event.getModelId(), event.getTextureId());
                        case SHOW_MODEL -> showModel(event.getModelId());
                        case APPLY_MASK_TO_TEXTURE ->
                                applyMaskToMainTexture(event.getModelId(), event.getTextureId(), event.getMaskColor());
                        case REMOVE -> clearModel(event.getModelId(), event.getQuestionId());
                        default -> { /* No action */}
                    }
                }
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
