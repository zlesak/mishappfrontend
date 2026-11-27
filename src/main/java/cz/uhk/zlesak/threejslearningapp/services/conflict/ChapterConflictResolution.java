package cz.uhk.zlesak.threejslearningapp.services.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares and combines two versions of a chapter: its name, its main model, and its content.
 *
 * <p>The content is compared and chosen as a whole. Headings in it name the models shown beside
 * them, so the models other than the main one travel with the content they belong to; taking one
 * version's text with the other's heading models would leave headings pointing at models the
 * chapter no longer lists.
 */
public class ChapterConflictResolution implements ConflictResolution<ChapterEntity> {

    public static final String NAME = "name";
    public static final String MAIN_MODEL = "mainModel";
    public static final String CONTENT = "content";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Translator translator;

    public ChapterConflictResolution(Translator translator) {
        this.translator = translator;
    }

    @Override
    public List<ConflictField> differences(ChapterEntity mine, ChapterEntity theirs) {
        List<ConflictField> fields = new ArrayList<>();
        if (!Objects.equals(mine.getName(), theirs.getName())) {
            fields.add(new ConflictField(NAME, translator.text("conflict.chapter.field.name"),
                    mine.getName(), theirs.getName()));
        }
        if (!Objects.equals(mainModelId(mine), mainModelId(theirs))) {
            fields.add(new ConflictField(MAIN_MODEL, translator.text("conflict.chapter.field.mainModel"),
                    modelName(mine, mainModelId(mine)), modelName(theirs, mainModelId(theirs))));
        }
        // editor.js stamps every save with the time it was taken, so comparing the stored text
        // would always report a difference; the blocks are what the author wrote.
        if (!Objects.equals(blocks(mine.getContent()), blocks(theirs.getContent()))) {
            fields.add(new ConflictField(CONTENT, translator.text("conflict.chapter.field.content"),
                    readable(mine), readable(theirs)));
        }
        return fields;
    }

    @Override
    public ChapterEntity merge(ChapterEntity mine, ChapterEntity theirs, Map<String, ConflictChoice> choices) {
        ChapterEntity name = ConflictResolution.takesTheirs(choices, NAME) ? theirs : mine;
        ChapterEntity main = ConflictResolution.takesTheirs(choices, MAIN_MODEL) ? theirs : mine;
        ChapterEntity content = ConflictResolution.takesTheirs(choices, CONTENT) ? theirs : mine;

        LinkedHashSet<String> modelIds = new LinkedHashSet<>();
        if (mainModelId(main) != null) {
            modelIds.add(mainModelId(main));
        }
        List<String> contentModels = content.getModelIds() == null ? List.of() : content.getModelIds();
        modelIds.addAll(contentModels.stream().skip(1).filter(Objects::nonNull).toList());

        Map<String, QuickModelEntity> known = new LinkedHashMap<>();
        for (ChapterEntity source : List.of(theirs, mine)) {
            if (source.getModels() != null) {
                source.getModels().stream().filter(Objects::nonNull).filter(model -> model.getId() != null)
                        .forEach(model -> known.putIfAbsent(model.getId(), model));
            }
        }

        return ChapterEntity.builder()
                .id(theirs.getId())
                .name(name.getName())
                .description(mine.getDescription())
                .content(content.getContent())
                .modelIds(new ArrayList<>(modelIds))
                .models(modelIds.stream().map(known::get).filter(Objects::nonNull).toList())
                .creatorId(theirs.getCreatorId())
                .created(theirs.getCreated())
                .updated(theirs.getUpdated())
                .build();
    }

    private static String mainModelId(ChapterEntity chapter) {
        return chapter.getModelIds() == null || chapter.getModelIds().isEmpty() ? null : chapter.getModelIds().getFirst();
    }

    private static String modelName(ChapterEntity chapter, String modelId) {
        if (modelId == null) {
            return "";
        }
        if (chapter.getModels() != null) {
            for (QuickModelEntity model : chapter.getModels()) {
                if (model != null && modelId.equals(model.getId()) && model.getName() != null) {
                    return model.getName();
                }
            }
        }
        return modelId;
    }

    private static JsonNode blocks(String content) {
        try {
            JsonNode root = JSON.readTree(content == null ? "" : content);
            return root == null ? null : root.get("blocks");
        } catch (Exception e) {
            return JSON.getNodeFactory().textNode(content);
        }
    }

    /**
     * Renders editor.js content as plain text, one block per line, so two versions can be read side
     * by side. Headings keep a marker of their level; markup inside the text is dropped.
     */
    private String readable(ChapterEntity chapter) {
        JsonNode blocks = blocks(chapter.getContent());
        if (blocks == null || !blocks.isArray()) {
            return chapter.getContent() == null ? "" : chapter.getContent();
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : blocks) {
            String type = block.path("type").asText("");
            JsonNode data = block.path("data");
            String line = switch (type) {
                case "header" -> "#".repeat(Math.max(1, data.path("level").asInt(1))) + " " + plain(data.path("text").asText(""))
                        + headingModel(chapter, data.path("modelId").asText(""));
                case "paragraph" -> plain(data.path("text").asText(""));
                case "list" -> listItems(data.path("items"), "");
                case "quote" -> "> " + plain(data.path("text").asText(""));
                default -> "[" + type + "]";
            };
            if (!line.isBlank()) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(line);
            }
        }
        return text.toString();
    }

    private String headingModel(ChapterEntity chapter, String modelId) {
        return modelId.isBlank() ? "" : " (" + translator.text("conflict.chapter.headingModel", modelName(chapter, modelId)) + ")";
    }

    private static String listItems(JsonNode items, String indent) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : items) {
            String content = item.isTextual() ? item.asText() : item.path("content").asText("");
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(indent).append("- ").append(plain(content));
            JsonNode nested = item.path("items");
            if (nested.isArray() && !nested.isEmpty()) {
                text.append('\n').append(listItems(nested, indent + "  "));
            }
        }
        return text.toString();
    }

    private static String plain(String html) {
        return html.replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .trim();
    }
}
