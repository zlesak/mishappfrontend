package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ModelRepository;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelBackendServiceTest {

    private ModelRepository modelRepository;
    private FileStorageService fileStorageService;
    private ChapterBackendService chapterBackendService;
    private ModelBackendService modelBackendService;
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        modelRepository = Mockito.mock(ModelRepository.class);
        fileStorageService = Mockito.mock(FileStorageService.class);
        chapterBackendService = Mockito.mock(ChapterBackendService.class);
        currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        Mockito.when(currentUserProvider.requireUserId()).thenReturn("alice");
        Mockito.doCallRealMethod().when(currentUserProvider)
                .requireAuthorOrTeacher(Mockito.any(), Mockito.anyString());

        ObjectProvider<ChapterBackendService> chapterLookup = Mockito.mock(ObjectProvider.class);
        Mockito.when(chapterLookup.getObject()).thenReturn(chapterBackendService);

        modelBackendService = new ModelBackendService(modelRepository, fileStorageService,
                Mockito.mock(ListingQueries.class), currentUserProvider,
                Mockito.mock(AuditLog.class), chapterLookup);

        Mockito.when(modelRepository.save(Mockito.any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void derivesTheViewersTexturesFromTheStoredFileHierarchy() {
        ModelFileEntity csv = file("csv-1", "areas.csv", FileSenseType.CSV_FILE);
        ModelFileEntity mainTexture = file("tex-main", "main.jpg", FileSenseType.MAIN_TEXTURE);
        ModelFileEntity otherTexture = file("tex-other", "other.jpg", FileSenseType.OTHER_TEXTURE, csv);
        ModelFileEntity root = file("model-file", "model.glb", FileSenseType.MODEL, mainTexture, otherTexture);

        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any())).thenReturn(root);
        Mockito.when(fileStorageService.readText("csv-1")).thenReturn("#AABBCC;Lalok");

        QuickModelEntity saved = modelBackendService.upload(uploads(), descriptor(), "thumb", true);

        assertThat(saved.getMainTexture().getId()).isEqualTo("tex-main");
        assertThat(saved.getMainTexture().getIsPrimary()).isTrue();
        assertThat(saved.getOtherTextures()).singleElement()
                .satisfies(texture -> {
                    assertThat(texture.getId()).isEqualTo("tex-other");
                    assertThat(texture.getCsvContent()).isEqualTo("#AABBCC;Lalok");
                });
        assertThat(saved.getCreatorId()).isEqualTo("alice");
        assertThat(saved.isAdvanced()).isTrue();
    }

    @Test
    void leavesTheTexturesEmptyWhenTheModelHasNone() {
        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any()))
                .thenReturn(file("model-file", "model.glb", FileSenseType.MODEL));

        QuickModelEntity saved = modelBackendService.upload(uploads(), descriptor(), "", false);

        assertThat(saved.getMainTexture()).isNull();
        assertThat(saved.getOtherTextures()).isEmpty();
    }

    @Test
    void rejectsAnUploadWhoseRootIsNotTheModelFile() {
        InputFileDesc textureAsRoot = new InputFileDesc("t.jpg", "t", "", FileSenseType.MAIN_TEXTURE, List.of(), null);

        assertThatThrownBy(() -> modelBackendService.upload(uploads(), textureAsRoot, "", false))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("3D model");
    }

    @Test
    void replacesTheFilesOnlyAfterTheNewOnesAreStored() {
        ModelFileEntity oldRoot = file("old-file", "old.glb", FileSenseType.MODEL);
        ModelFileEntity newRoot = file("new-file", "new.glb", FileSenseType.MODEL);
        QuickModelEntity existing = QuickModelEntity.builder().id("model-1").name("Old").model(oldRoot).build();

        Mockito.when(modelRepository.findById("model-1")).thenReturn(Optional.of(existing));
        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any())).thenReturn(newRoot);

        QuickModelEntity updated = modelBackendService.update("model-1", uploads(), descriptor(), "d", false);

        assertThat(updated.getModel().getId()).isEqualTo("new-file");
        Mockito.verify(fileStorageService).deleteTree(oldRoot);
        Mockito.verify(modelRepository).save(Mockito.any());
    }

    @Test
    void refusesToDeleteAModelThatAChapterStillUses() {
        QuickModelEntity model = QuickModelEntity.builder().id("model-1").name("Lebka")
                .model(file("f", "m.glb", FileSenseType.MODEL)).build();
        Mockito.when(modelRepository.findById("model-1")).thenReturn(Optional.of(model));
        Mockito.when(chapterBackendService.usingModel("model-1"))
                .thenReturn(List.of(ChapterEntity.builder().id("ch-1").name("Mozek").build()));

        assertThatThrownBy(() -> modelBackendService.delete("model-1"))
                .isInstanceOf(BackendException.Validation.class)
                .hasMessageContaining("Mozek");

        Mockito.verify(modelRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
    }

    @Test
    void deletesAnUnusedModelWithItsFiles() {
        ModelFileEntity root = file("f", "m.glb", FileSenseType.MODEL);
        Mockito.when(modelRepository.findById("model-1"))
                .thenReturn(Optional.of(QuickModelEntity.builder().id("model-1").model(root).build()));
        Mockito.when(chapterBackendService.usingModel("model-1")).thenReturn(List.of());

        modelBackendService.delete("model-1");

        Mockito.verify(fileStorageService).deleteTree(root);
        Mockito.verify(modelRepository).deleteById("model-1");
    }

    @Test
    void letsOnlyTheAuthorOrATeacherDeleteAModel() {
        ModelFileEntity root = file("f", "m.glb", FileSenseType.MODEL);
        Mockito.when(modelRepository.findById("model-1")).thenReturn(Optional.of(
                QuickModelEntity.builder().id("model-1").model(root).creatorId("someone-else").build()));
        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(false);

        assertThatThrownBy(() -> modelBackendService.delete("model-1"))
                .isInstanceOf(BackendException.Forbidden.class);

        Mockito.verify(modelRepository, Mockito.never()).deleteById(Mockito.anyString());
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
    }

    @Test
    void letsOnlyTheAuthorOrATeacherReplaceAModelsFiles() {
        Mockito.when(modelRepository.findById("model-1")).thenReturn(Optional.of(
                QuickModelEntity.builder().id("model-1")
                        .model(file("f", "m.glb", FileSenseType.MODEL))
                        .creatorId("someone-else").build()));
        Mockito.when(currentUserProvider.hasRole("TEACHER")).thenReturn(false);

        InputFileDesc metadata = new InputFileDesc("m.glb", "Lebka", "", FileSenseType.MODEL, List.of(), null);

        assertThatThrownBy(() -> modelBackendService.update("model-1", List.of(), metadata, "popis", false))
                .isInstanceOf(BackendException.Forbidden.class);

        // The replacement files must not be stored for a change that is going to be refused.
        Mockito.verify(fileStorageService, Mockito.never()).store(Mockito.any(), Mockito.any());
        Mockito.verify(modelRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void reportsAMissingModelRatherThanReturningNothing() {
        Mockito.when(modelRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelBackendService.require("nope"))
                .isInstanceOf(BackendException.NotFound.class);
        assertThatThrownBy(() -> modelBackendService.require(" "))
                .isInstanceOf(BackendException.Validation.class);
    }

    @Test
    void returnsRequestedModelsInTheOrderTheyWereAskedForAndSkipsMissingOnes() {
        Mockito.when(modelRepository.findAllById(List.of("b", "a", "gone"))).thenReturn(List.of(
                QuickModelEntity.builder().id("a").build(),
                QuickModelEntity.builder().id("b").build()));

        List<QuickModelEntity> found = modelBackendService.findAll(List.of("b", "a", "gone"));

        assertThat(found).extracting(QuickModelEntity::getId).containsExactly("b", "a");
        assertThat(modelBackendService.findAll(List.of())).isEmpty();
        assertThat(modelBackendService.findAll(null)).isEmpty();
    }

    private ModelFileEntity file(String id, String name, FileSenseType senseType, ModelFileEntity... related) {
        return ModelFileEntity.builder().id(id).name(name).senseType(senseType).related(List.of(related)).build();
    }

    private InputFileDesc descriptor() {
        return new InputFileDesc("model.glb", "Model", "", FileSenseType.MODEL, List.of(), null);
    }

    private List<MultipartFile> uploads() {
        return List.of(new MockMultipartFile("files", "model.glb", "application/octet-stream",
                "model".getBytes(StandardCharsets.UTF_8)));
    }
}
