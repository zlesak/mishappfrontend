package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.BinaryFileStore;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private BinaryFileStore binaryFileStore;
    private FileStorageService fileStorageService;
    private final AtomicInteger storedCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        binaryFileStore = Mockito.mock(BinaryFileStore.class);
        Mockito.when(binaryFileStore.store(Mockito.any()))
                .thenAnswer(invocation -> "file-" + storedCount.incrementAndGet());
        fileStorageService = new FileStorageService(binaryFileStore);
    }

    @Test
    void storesTheWholeHierarchyAndReportsTheAssignedIds() {
        InputFileDesc csv = desc("areas.csv", FileSenseType.CSV_FILE);
        InputFileDesc texture = desc("texture.jpg", FileSenseType.OTHER_TEXTURE, csv);
        InputFileDesc model = desc("model.glb", FileSenseType.MODEL, texture);

        ModelFileEntity root = fileStorageService.store(files("model.glb", "texture.jpg", "areas.csv"), model);

        assertThat(root.getId()).isNotBlank();
        assertThat(root.getSenseType()).isEqualTo(FileSenseType.MODEL);
        assertThat(root.getRelated()).singleElement()
                .satisfies(child -> assertThat(child.getSenseType()).isEqualTo(FileSenseType.OTHER_TEXTURE));
        assertThat(root.getRelated().getFirst().getRelated()).singleElement()
                .satisfies(csvNode -> assertThat(csvNode.getSenseType()).isEqualTo(FileSenseType.CSV_FILE));
    }

    @Test
    void uploadsAFileSharedByTwoBranchesOnlyOnce() {
        InputFileDesc sharedLeft = desc("shared.jpg", FileSenseType.OTHER_TEXTURE);
        InputFileDesc sharedRight = desc("shared.jpg", FileSenseType.MAIN_TEXTURE);
        InputFileDesc model = desc("model.glb", FileSenseType.MODEL, sharedLeft, sharedRight);

        ModelFileEntity root = fileStorageService.store(files("model.glb", "shared.jpg"), model);

        assertThat(root.getRelated()).hasSize(2);
        assertThat(root.getRelated().get(0).getId()).isEqualTo(root.getRelated().get(1).getId());
        Mockito.verify(binaryFileStore, Mockito.times(2)).store(Mockito.any());
    }

    @Test
    void rejectsADescriptorNamingAFileThatWasNotUploaded() {
        InputFileDesc model = desc("model.glb", FileSenseType.MODEL, desc("missing.jpg", FileSenseType.MAIN_TEXTURE));

        assertThatThrownBy(() -> fileStorageService.store(files("model.glb"), model))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("missing.jpg");
    }

    @Test
    void rejectsADescriptorThatReferencesItself() {
        InputFileDesc model = new InputFileDesc("model.glb", "Model", "", FileSenseType.MODEL, new ArrayList<>(), null);
        model.getRelatedFiles().add(model);

        assertThatThrownBy(() -> fileStorageService.store(files("model.glb"), model))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("cyklus");
    }

    @Test
    void rejectsAHierarchyDeeperThanAModelEverIs() {
        InputFileDesc deepest = desc("f9.bin", FileSenseType.CSV_FILE);
        InputFileDesc current = deepest;
        for (int level = 8; level >= 1; level--) {
            current = desc("f" + level + ".bin", FileSenseType.OTHER_TEXTURE, current);
        }
        InputFileDesc model = desc("model.glb", FileSenseType.MODEL, current);

        Map<String, MultipartFile> uploads = files("model.glb", "f9.bin");
        for (int level = 1; level <= 8; level++) {
            uploads.putAll(files("f" + level + ".bin"));
        }

        assertThatThrownBy(() -> fileStorageService.store(uploads, model))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("zanořená");
    }

    @Test
    void deletesEveryBinaryBelowTheRoot() {
        ModelFileEntity csv = ModelFileEntity.builder().id("csv-1").name("a.csv").related(List.of()).build();
        ModelFileEntity texture = ModelFileEntity.builder().id("tex-1").name("t.jpg").related(List.of(csv)).build();
        ModelFileEntity model = ModelFileEntity.builder().id("model-1").name("m.glb").related(List.of(texture)).build();

        fileStorageService.deleteTree(model);

        Mockito.verify(binaryFileStore).delete("csv-1");
        Mockito.verify(binaryFileStore).delete("tex-1");
        Mockito.verify(binaryFileStore).delete("model-1");
    }

    @Test
    void keepsDeletingSiblingsWhenOneFileCannotBeRemoved() {
        Mockito.doThrow(new IllegalStateException("gone")).when(binaryFileStore).delete("tex-1");
        ModelFileEntity texture = ModelFileEntity.builder().id("tex-1").name("t.jpg").related(List.of()).build();
        ModelFileEntity model = ModelFileEntity.builder().id("model-1").name("m.glb").related(List.of(texture)).build();

        fileStorageService.deleteTree(model);

        Mockito.verify(binaryFileStore).delete("model-1");
    }

    @Test
    void findsChildrenByTheRoleTheyPlay() {
        ModelFileEntity main = ModelFileEntity.builder().id("m").senseType(FileSenseType.MAIN_TEXTURE).build();
        ModelFileEntity other = ModelFileEntity.builder().id("o").senseType(FileSenseType.OTHER_TEXTURE).build();
        ModelFileEntity root = ModelFileEntity.builder().id("r").related(List.of(main, other)).build();

        assertThat(FileStorageService.childrenOfType(root, FileSenseType.MAIN_TEXTURE))
                .extracting(ModelFileEntity::getId).containsExactly("m");
        assertThat(FileStorageService.childrenOfType(root, FileSenseType.MAIN_TEXTURE, FileSenseType.OTHER_TEXTURE))
                .hasSize(2);
        assertThat(FileStorageService.childrenOfType(null, FileSenseType.MODEL)).isEmpty();
    }

    private InputFileDesc desc(String fileName, FileSenseType senseType, InputFileDesc... children) {
        return new InputFileDesc(fileName, fileName, "", senseType, new ArrayList<>(List.of(children)), null);
    }

    private Map<String, MultipartFile> files(String... names) {
        Map<String, MultipartFile> uploads = new LinkedHashMap<>();
        for (String name : names) {
            uploads.put(name, new MockMultipartFile("files", name, "application/octet-stream",
                    name.getBytes(StandardCharsets.UTF_8)));
        }
        return uploads;
    }
}
