package cz.uhk.zlesak.threejslearningapp.services.conflict;

import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterConflictResolutionTest {

    private static final Instant MINE_BASE = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant THEIRS_SAVED = Instant.parse("2026-11-01T10:05:00Z");

    private final ChapterConflictResolution resolution = new ChapterConflictResolution((key, params) -> key);

    @Test
    void reportsNothingWhenOnlyTheEditorTimestampDiffers() {
        ChapterEntity mine = chapter("Mozek", content(1, "Úvod", "model-a"), MINE_BASE, "main-1", "model-a");
        ChapterEntity theirs = chapter("Mozek", content(2, "Úvod", "model-a"), THEIRS_SAVED, "main-1", "model-a");

        assertThat(resolution.differences(mine, theirs)).isEmpty();
    }

    @Test
    void listsEachDifferingFieldWithBothValuesReadable() {
        ChapterEntity mine = chapter("Mozek", content(1, "Úvod <b>moje</b>", null), MINE_BASE, "main-1");
        ChapterEntity theirs = chapter("Mozek a mícha", content(2, "Úvod jejich", null), THEIRS_SAVED, "main-2");

        List<ConflictField> fields = resolution.differences(mine, theirs);

        assertThat(fields).extracting(ConflictField::key)
                .containsExactly(ChapterConflictResolution.NAME, ChapterConflictResolution.MAIN_MODEL,
                        ChapterConflictResolution.CONTENT);
        assertThat(fields.get(1).mine()).isEqualTo("Model main-1");
        assertThat(fields.get(1).theirs()).isEqualTo("Model main-2");
        // Markup dropped, heading marked: the author reads text, not editor.js JSON.
        assertThat(fields.get(2).mine()).isEqualTo("# Úvod moje\nOdstavec");
        assertThat(fields.get(2).theirs()).isEqualTo("# Úvod jejich\nOdstavec");
    }

    @Test
    void combinesTheChosenFieldsOnTopOfTheStoredVersion() {
        ChapterEntity mine = chapter("Moje jméno", content(1, "Můj text", "model-a"), MINE_BASE, "main-1", "model-a");
        ChapterEntity theirs = chapter("Jejich jméno", content(2, "Jejich text", "model-b"), THEIRS_SAVED, "main-2", "model-b");

        ChapterEntity combined = resolution.merge(mine, theirs, Map.of(
                ChapterConflictResolution.NAME, ConflictChoice.THEIRS,
                ChapterConflictResolution.MAIN_MODEL, ConflictChoice.MINE,
                ChapterConflictResolution.CONTENT, ConflictChoice.MINE));

        assertThat(combined.getName()).isEqualTo("Jejich jméno");
        assertThat(combined.getContent()).contains("Můj text");
        // Heading models travel with the content they belong to; the main model is chosen on its own.
        assertThat(combined.getModelIds()).containsExactly("main-1", "model-a");
        assertThat(combined.getModels()).extracting(QuickModelEntity::getId).containsExactly("main-1", "model-a");
        // Saving the combination is checked against the version the dialog showed.
        assertThat(combined.getUpdated()).isEqualTo(THEIRS_SAVED);
        assertThat(combined.getId()).isEqualTo("ch-1");
    }

    @Test
    void takingTheirContentTakesTheirHeadingModels() {
        ChapterEntity mine = chapter("Mozek", content(1, "Můj text", "model-a"), MINE_BASE, "main-1", "model-a");
        ChapterEntity theirs = chapter("Mozek", content(2, "Jejich text", "model-b"), THEIRS_SAVED, "main-1", "model-b");

        ChapterEntity combined = resolution.merge(mine, theirs, Map.of(
                ChapterConflictResolution.CONTENT, ConflictChoice.THEIRS));

        assertThat(combined.getContent()).contains("Jejich text");
        assertThat(combined.getModelIds()).containsExactly("main-1", "model-b");
    }

    @Test
    void withoutAnyChoiceTheUsersVersionIsKept() {
        ChapterEntity mine = chapter("Moje", content(1, "Můj text", null), MINE_BASE, "main-1");
        ChapterEntity theirs = chapter("Jejich", content(2, "Jejich text", null), THEIRS_SAVED, "main-2");

        ChapterEntity combined = resolution.merge(mine, theirs, Map.of());

        assertThat(combined.getName()).isEqualTo("Moje");
        assertThat(combined.getContent()).contains("Můj text");
        assertThat(combined.getModelIds()).containsExactly("main-1");
        assertThat(combined.getUpdated()).isEqualTo(THEIRS_SAVED);
    }

    private static ChapterEntity chapter(String name, String content, Instant updated, String... modelIds) {
        return ChapterEntity.builder()
                .id("ch-1")
                .name(name)
                .content(content)
                .updated(updated)
                .modelIds(List.of(modelIds))
                .models(java.util.Arrays.stream(modelIds)
                        .map(id -> (QuickModelEntity) QuickModelEntity.builder().id(id).name("Model " + id).build())
                        .toList())
                .build();
    }

    private static String content(long time, String heading, String headingModelId) {
        String model = headingModelId == null ? "" : ",\"modelId\":\"" + headingModelId + "\"";
        return "{\"time\":" + time + ",\"blocks\":["
                + "{\"id\":\"h1\",\"type\":\"header\",\"data\":{\"level\":1,\"text\":\"" + heading + "\"" + model + "}},"
                + "{\"id\":\"p1\",\"type\":\"paragraph\",\"data\":{\"text\":\"Odstavec\"}}"
                + "],\"version\":\"2.28\"}";
    }
}
