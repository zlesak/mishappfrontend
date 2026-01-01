package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.uhk.zlesak.threejslearningapp.api.clients.ChapterApiClient;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing chapters in the application.
 * This class provides methods to create chapters, retrieve chapter details, and manage sub-chapters.
 * Provides the connector to the ChapterApiClient for performing operations related to chapters on BE side.
 */
@Slf4j
@Service
@Scope("prototype")
public class ChapterService extends AbstractService<ChapterEntity, ChapterEntity, ChapterFilter> { //TODO quick chapter entity on BE side
    private final ObjectMapper objectMapper;
    private final List<QuickModelEntity> uploadedModels = new ArrayList<>();

    /**
     * Constructor for ChapterService that initializes the ChapterApiClient.
     *
     * @param chapterApiClient The API client used to interact with chapter-related operations.
     */
    @Autowired
    public ChapterService(ChapterApiClient chapterApiClient, ObjectMapper objectMapper) {
        super(chapterApiClient);
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves the name of a chapter by its ID.
     * If the chapterEntity is not set or does not match the provided chapterId, it fetches the chapter details using the getChapter method.
     * This method saves network calls by caching the chapterEntity after the first retrieval.
     *
     * @param chapterId the ID of the chapter whose name is to be retrieved
     * @return the name of the chapter
     */
    public String getChapterName(String chapterId) {
        read(chapterId);
        return entity.getName();
    }

    /**
     * Retrieves the content of a chapter.
     * If the chapterEntity is not set or does not match the provided chapterId, it fetches the chapter details using the getChapter method.
     *
     * @return the content of the chapter as a JSON string
     */
    public String getChapterContent(String chapterId) {
        read(chapterId);
        return entity.getContent();
    }

    /**
     * Retrieves the names of sub-chapters from the chapter content.
     * It parses the chapter content to extract sub-chapter headers (level 1 headers).
     * Each sub-chapter is represented by its ID and text.
     * If an error occurs during the parsing, it logs the error and throws an Exception.
     *
     * @return a list of SubChapterForComboBoxRecord objects containing sub-chapter IDs and names
     * @see SubChapterForSelect
     */
    public List<SubChapterForSelect> getSubChaptersNames(String chapterId) {
        read(chapterId);

        List<SubChapterForSelect> subChapters = new ArrayList<>();
        try {
            JsonArray blocks = Json.parse(entity.getContent()).getArray("blocks");

            for (int i = 0; i < blocks.length(); i++) {
                JsonObject block = blocks.getObject(i);
                if ("header".equals(block.getString("type")) && block.getObject("data").getNumber("level") == 1) {
                    String id = block.hasKey("id") ? block.getString("id") : "fallback-" + java.util.UUID.randomUUID().toString().substring(0, 7);
                    String text = block.getObject("data").getString("text");
                    String modelId = block.getObject("data").hasKey("modelId") ? block.getObject("data").getString("modelId") : "";
                    subChapters.add(new SubChapterForSelect(id, text, modelId));
                }
            }
            return subChapters;
        } catch (Exception e) {
            log.error("Error getting subchapter names: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the content of sub-chapters from the chapter content.
     * It parses the chapter content to extract sub-chapter headers (level 1 headers)
     *
     * @return a JsonArray containing sub-chapter content, where each sub-chapter is represented by its header and content blocks
     */
    public JsonArray getSubChaptersContent(String chapterId) {
        read(chapterId);

        try {
            JsonArray blocks = Json.parse(entity.getContent()).getArray("blocks");
            JsonArray result = Json.createArray();
            int resultIndex = 0;
            int objectIndex = 0;

            JsonObject oldHeaderBlock;
            JsonObject newHeaderBlock = null;

            JsonArray blocksArray = Json.createArray();

            while (objectIndex < blocks.length()) {
                JsonObject block = blocks.getObject(objectIndex++);
                if ("header".equals(block.getString("type")) && block.getObject("data").getNumber("level") == 1) {
                    oldHeaderBlock = newHeaderBlock;
                    newHeaderBlock = block;
                    JsonObject obj = Json.createObject();
                    if (oldHeaderBlock != null) {
                        obj.put("h1", oldHeaderBlock);
                    } else {
                        JsonObject noHeader = Json.createObject();
                        noHeader.put("id", "fallback-" + java.util.UUID.randomUUID().toString().substring(0, 7));
                        noHeader.put("type", "header");
                        JsonObject data = Json.createObject();
                        data.put("text", "Obsah bez hlavního nadpisu");
                        data.put("level", 1);
                        noHeader.put("data", data);
                        obj.put("h1", noHeader);
                    }
                    obj.put("content", blocksArray);
                    result.set(resultIndex++, obj);
                    blocksArray = Json.createArray();
                } else if ("header".equals(block.getString("type"))) {
                    blocksArray.set(blocksArray.length(), block);
                }
                JsonObject previousBlock = blocks.getObject(objectIndex - 1);

                if (objectIndex == blocks.length() && "header".equals(previousBlock.getString("type")) && previousBlock.getObject("data").getNumber("level") == 1) {
                    oldHeaderBlock = newHeaderBlock;
                    newHeaderBlock = block;
                    JsonObject obj = Json.createObject();
                    if (oldHeaderBlock != null) {
                        obj.put("h1", oldHeaderBlock);
                    } else {
                        JsonObject noHeader = Json.createObject();
                        noHeader.put("id", "fallback-" + java.util.UUID.randomUUID().toString().substring(0, 7));
                        noHeader.put("type", "header");
                        JsonObject data = Json.createObject();
                        data.put("text", "Obsah bez hlavního nadpisu");
                        data.put("level", 1);
                        noHeader.put("data", data);
                        obj.put("h1", noHeader);
                    }
                    obj.put("content", blocksArray);
                    result.set(resultIndex++, obj);
                    blocksArray = Json.createArray();
                }
            }
            if (blocksArray.length() > 0) {
                oldHeaderBlock = newHeaderBlock;
                JsonObject obj = Json.createObject();
                if (oldHeaderBlock != null) {
                    obj.put("h1", oldHeaderBlock);
                } else {
                    JsonObject noHeader = Json.createObject();
                    noHeader.put("id", "fallback-" + java.util.UUID.randomUUID().toString().substring(0, 7));
                    noHeader.put("type", "header");
                    JsonObject data = Json.createObject();
                    data.put("text", "Obsah bez hlavního nadpisu");
                    data.put("level", 1);
                    noHeader.put("data", data);
                    obj.put("h1", noHeader);
                }
                obj.put("content", blocksArray);
                result.set(resultIndex, obj);
            }
            return result;
        } catch (Exception e) {
            log.error("Error getting subchapters content: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the content of a selected sub-chapter by its ID.
     * It parses the chapter content to find the blocks associated with the specified sub-chapter header (level 1 header).
     * If the header with the specified ID does not exist, it returns the entire chapter content.
     * If the header exists, it collects all blocks until the next header of the same level (level 1) is found.
     *
     * @param id the ID of the sub-chapter header to retrieve content for
     * @return the content of the selected sub-chapter as a JSON string
     */
    public String getSelectedSubChapterContent(String id) {
        JsonArray blocks = Json.parse(entity.getContent()).getArray("blocks");
        boolean headerExists = false;
        for (int i = 0; i < blocks.length(); i++) {
            JsonObject block = blocks.getObject(i);
            if ("header".equals(block.getString("type")) && block.getObject("data").getNumber("level") == 1 && block.hasKey("id") && block.getString("id").equals(id)) {
                headerExists = true;
                break;
            }
        }
        if (!headerExists) {
            return blocks.toJson();
        }
        boolean found = false;
        JsonArray content = Json.createArray();
        int contentIndex = 0;
        for (int i = 0; i < blocks.length(); i++) {
            JsonObject block = blocks.getObject(i);
            if (!found) {
                if ("header".equals(block.getString("type")) && block.getObject("data").getNumber("level") == 1 && block.hasKey("id") && block.getString("id").equals(id)) {
                    found = true;
                    content.set(contentIndex++, block);
                }
            } else if ("header".equals(block.getString("type")) && block.getObject("data").getNumber("level") == 1) {
                break;
            } else {
                content.set(contentIndex++, block);
            }
        }
        return content.toJson();
    }

    /**
     * Retrieves a map of sub-chapter IDs to their corresponding QuickModelEntity objects for the specified chapter.
     * If the chapterEntity is not set or does not match the provided chapterId, it fetches the chapter details using the getChapter method.
     * It then iterates through the sub-chapters and maps their IDs to the corresponding QuickModelEntity objects.
     *
     * @param chapterId the ID of the chapter whose sub-chapter models are to be retrieved
     * @return a map where the keys are sub-chapter IDs and the values are QuickModelEntity objects
     * @throws Exception if there is an error retrieving the chapter or sub-chapter models
     */
    public Map<String, QuickModelEntity> getChaptersModels(String chapterId) throws Exception {
        read(chapterId);
        try {
            List<SubChapterForSelect> subChaptersNames = getSubChaptersNames(chapterId);
            subChaptersNames.addFirst(new SubChapterForSelect("main", null, null));
            Map<String, QuickModelEntity> modelsMap = new HashMap<>();
            List<QuickModelEntity> modelsList = new ArrayList<>(entity.getModels());
            modelsMap.put("main", modelsList.getFirst());

            for (SubChapterForSelect subChapter : subChaptersNames) {
                for (QuickModelEntity model : modelsList) {
                    if (Objects.equals(model.getModel().getId(), subChapter.modelId())) {
                        modelsMap.put(subChapter.id(), model);
                    }
                }
            }
            return modelsMap;
        } catch (Exception e) {
            log.error("Chyba při čtení dat modelu kapitoly pro mapped verzi: {}", e.getMessage(), e);
            throw new Exception("Chyba při čtení dat modelu kapitoly pro mapped verzi: " + e.getMessage());
        }
    }

    /**
     * Validates the ChapterCreateEntity before creating a new chapter.
     * It checks that the chapter name, content, and models are not null or empty.
     *
     * @param chapterCreateEntity the ChapterCreateEntity to validate
     * @throws RuntimeException if any validation check fails
     */
    @Override
    protected ChapterEntity validateCreateEntity(ChapterEntity chapterCreateEntity) throws RuntimeException {
        if (chapterCreateEntity.getName() == null || chapterCreateEntity.getName().isEmpty()) {
            throw new RuntimeException("Název kapitoly nesmí být prázdný.");
        }
        if (chapterCreateEntity.getContent() == null || chapterCreateEntity.getContent().isEmpty()) {
            throw new RuntimeException("Obsah kapitoly nesmí být prázdný.");
        }
        if (chapterCreateEntity.getModels() == null || chapterCreateEntity.getModels().isEmpty()) {
            throw new RuntimeException("Kapitola musí mít alespoň jeden model.");
        }
        return chapterCreateEntity;
    }

    /**
     * Creates the final ChapterEntity from the ChapterCreateEntity.
     * It processes the content to associate models with their respective blocks and prepares the list of uploaded
     *
     * @param chapterCreateEntity the ChapterCreateEntity to convert
     * @return the created ChapterEntity
     * @throws RuntimeException if any error occurs during the creation process
     */
    @Override
    protected ChapterEntity createFinalEntity(ChapterEntity chapterCreateEntity) throws RuntimeException {

        String content = "";
        try {
            ObjectNode bodyJson = (ObjectNode) objectMapper.readTree(chapterCreateEntity.getContent());
            ArrayNode blocks = (ArrayNode) bodyJson.get("blocks");

            if (blocks.isEmpty()) {
                throw new ApplicationContextException("Obsah kapitoly nesmí být prázdný.");
            }

            blocks.forEach(blockNode -> {
                if (blockNode.has("id") && chapterCreateEntity.getModelHeaderMap().containsKey(blockNode.get("id").asText())) {
                    String blockId = blockNode.get("id").asText();
                    QuickModelEntity model = chapterCreateEntity.getModelHeaderMap().get(blockId);
                    ObjectNode dataNode = (ObjectNode) blockNode.get("data");
                    dataNode.put("modelId", model.getModel().getId());
                }
            });
            content = objectMapper.writeValueAsString(bodyJson);
        } catch (ApplicationContextException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chyba při úpravě bloků editorjs: {}", e.getMessage(), e);
            new ErrorNotification("Chyba při úpravě bloků editorjs: " + e.getMessage(), 5000);
        }

        Set<QuickModelEntity> addedModelIds = new HashSet<>();

        chapterCreateEntity.getModelHeaderMap().forEach((key, model) -> {
            if (!key.equals("main")) {
                addedModelIds.add(model);
            }
        });

        ArrayList<QuickModelEntity> modelsList = new ArrayList<>(addedModelIds);

        if (chapterCreateEntity.getModelHeaderMap().containsKey("main")) {
            QuickModelEntity mainModel = chapterCreateEntity.getModelHeaderMap().get("main");
            modelsList.addFirst(mainModel);
        }

        uploadedModels.addAll(modelsList);

        return ChapterEntity.builder()
                .name(chapterCreateEntity.getName())
                .created(Instant.now())
                .description("")
                .content(content)
                .models(uploadedModels)
                .build();
    }
}
