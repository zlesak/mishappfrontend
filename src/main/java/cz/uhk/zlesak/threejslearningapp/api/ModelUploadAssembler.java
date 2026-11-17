package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a filled-in {@link ModelEntity} into the two things an upload needs: the flat list of files
 * and the descriptor tree explaining how they relate.
 * Shared by the embedded and remote clients so both describe an upload identically.
 */
public final class ModelUploadAssembler {

    private ModelUploadAssembler() {
    }

    /**
     * Describes the model's file hierarchy: the model at the root, textures below it, and each
     * texture's CSV annotation below that. CSV files are matched to their texture by base name.
     *
     * @param entity model being uploaded.
     * @return the descriptor tree.
     */
    public static InputFileDesc describe(ModelEntity entity) {
        List<InputFileDesc> relatedFiles = new ArrayList<>();

        if (hasFile(entity.getFullMainTexture())) {
            InputStreamMultipartFile file = entity.getFullMainTexture().getTextureFile();
            relatedFiles.add(new InputFileDesc(file.getOriginalFilename(), file.getDisplayName(), "",
                    FileSenseType.MAIN_TEXTURE, List.of(), null));
        }

        if (entity.getFullOtherTextures() != null) {
            for (TextureEntity texture : entity.getFullOtherTextures()) {
                if (!hasFile(texture)) {
                    continue;
                }
                InputStreamMultipartFile textureFile = texture.getTextureFile();
                relatedFiles.add(new InputFileDesc(textureFile.getOriginalFilename(), textureFile.getDisplayName(), "",
                        FileSenseType.OTHER_TEXTURE, csvChildrenOf(entity, textureFile), null));
            }
        }

        if (entity.getBackgroundImageFile() != null) {
            InputStreamMultipartFile background = entity.getBackgroundImageFile();
            relatedFiles.add(new InputFileDesc(background.getOriginalFilename(), background.getDisplayName(), "",
                    FileSenseType.BACKGROUND_IMAGE, List.of(), null));
        }

        InputStreamMultipartFile modelFile = entity.getInputStreamMultipartFile();
        return new InputFileDesc(
                modelFile.getOriginalFilename(),
                entity.getName(),
                entity.getDescription() != null ? entity.getDescription() : "",
                FileSenseType.MODEL,
                relatedFiles,
                null);
    }

    /**
     * @param entity model being uploaded.
     * @return every file referenced by {@link #describe(ModelEntity)}, in upload order.
     */
    public static List<InputStreamMultipartFile> files(ModelEntity entity) {
        List<InputStreamMultipartFile> files = new ArrayList<>();
        files.add(entity.getInputStreamMultipartFile());

        if (hasFile(entity.getFullMainTexture())) {
            files.add(entity.getFullMainTexture().getTextureFile());
        }
        if (entity.getFullOtherTextures() != null) {
            entity.getFullOtherTextures().stream()
                    .filter(ModelUploadAssembler::hasFile)
                    .map(TextureEntity::getTextureFile)
                    .forEach(files::add);
        }
        if (entity.getCsvFiles() != null) {
            files.addAll(entity.getCsvFiles());
        }
        if (entity.getBackgroundImageFile() != null) {
            files.add(entity.getBackgroundImageFile());
        }
        return files;
    }

    private static List<InputFileDesc> csvChildrenOf(ModelEntity entity, InputStreamMultipartFile textureFile) {
        if (entity.getCsvFiles() == null) {
            return List.of();
        }

        List<InputFileDesc> children = new ArrayList<>();
        for (InputStreamMultipartFile csv : entity.getCsvFiles()) {
            if (toJpgName(csv.getOriginalFilename()).equals(textureFile.getOriginalFilename())) {
                children.add(new InputFileDesc(csv.getOriginalFilename(), csv.getOriginalFilename(), "",
                        FileSenseType.CSV_FILE, List.of(), null));
            }
        }
        return children;
    }

    private static boolean hasFile(TextureEntity texture) {
        return texture != null && texture.getTextureFile() != null;
    }

    /**
     * Mirrors the upload form's naming rule: a CSV belongs to the texture with the same base name.
     */
    private static String toJpgName(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base + ".jpg";
    }
}
