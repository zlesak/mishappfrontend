package cz.uhk.zlesak.threejslearningapp.services.conflict;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelConflictResolutionTest {

    private static final Instant MINE_BASE = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant THEIRS_SAVED = Instant.parse("2026-11-01T10:05:00Z");

    private final ModelConflictResolution resolution = new ModelConflictResolution(
            (key, params) -> params.length == 0 ? key : key + Arrays.toString(params),
            description -> description == null ? null : "thumb:" + description,
            fileId -> "m".equals(fileId) ? "femur.obj" : null);

    @Test
    void alwaysOffersTheFilesWithThumbnailsAndTheNameOnlyWhenItDiffers() {
        List<ConflictField> sameName = resolution.differences(mine("Lebka"), theirs("Lebka"));
        assertThat(sameName).extracting(ConflictField::key).containsExactly(ModelConflictResolution.FILES);

        List<ConflictField> fields = resolution.differences(mine("Lebka moje"), theirs("Lebka jejich"));
        assertThat(fields).extracting(ConflictField::key)
                .containsExactly(ModelConflictResolution.NAME, ModelConflictResolution.FILES);
        ConflictField files = fields.get(1);
        // The model file is part of the unit, so it is listed first, by the name it was uploaded under.
        assertThat(files.mine()).isEqualTo("conflict.model.files.model[lebka.glb]\n"
                + "conflict.model.files.mainTexture[moje.jpg]");
        assertThat(files.theirs()).isEqualTo("conflict.model.files.model[femur.obj]\n"
                + "conflict.model.files.mainTexture[jejich.jpg]\n"
                + "conflict.model.files.otherTexture[oblasti.jpg]\n  conflict.model.files.csv[oblasti.csv]");
        assertThat(files.mineImage()).isEqualTo("thumb:moje-popis");
        assertThat(files.theirsImage()).isEqualTo("thumb:jejich-popis");
    }

    @Test
    void keepingTheStoredFilesUploadsNothingAndIsBasedOnTheStoredVersion() {
        QuickModelEntity combined = resolution.merge(mine("Lebka moje"), theirs("Lebka jejich"), Map.of(
                ModelConflictResolution.NAME, ConflictChoice.MINE,
                ModelConflictResolution.FILES, ConflictChoice.THEIRS));

        assertThat(combined).isInstanceOfSatisfying(ModelEntity.class, model -> {
            assertThat(model.isKeepStoredFiles()).isTrue();
            assertThat(model.getInputStreamMultipartFile()).isNull();
            assertThat(model.getName()).isEqualTo("Lebka moje");
            assertThat(model.getUpdated()).isEqualTo(THEIRS_SAVED);
            assertThat(model.getId()).isEqualTo("model-1");
        });
    }

    @Test
    void keepingMyFilesUploadsThemOnTopOfTheStoredVersion() {
        ModelEntity mine = mine("Lebka moje");

        QuickModelEntity combined = resolution.merge(mine, theirs("Lebka jejich"), Map.of(
                ModelConflictResolution.NAME, ConflictChoice.THEIRS,
                ModelConflictResolution.FILES, ConflictChoice.MINE));

        assertThat(combined).isInstanceOfSatisfying(ModelEntity.class, model -> {
            assertThat(model.isKeepStoredFiles()).isFalse();
            assertThat(model.getInputStreamMultipartFile()).isSameAs(mine.getInputStreamMultipartFile());
            assertThat(model.getFullMainTexture()).isSameAs(mine.getFullMainTexture());
            assertThat(model.getDescription()).isEqualTo("moje-popis");
            assertThat(model.getName()).isEqualTo("Lebka jejich");
            assertThat(model.getUpdated()).isEqualTo(THEIRS_SAVED);
        });
    }

    @Test
    void aVersionThatAlreadyKeptTheStoredFilesOffersNoFilesOfItsOwn() {
        // What the first round of the dialog saves when the user keeps the stored files; if that
        // save conflicts again, it is "mine" in the second round.
        ModelEntity keptStored = ModelEntity.builder().id("model-1").name("Lebka moje")
                .updated(THEIRS_SAVED).keepStoredFiles(true).build();
        QuickModelEntity third = theirs("Lebka třetí");

        assertThat(resolution.differences(keptStored, third)).extracting(ConflictField::key)
                .containsExactly(ModelConflictResolution.NAME);

        QuickModelEntity combined = resolution.merge(keptStored, third, Map.of(
                ModelConflictResolution.NAME, ConflictChoice.MINE,
                ModelConflictResolution.FILES, ConflictChoice.MINE));
        assertThat(combined).isInstanceOfSatisfying(ModelEntity.class, model -> {
            assertThat(model.isKeepStoredFiles()).isTrue();
            assertThat(model.getName()).isEqualTo("Lebka moje");
            assertThat(model.getUpdated()).isEqualTo(THEIRS_SAVED);
        });
    }

    private static ModelEntity mine(String name) {
        return ModelEntity.builder()
                .id("model-1")
                .name(name)
                .description("moje-popis")
                .updated(MINE_BASE)
                .inputStreamMultipartFile(file("lebka.glb"))
                .fullMainTexture(TextureEntity.builder().textureFile(file("moje.jpg")).build())
                .build();
    }

    private static QuickModelEntity theirs(String name) {
        ModelFileEntity csv = ModelFileEntity.builder().id("c").name("oblasti.csv").senseType(FileSenseType.CSV_FILE).build();
        return QuickModelEntity.builder()
                .id("model-1")
                .name(name)
                .description("jejich-popis")
                .updated(THEIRS_SAVED)
                .model(ModelFileEntity.builder().id("m").name(name).senseType(FileSenseType.MODEL).related(List.of(
                        ModelFileEntity.builder().id("t1").name("jejich.jpg").senseType(FileSenseType.MAIN_TEXTURE).build(),
                        ModelFileEntity.builder().id("t2").name("oblasti.jpg").senseType(FileSenseType.OTHER_TEXTURE)
                                .related(List.of(csv)).build())).build())
                .build();
    }

    private static InputStreamMultipartFile file(String name) {
        return new InputStreamMultipartFile(new ByteArrayInputStream(new byte[]{1}), name, name);
    }
}
