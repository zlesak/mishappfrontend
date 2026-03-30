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
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsLoadingProgress;
import cz.uhk.zlesak.threejslearningapp.security.AccessTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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

    private static final long PROGRESS_EVENT_MIN_INTERVAL_MS = 150;
    private static final long ACTION_EVENT_MIN_INTERVAL_MS = 200;

    private Runnable onDisposedCallback;
    protected final List<Registration> registrations = new ArrayList<>();
    private ExecutorService jsDispatchExecutor;
    private long lastProgressEventAtMs = 0L;
    private int lastProgressPercent = Integer.MIN_VALUE;
    private String lastProgressDescription = null;
    private long lastDoingActionAtMs = 0L;
    private String lastDoingActionDescription = null;
    private final AtomicLong callbackRequestSequence = new AtomicLong(0);
    private final Map<String, Consumer<String>> thumbnailCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Consumer<String>> backgroundSpecCallbacks = new ConcurrentHashMap<>();

    /**
     * Default constructor for ThreeJsComponent.
     */
    public ThreeJsComponent() {
        addAttachListener(e -> init());
    }

    private synchronized ExecutorService ensureJsDispatchExecutor() {
        if (jsDispatchExecutor == null || jsDispatchExecutor.isShutdown()) {
            ThreadFactory tf = runnable -> {
                Thread t = new Thread(runnable, "threejs-js-dispatch");
                t.setDaemon(true);
                return t;
            };
            jsDispatchExecutor = Executors.newSingleThreadExecutor(tf);
        }
        return jsDispatchExecutor;
    }

    private synchronized void shutdownJsDispatchExecutor() {
        if (jsDispatchExecutor != null) {
            jsDispatchExecutor.shutdownNow();
            jsDispatchExecutor = null;
        }
    }

    private void dispatchJsAsync(String script, Serializable... args) {
        final UI ui = getUI().orElse(null);
        if (ui == null || !isAttached()) return;

        ensureJsDispatchExecutor().submit(() -> ui.access(() -> {
            if (!isAttached()) return;
            getElement().executeJs(script, args);
        }));
    }

    /**
     * Initializes the Three.js component by executing the JavaScript initialization function.
     * This method is called automatically when the component is created.
     * Further initialization is done in the JavaScript side, where the Three.js scene, camera, and renderer are set up.
     */
    private void init() {
        dispatchJsAsync("""
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
        dispatchJsAsync("""
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
     * Loads a 3D model into the Three.js scene.
     * The correct loader (GLTF vs OBJ) is determined on the JS side by reading magic bytes.
     *
     * @param modelUrl  the base64 encoded string or URL of the model data.
     * @param modelId   id of the loaded model.
     * @param mainModel whether this is the primary model.
     */
    private void loadModel(String modelUrl, String modelId, boolean mainModel, String... questionId) {
        dispatchJsAsync("""
                try {
                    if (typeof window.loadModel === 'function') {
                        window.loadModel($0, $1, $2, $3, $4).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in loadModel:', e);
                }
                """, getElement(), modelUrl, modelId, mainModel, questionId.length > 0 ? questionId[0] : null);
    }

    private void loadModelAndShow(String modelUrl, String modelId, boolean mainModel, String... questionId) {
        dispatchJsAsync("""
                try {
                    if (typeof window.loadModel === 'function') {
                        window.loadModel($0, $1, $2, $3, $4)
                            .then(() => {
                                if (typeof window.showModel === 'function') {
                                    return window.showModel($0, $2);
                                }
                            })
                            .then(_ => {})
                            .catch(e => console.error('[JS] Error in loadModelAndShow chain:', e));
                    }
                } catch (e) {
                    console.error('[JS] Error in loadModelAndShow:', e);
                }
                """, getElement(), modelUrl, modelId, mainModel, questionId.length > 0 ? questionId[0] : null);
    }

    private void removeModel(String modelId) {
        dispatchJsAsync("""
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
        dispatchJsAsync("""
                try {
                    if (typeof window.addMainTexture === 'function') {
                        window.addMainTexture($0, $1, $2).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTexture:', e);
                }
                """, getElement(), mainTexture, modelId);
    }

    private void addMainTextureAndSwitch(String mainTexture, String modelId) {
        dispatchJsAsync("""
                try {
                    if (typeof window.addMainTexture === 'function') {
                        window.addMainTexture($0, $1, $2)
                            .then(() => {
                                if (typeof window.switchToMainTexture === 'function') {
                                    return window.switchToMainTexture($0, $2);
                                }
                            })
                            .then(_ => {})
                            .catch(e => console.error('[JS] Error in addMainTextureAndSwitch chain:', e));
                    }
                } catch (e) {
                    console.error('[JS] Error in addMainTextureAndSwitch:', e);
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
        dispatchJsAsync("""
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
        dispatchJsAsync("""
                try {
                    if (typeof window.addOtherTexture === 'function') {
                        window.addOtherTexture($0, $1, $2, $3).then(_ =>{});
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTexture:', e);
                }
                """, getElement(), otherTextureUrl, textureId, modelId);
    }

    private void addOtherTextureAndSwitch(String otherTextureUrl, String textureId, String modelId) {
        dispatchJsAsync("""
                try {
                    if (typeof window.addOtherTexture === 'function') {
                        window.addOtherTexture($0, $1, $2, $3)
                            .then(() => {
                                if (typeof window.switchOtherTexture === 'function') {
                                    return window.switchOtherTexture($0, $3, $2);
                                }
                            })
                            .then(_ => {})
                            .catch(e => console.error('[JS] Error in addOtherTextureAndSwitch chain:', e));
                    }
                } catch (e) {
                    console.error('[JS] Error in addOtherTextureAndSwitch:', e);
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
        dispatchJsAsync("""
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
        dispatchJsAsync("""
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
        dispatchJsAsync("""
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
        dispatchJsAsync("""
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
        dispatchJsAsync("""
                try {
                    if (typeof window.applyMaskToMainTexture === 'function') {
                        window.applyMaskToMainTexture($0, $1, $2, $3).then(_ => {});
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
        dispatchJsAsync("""
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
     * Retrieves a thumbnail image of the specified model in the Three.js scene.
     * This method calls the JavaScript function getThumbnail to handle the thumbnail generation process.
     * The generated thumbnail is returned as a data URL, which can be used to display the thumbnail image in the UI.
     * @param modelId identification of the model for which the thumbnail is to be generated.
     * @param width the desired width of the generated thumbnail image.
     * @param height the desired height of the generated thumbnail image.
     * @param callback a callback function that will be called with the generated thumbnail data URL once it is ready.
     */
    public void getThumbnailDataUrl(String modelId, int width, int height, java.util.function.Consumer<String> callback) {
        String requestId = String.valueOf(callbackRequestSequence.incrementAndGet());
        thumbnailCallbacks.put(requestId, callback);
        dispatchJsAsync("""
                try {
                    if (typeof window.getThumbnail === 'function') {
                        window.getThumbnail($0, $1, $2, $3).then(dataUrl => {
                            $4.$server.onThumbnailReady($5, dataUrl);
                        });
                    } else {
                        $4.$server.onThumbnailReady($5, null);
                    }
                } catch (e) {
                    console.error('[JS] Error in getThumbnailDataUrl:', e);
                    $4.$server.onThumbnailReady($5, null);
                }
                """, getElement(), modelId, width, height, this, requestId);
    }

    @ClientCallable
    private void onThumbnailReady(String requestId, String dataUrl) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        java.util.function.Consumer<String> callback = thumbnailCallbacks.remove(requestId);
        if (callback != null) {
            callback.accept(dataUrl);
        }
    }

    public void getBackgroundSpecData(java.util.function.Consumer<String> callback) {
        String requestId = String.valueOf(callbackRequestSequence.incrementAndGet());
        backgroundSpecCallbacks.put(requestId, callback);
        dispatchJsAsync("""
                try {
                    if (typeof window.getBackgroundSpec === 'function') {
                        window.getBackgroundSpec($0).then(backgroundSpec => {
                            $1.$server.onBackgroundSpecReady($2, backgroundSpec ? JSON.stringify(backgroundSpec) : null);
                        });
                    } else {
                        $1.$server.onBackgroundSpecReady($2, null);
                    }
                } catch (e) {
                    console.error('[JS] Error in getBackgroundSpecData:', e);
                    $1.$server.onBackgroundSpecReady($2, null);
                }
                """, getElement(), this, requestId);
    }

    public void setBackgroundSpec(String backgroundSpecJson) {
        dispatchJsAsync("""
                try {
                    if (typeof window.setBackgroundSpec === 'function') {
                        window.setBackgroundSpec($0, $1).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in setBackgroundSpec:', e);
                }
                """, getElement(), backgroundSpecJson);
    }

    public void restoreDefaultBackground() {
        dispatchJsAsync("""
                try {
                    if (typeof window.restoreDefaultBackground === 'function') {
                        window.restoreDefaultBackground($0).then(_ => {});
                    }
                } catch (e) {
                    console.error('[JS] Error in restoreDefaultBackground:', e);
                }
                """, getElement());
    }

    @ClientCallable
    private void onBackgroundSpecReady(String requestId, String backgroundSpecJson) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        java.util.function.Consumer<String> callback = backgroundSpecCallbacks.remove(requestId);
        if (callback != null) {
            callback.accept(backgroundSpecJson);
        }
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
     *
     * @param actionDescription a description of the actions being performed, which can be displayed in the UI to inform the user about the ongoing process.
     */
    @ClientCallable
    public void doingActions(String actionDescription) {
        long now = System.currentTimeMillis();
        if (actionDescription != null
                && actionDescription.equals(lastDoingActionDescription)
                && (now - lastDoingActionAtMs) < ACTION_EVENT_MIN_INTERVAL_MS) {
            return;
        }

        lastDoingActionAtMs = now;
        lastDoingActionDescription = actionDescription;
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
     * This method is called from the JavaScript side to report loading progress (0..100).
     * If percent < 0, indicates indeterminate progress.
     *
     * @param percent     the loading progress percentage (0..100), or negative for indeterminate progress.
     * @param description a description of the current loading step, which can be displayed in the
     */
    @ClientCallable
    public void loadingProgress(int percent, String description) {
        long now = System.currentTimeMillis();
        boolean intervalElapsed = (now - lastProgressEventAtMs) >= PROGRESS_EVENT_MIN_INTERVAL_MS;
        boolean changedValue = percent != lastProgressPercent
                || (description != null && !description.equals(lastProgressDescription))
                || (description == null && lastProgressDescription != null);
        boolean terminal = percent >= 100 || percent < 0;

        if (!terminal && !intervalElapsed && !changedValue) {
            return;
        }

        lastProgressEventAtMs = now;
        lastProgressPercent = percent;
        lastProgressDescription = description;
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsLoadingProgress(this, percent, description));
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
        ensureJsDispatchExecutor();

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                UploadFileEvent.class,
                event -> {
                    switch (event.getFileType()) {
                        case MODEL -> {
                            if (event.isFromClient()) {
                                loadModelAndShow(event.getBase64File(), event.getModelId(), event.isMain(), event.getQuestionId());
                            } else {
                                loadModel(event.getBase64File(), event.getModelId(), event.isMain(), event.getQuestionId());
                            }
                        }
                        case OTHER -> {
                            if (event.isFromClient()) {
                                addOtherTextureAndSwitch(event.getBase64File(), event.getEntityId(), event.getModelId());
                            } else {
                                addOtherTexture(event.getBase64File(), event.getEntityId(), event.getModelId());
                            }
                        }
                        case MAIN -> {
                            if (event.isFromClient()) {
                                addMainTextureAndSwitch(event.getBase64File(), event.getModelId());
                            } else {
                                addMainTexture(event.getBase64File(), event.getModelId());
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
        thumbnailCallbacks.clear();
        backgroundSpecCallbacks.clear();
        shutdownJsDispatchExecutor();
    }
}
