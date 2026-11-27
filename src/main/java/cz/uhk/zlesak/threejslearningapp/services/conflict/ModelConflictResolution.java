package cz.uhk.zlesak.threejslearningapp.services.conflict;

import cz.uhk.zlesak.threejslearningapp.api.ModelUploadAssembler;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Compares and combines two versions of a 3D model: its name, and its files and appearance.
 *
 * <p>The files are one field: the model, its textures, their CSV annotations, the background image
 * and the thumbnail rendered from them only make sense together. Keeping the stored files leaves
 * them exactly as they are (nothing is uploaded or deleted); keeping the user's files uploads them,
 * and the stored version's files are removed only once that save has been accepted.
 *
 * <p>The files field is offered whenever the user's version has files of its own, which is every
 * save from the editor: a model save uploads all of its files again, and whether their bytes match
 * the stored ones cannot be told from here. The thumbnails show the difference that matters to a
 * person. A version from an earlier round of the dialog that kept the stored files has none, so
 * the field is not offered and the stored files stay.
 */
public class ModelConflictResolution implements ConflictResolution<QuickModelEntity> {

    public static final String NAME = "name";
    public static final String FILES = "files";

    private final Translator translator;
    private final UnaryOperator<String> thumbnailOf;
    private final UnaryOperator<String> storedFileName;

    /**
     * @param translator     text lookup.
     * @param thumbnailOf    extracts the thumbnail URL from a model's stored description.
     * @param storedFileName looks up the name a stored file was uploaded under, by its id; may
     *                       return {@code null}. The stored model keeps only the model's display
     *                       name on its file entry, not the file's own name.
     */
    public ModelConflictResolution(Translator translator, UnaryOperator<String> thumbnailOf,
                                   UnaryOperator<String> storedFileName) {
        this.translator = translator;
        this.thumbnailOf = thumbnailOf;
        this.storedFileName = storedFileName;
    }

    @Override
    public List<ConflictField> differences(QuickModelEntity mine, QuickModelEntity theirs) {
        List<ConflictField> fields = new ArrayList<>();
        if (!Objects.equals(mine.getName(), theirs.getName())) {
            fields.add(new ConflictField(NAME, translator.text("conflict.model.field.name"),
                    mine.getName(), theirs.getName()));
        }
        // A version that already chose to keep the stored files has no files of its own to offer;
        // that happens when saving that choice ran into yet another save.
        if (uploadOf(mine) != null) {
            fields.add(new ConflictField(FILES, translator.text("conflict.model.field.files"),
                    files(mine), files(theirs),
                    blankToNull(thumbnailOf.apply(mine.getDescription())),
                    blankToNull(thumbnailOf.apply(theirs.getDescription()))));
        }
        return fields;
    }

    @Override
    public QuickModelEntity merge(QuickModelEntity mine, QuickModelEntity theirs, Map<String, ConflictChoice> choices) {
        String name = ConflictResolution.takesTheirs(choices, NAME) ? theirs.getName() : mine.getName();

        ModelEntity upload = uploadOf(mine);
        if (ConflictResolution.takesTheirs(choices, FILES) || upload == null) {
            return ModelEntity.builder()
                    .id(theirs.getId())
                    .name(name)
                    .updated(theirs.getUpdated())
                    .keepStoredFiles(true)
                    .build();
        }
        return ModelEntity.builder()
                .id(theirs.getId())
                .name(name)
                .description(upload.getDescription())
                .isAdvanced(upload.isAdvanced())
                .inputStreamMultipartFile(upload.getInputStreamMultipartFile())
                .fullMainTexture(upload.getFullMainTexture())
                .fullOtherTextures(upload.getFullOtherTextures())
                .csvFiles(upload.getCsvFiles())
                .backgroundImageFile(upload.getBackgroundImageFile())
                .updated(theirs.getUpdated())
                .build();
    }

    /**
     * Lists the model file, then the textures, annotations and background.
     */
    private String files(QuickModelEntity model) {
        List<String> lines = new ArrayList<>();
        if (model instanceof ModelEntity upload && upload.getInputStreamMultipartFile() != null) {
            lines.add(translator.text("conflict.model.files.model",
                    Objects.toString(upload.getInputStreamMultipartFile().getOriginalFilename(), "")));
            InputFileDesc root = ModelUploadAssembler.describe(upload);
            for (InputFileDesc child : root.getRelatedFiles()) {
                describe(child.getFileSenseType(), child.getName(), lines, "");
                for (InputFileDesc csv : child.getRelatedFiles() == null ? List.<InputFileDesc>of() : child.getRelatedFiles()) {
                    describe(csv.getFileSenseType(), csv.getName(), lines, "  ");
                }
            }
        } else if (model.getModel() != null) {
            String fileName = model.getModel().getId() == null ? null : storedFileName.apply(model.getModel().getId());
            lines.add(translator.text("conflict.model.files.model",
                    Objects.toString(fileName != null ? fileName : model.getModel().getName(), "")));
            List<ModelFileEntity> related = model.getModel().getRelated() == null ? List.of() : model.getModel().getRelated();
            for (ModelFileEntity child : related) {
                describe(child.getSenseType(), child.getName(), lines, "");
                for (ModelFileEntity csv : child.getRelated() == null ? List.<ModelFileEntity>of() : child.getRelated()) {
                    describe(csv.getSenseType(), csv.getName(), lines, "  ");
                }
            }
        }
        return String.join("\n", lines);
    }

    private void describe(FileSenseType type, String name, List<String> lines, String indent) {
        String key = switch (type == null ? FileSenseType.MODEL : type) {
            case MAIN_TEXTURE -> "conflict.model.files.mainTexture";
            case OTHER_TEXTURE -> "conflict.model.files.otherTexture";
            case CSV_FILE -> "conflict.model.files.csv";
            case BACKGROUND_IMAGE -> "conflict.model.files.background";
            default -> null;
        };
        if (key != null) {
            lines.add(indent + translator.text(key, Objects.toString(name, "")));
        }
    }

    /**
     * @return the user's version as an upload, or {@code null} when it carries no files of its own
     * (it kept the stored files, or it is not an upload at all).
     */
    private static ModelEntity uploadOf(QuickModelEntity mine) {
        return mine instanceof ModelEntity upload && !upload.isKeepStoredFiles()
                && upload.getInputStreamMultipartFile() != null ? upload : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
