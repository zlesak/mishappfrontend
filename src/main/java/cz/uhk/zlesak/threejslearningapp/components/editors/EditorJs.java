package cz.uhk.zlesak.threejslearningapp.components.editors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.parsers.ModelListingDataParser;
import cz.uhk.zlesak.threejslearningapp.domain.parsers.TextureListingDataParser;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureListingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ScrollToElement;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
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
     *
     * @param createMode true for edit mode, false for read-only mode
     */
    public EditorJs(boolean createMode) {
        getElement().setProperty("readOnly", !createMode);
        addModelTextureColorAreaClickListener();
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

    public void filterContentByLevel1Header(String headerIdOrText) {
        getElement().callJsFunction("filterContentByLevel1Header", headerIdOrText, false)
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new RuntimeException("Chyba při zobrazování dat dle id kapitoly " + error.getMessage());
                })
                .thenApply(ignore -> null);
    }

    public void scrollToHeading(String headerIdOrText) {
        getElement().callJsFunction("scrollToDataId", headerIdOrText)
                .toCompletableFuture()
                .exceptionally(error -> {
                    throw new RuntimeException("Chyba při scrollování k prkvu " + error.getMessage());
                })
                .thenApply(ignore -> null);
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
     */
    public void addModelTextureColorAreaClickListener() {
        getElement().addEventListener("texturecolorareaclick", event -> {
                    String modelId = event.getEventData().get("event.detail.modelId").asString();
                    String textureId = event.getEventData().get("event.detail.textureId").asString();
                    String hexColor = event.getEventData().get("event.detail.hexColor").asString();
                    ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, textureId, ThreeJsActions.APPLY_MASK_TO_TEXTURE, true, null, hexColor));
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
     * Loads provided Moodle HTML string into the editor.
     *
     * @param html HTML content from Moodle
     */
    public void loadMoodleHtml(String html) {
        getElement().callJsFunction("loadMoodleHtml", html);
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ShowSubchapterContentEvent.class,
                e -> filterContentByLevel1Header(e.getSubchapterId() != null ? e.getSubchapterId() : "")
        ));
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ScrollToElement.class,
                e -> scrollToHeading(e.getElement())
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}