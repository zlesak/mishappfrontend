package cz.uhk.zlesak.threejslearningapp.components.editors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.events.editor.MarkdownModeToggleEvent;
import cz.uhk.zlesak.threejslearningapp.events.editor.MarkdownValueChangedEvent;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.parsers.ModelListingDataParser;
import cz.uhk.zlesak.threejslearningapp.domain.parsers.TextureListingDataParser;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import elemental.json.JsonValue;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Custom component for Editor.js integration in Vaadin.
 * This component allows interaction with Editor.js, including getting data,
 * toggling read-only mode, and setting chapter content data.
 * It uses JavaScript interop to call methods defined in the Editor.js JavaScript module.
 * This is the heart of the Editor.js integration, allowing for rich text editing capabilities within a Vaadin application.
 */
@Tag("editor-js")
@JsModule("./js/editorjs/editor-js.ts")
@NpmPackage(value = "@editorjs/editorjs", version = "2.30.8")
@Scope("prototype")
public class EditorJs extends Component implements HasSize, HasStyle, I18nAware {
    private final List<Registration> registrations = new ArrayList<>();

    /**
     * Default constructor for EditorJsComponent.
     * Does not take any parameters as they are not needed at the time of instantiation.
     */
    public EditorJs() {
    }

    /**
     * Retrieves data from the Editor.js instance as a JSON string.
     *
     * @return JSON as string with data retrieved from Editor.js.
     */
    public CompletableFuture<String> getData() {
        return getElement().callJsFunction("getData").toCompletableFuture()
                .thenApply(json -> {
                    String result = json.asString();
                    return (result == null || result.isEmpty()) ? "{}" : result;
                });
    }

    /**
     * Toggles the read-only mode of the Editor.js instance.
     * When in read-only mode, users cannot edit the content.
     *
     * @param readOnly true to enable read-only mode, false to disable it.
     */
    public void toggleReadOnlyMode(boolean readOnly) {
        getElement().callJsFunction("toggleReadOnlyMode", readOnly);
    }

    /**
     * Sets the chapter content data in the Editor.js instance.
     * This method expects a JSON string that represents the chapter content.
     *
     * @param jsonData JSON string containing the chapter content data.
     */
    public void setChapterContentData(String jsonData) {
        getElement()
                .callJsFunction("setChapterContentData", jsonData)
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new RuntimeException("Chyba při nastavování chapterContentData: " + error.getMessage());
                })
                .thenApply(ignore -> null);
    }

    /**
     * Shows the whole chapter data in the Editor.js instance.
     * This method is used to display all the content of the chapter, including subchapters.
     * Used when no specific subchapter is selected.
     */
    public void showWholeChapterData() {
        getElement().callJsFunction("showWholeChapterData")
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new RuntimeException("Chyba při zobrazování dat celé kapitoly " + error.getMessage());
                })
                .thenApply(ignore -> null);
    }

    /**
     * Sets the selected subchapter data in the Editor.js instance.
     * This method expects a JSON string that represents the subchapter data.
     *
     * @param jsonData JSON string containing the subchapter data.
     */
    public void setSelectedSubchapterData(String jsonData) {
        getElement().callJsFunction("setSelectedSubchapterData", jsonData);
    }

    /**
     * Initializes texture selection options in the Editor.js instance.
     * This method takes a map of String and QuickModelEntity, processes them,
     * and passes the relevant data to the JavaScript side for initializing custom TextureColorLinkTool inline tool.
     *
     * @param quickModelEntityList list of QuickModelEntities and teh subchapters they belong to.
     */
    public void initializeTextureSelects(Map<String, QuickModelEntity> quickModelEntityList) {
        List<ModelForSelect> modelForSelects = ModelListingDataParser.modelForSelectDataParser(quickModelEntityList);
        List<TextureListingForSelect> otherTexturesMap = TextureListingDataParser.textureListingForSelectDataParser(quickModelEntityList, false, text("textureListingSelect.noOtherTextures"));
        List<TextureAreaForSelect> textureAreaForSelect = TextureMapHelper.createTextureAreaForSelectRecordList(quickModelEntityList);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String modelsJson = mapper.writeValueAsString(modelForSelects);
            String texturesJson = mapper.writeValueAsString(otherTexturesMap);
            String areasJson = mapper.writeValueAsString(textureAreaForSelect);
            getElement().callJsFunction("initializeModelTextureAreaSelects", modelsJson, texturesJson, areasJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Chyba při serializaci texture dat: " + e.getMessage());
        }
    }

    /**
     * Adds a listener for texture color area click events.
     * When a texture color area is clicked in the Editor.js instance, this listener will be triggered.
     * The listener receives the texture ID, hex color, and associated text as parameters.
     *
     * @param listener the listener to be added
     */
    public void addModelTextureColorAreaClickListener(ModelTextureAreaClickListener listener) {
        getElement().addEventListener("texturecolorareaclick", event -> {
                    String modelId = event.getEventData().getString("event.detail.modelId");
                    String textureId = event.getEventData().getString("event.detail.textureId");
                    String hexColor = event.getEventData().getString("event.detail.hexColor");
                    String text = event.getEventData().getString("event.detail.text");
                    listener.onTextureColorAreaClick(modelId, textureId, hexColor, text);
                }).addEventData("event.detail.modelId")
                .addEventData("event.detail.textureId")
                .addEventData("event.detail.hexColor")
                .addEventData("event.detail.text");
    }

    /**
     * Searches for the given text in the Editor.js instance.
     * This method triggers a search operation within the Editor.js content.
     *
     * @param searchText the text to search for within the Editor.js content.
     * @see SearchTextField
     */
    public void search(String searchText) {
        getElement().callJsFunction("search", searchText)
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new RuntimeException("Chyba při vyhledávání " + error.getMessage());
                })
                .thenApply(ignore -> null);
    }

    /**
     * Listener interface for handling texture color area click events.
     * Implement this interface to define custom behavior when a texture color area is clicked.
     */
    public interface ModelTextureAreaClickListener {
        void onTextureColorAreaClick(String modelId, String textureId, String hexColor, String text);
    }

    /**
     * Loads provided Markdown string into the editor (converts it to EditorJS blocks).
     *
     * @param markdown markdown content
     */
    public void loadMarkdown(String markdown) {
        getElement().callJsFunction("loadMarkdown", markdown);
    }

    /**
     * Retrieves current content as Markdown (converts blocks if in block mode).
     *
     * @return future with markdown string
     */
    public CompletableFuture<String> getMarkdown() {
        return getElement()
                .callJsFunction("getMarkdown")
                .toCompletableFuture()
                .thenApply(JsonValue::asString);
    }

    /**
     * Retrieves a map of subchapter IDs to their names from the Editor.js instance, that has not yet been saved into the database.
     *
     * @return CompletableFuture that resolves to a Map where keys are subchapter IDs and values are subchapter names.
     */
    public CompletableFuture<Map<String, String>> getSubchaptersNames() {
        return getElement().callJsFunction("getSubchaptersNames").toCompletableFuture().thenApply(jsonValue -> {
            String jsonString = jsonValue.asString();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(jsonString, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Chyba při parsování subchapter names JSON: " + e.getMessage());
            }
        });
    }

    /**
     * Adds a listener for Markdown mode toggle events.
     * This listener is triggered when the Markdown mode is toggled.
     *
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                MarkdownModeToggleEvent.class,
                event -> {
                    if (event.isMarkdownMode()) {
                        getMarkdown().whenComplete((markdown, throwable) -> {
                            if (throwable != null) {
                                throw new RuntimeException("Chyba při získávání markdown hodnoty: " + throwable.getMessage());
                            } else {
                                ComponentUtil.fireEvent(UI.getCurrent(), new MarkdownValueChangedEvent(UI.getCurrent(), markdown, true));
                                setVisible(false);
                            }
                        });
                    } else {
                        setVisible(true);
                    }
                }
        ));

        registrations.add(
                ComponentUtil.addListener(
                        attachEvent.getUI(),
                        MarkdownValueChangedEvent.class,
                        event -> {
                            if (!event.isMarkdownMode()) {
                                loadMarkdown(event.getMarkdownValue());
                            }
                        }
                )
        );
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}