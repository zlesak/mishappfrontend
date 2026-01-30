package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.uhk.zlesak.threejslearningapp.api.clients.ChapterApiClient;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterFilter;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

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
            JsonNode root = objectMapper.readTree(entity.getContent());
            JsonNode blocks = root.get("blocks");

            for (JsonNode block : blocks) {
                if ("header".equals(block.get("type").asText())) {
                    JsonNode data = block.get("data");
                    if (data.get("level").asInt() == 1) {
                        String id = block.has("id") ? block.get("id").asText() : "fallback-" + java.util.UUID.randomUUID().toString().substring(0, 7);
                        String text = data.get("text").asText();
                        String modelId = data.has("modelId") ? data.get("modelId").asText() : "";
                        subChapters.add(new SubChapterForSelect(id, text, modelId));
                    }
                }
            }
            return subChapters;
        } catch (Exception e) {
            log.error("Error getting subchapter names: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes the chapter content to extract headers and their associated sub-headers.
     * It parses the chapter content to identify level 1 headers and their corresponding sub-headers (level 2 and above).
     * The result is a map where each key is a Triple containing the level 1 header's ID, text, and modelId,
     * and the value is a list of Tuples representing the sub-headers with their IDs and texts.
     * @param chapterId the ID of the chapter to process
     * @return a map of level 1 headers to their associated sub-headers
     * @throws Exception if there is an error reading or parsing the chapter content
     */
    public Map<Triple<String, String,String>, List<Tuple<String, String>>> processHeaders(String chapterId) throws Exception {
        read(chapterId);
        JsonNode root = objectMapper.readTree(entity.getContent());
        JsonNode blocks = root.get("blocks");

        LinkedHashMap<Triple<String, String, String>, List<Tuple<String, String>>> result = new LinkedHashMap<>();

        Triple<String, String, String> currentLevel1Header = null;
        LinkedList<Tuple<String, String>> currentSubHeaders = null;

        for (JsonNode block : blocks) {
            String type = block.get("type").asText();

            if ("header".equals(type)) {
                JsonNode data = block.get("data");
                int level = data.get("level").asInt();
                String text = data.get("text").asText();
                String id = block.get("id").asText();
                String modelId = data.get("modelId") != null ? data.get("modelId").asText() : null;

                if (level == 1) {
                    if (currentLevel1Header != null) {
                        result.put(currentLevel1Header, currentSubHeaders);
                    }

                    currentLevel1Header = Triple.of(id, text, modelId);
                    currentSubHeaders = new LinkedList<>();

                } else if (level >= 2) {
                    if (currentLevel1Header != null) {
                        currentSubHeaders.add(new Tuple<>(id, text));
                    }
                }
            }
        }

        if (currentLevel1Header != null) {
            result.put(currentLevel1Header, currentSubHeaders);
        }

        return result;
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
            throw new RuntimeException("Kapitola musí mít alespoň jeden hlavní model.");
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
                if (blockNode.has("id") && blockNode.has("type") && "header".equals(blockNode.get("type").asText())) {
                    String blockId = blockNode.get("id").asText();
                    QuickModelEntity model = chapterCreateEntity.getModelHeaderMap().containsKey(blockId) ? chapterCreateEntity.getModelHeaderMap().get(blockId) :  chapterCreateEntity.getModelHeaderMap().get("main");
                    ObjectNode dataNode = (ObjectNode) blockNode.get("data");
                    dataNode.put("modelId", model.getModel().getId());
                }
            });
            content = objectMapper.writeValueAsString(bodyJson);
        } catch (ApplicationContextException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chyba při úpravě bloků editorjs: {}", e.getMessage(), e);
            new ErrorNotification("Chyba při úpravě bloků editorjs: " + e.getMessage());
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
                .id(chapterCreateEntity.getId())
                .name(chapterCreateEntity.getName())
                .created(Instant.now())
                .description("")
                .content(content)
                .models(uploadedModels)
                .build();
    }
}
