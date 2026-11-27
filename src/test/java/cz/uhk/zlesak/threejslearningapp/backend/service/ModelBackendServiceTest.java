package cz.uhk.zlesak.threejslearningapp.backend.service;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.common.logging.AuditLog;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ListingQueries;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.ModelRepository;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.MongoCollections;
import cz.uhk.zlesak.threejslearningapp.backend.persistence.VersionedWrites;
import cz.uhk.zlesak.threejslearningapp.testsupport.VersionedCollectionFake;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.InputFileDesc;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private MongoTemplate mongoTemplate;
    private VersionedCollectionFake<QuickModelEntity> models;

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

        mongoTemplate = Mockito.mock(MongoTemplate.class);
        models = new VersionedCollectionFake<>(mongoTemplate, QuickModelEntity.class, MongoCollections.MODEL,
                ModelBackendServiceTest::copy);
        Mockito.when(modelRepository.findById(Mockito.anyString()))
                .thenAnswer(call -> models.find(call.getArgument(0)));

        modelBackendService = new ModelBackendService(modelRepository, fileStorageService,
                Mockito.mock(ListingQueries.class), currentUserProvider,
                Mockito.mock(AuditLog.class), new VersionedWrites(mongoTemplate), chapterLookup);

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
        models.put(storedModel(oldRoot, T1));
        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any())).thenReturn(newRoot);

        QuickModelEntity updated = modelBackendService.update("model-1", uploads(), descriptor(), "d", false, T1);

        assertThat(updated.getModel().getId()).isEqualTo("new-file");
        assertThat(models.get("model-1").getModel().getId()).isEqualTo("new-file");
        Mockito.verify(fileStorageService).deleteTree(oldRoot);
        // The check and the switch are one conditional write, not a load followed by a save.
        Mockito.verify(modelRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void aSaveBasedOnAnOlderVersionStoresNoFilesAndDeletesNone() {
        ModelFileEntity theirRoot = file("their-file", "their.glb", FileSenseType.MODEL);
        models.put(storedModel(theirRoot, T2));

        assertThatThrownBy(() -> modelBackendService.update("model-1", uploads(), descriptor(), "d", false, T1))
                .isInstanceOfSatisfying(BackendException.Conflict.class, conflict ->
                        assertThat(((QuickModelEntity) conflict.getCurrent()).getModel().getId()).isEqualTo("their-file"));

        Mockito.verify(fileStorageService, Mockito.never()).store(Mockito.any(), Mockito.any());
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
        assertThat(models.get("model-1").getModel().getId()).isEqualTo("their-file");
    }

    @Test
    void aSaveThatLosesTheRaceRemovesOnlyTheFilesItStored() {
        ModelFileEntity sharedRoot = file("shared-file", "shared.glb", FileSenseType.MODEL);
        ModelFileEntity newRoot = file("new-file", "new.glb", FileSenseType.MODEL);
        models.put(storedModel(sharedRoot, T1));
        // Another save lands while this one is storing its files. It kept the same files, so they
        // belong to the stored version and must survive this attempt being turned away.
        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any())).thenAnswer(call -> {
            models.put(storedModel(sharedRoot, T2));
            return newRoot;
        });

        assertThatThrownBy(() -> modelBackendService.update("model-1", uploads(), descriptor(), "d", false, T1))
                .isInstanceOf(BackendException.Conflict.class);

        Mockito.verify(fileStorageService).deleteTree(newRoot);
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(sharedRoot);
        assertThat(models.get("model-1").getUpdated()).isEqualTo(T2);
    }

    @Test
    void aFailedWriteRemovesTheFilesItStored() {
        ModelFileEntity oldRoot = file("old-file", "old.glb", FileSenseType.MODEL);
        ModelFileEntity newRoot = file("new-file", "new.glb", FileSenseType.MODEL);
        models.put(storedModel(oldRoot, T1));
        Mockito.when(fileStorageService.store(Mockito.anyMap(), Mockito.any())).thenReturn(newRoot);
        Mockito.when(mongoTemplate.findAndReplace(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.eq(QuickModelEntity.class), Mockito.anyString()))
                .thenThrow(new IllegalStateException("Mongo nedostupná"));

        assertThatThrownBy(() -> modelBackendService.update("model-1", uploads(), descriptor(), "d", false, T1))
                .isInstanceOf(IllegalStateException.class);

        Mockito.verify(fileStorageService).deleteTree(newRoot);
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(oldRoot);
    }

    @Test
    void keepingTheStoredFilesRenamesWithoutTouchingAnyFile() {
        ModelFileEntity theirRoot = file("their-file", "Lebka", FileSenseType.MODEL);
        models.put(storedModel(theirRoot, T2));

        QuickModelEntity renamed = modelBackendService.renameKeepingFiles("model-1", "Lebka nová", T2);

        assertThat(models.get("model-1").getName()).isEqualTo("Lebka nová");
        assertThat(models.get("model-1").getModel().getId()).isEqualTo("their-file");
        assertThat(renamed.getUpdated()).isAfter(T2);
        Mockito.verify(fileStorageService, Mockito.never()).store(Mockito.any(), Mockito.any());
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
    }

    @Test
    void aRenameThatLosesTheRaceIsRejected() {
        ModelFileEntity root = file("their-file", "Lebka", FileSenseType.MODEL);
        models.put(storedModel(root, T1));
        // The first read passes the early check; another save lands before the write.
        java.util.concurrent.atomic.AtomicBoolean raced = new java.util.concurrent.atomic.AtomicBoolean();
        Mockito.when(modelRepository.findById("model-1")).thenAnswer(call -> {
            Optional<QuickModelEntity> read = models.find("model-1");
            if (raced.compareAndSet(false, true)) {
                models.put(QuickModelEntity.builder().id("model-1").name("Lebka třetí").creatorId("alice")
                        .model(file("their-file", "Lebka", FileSenseType.MODEL)).updated(T2).build());
            }
            return read;
        });

        assertThatThrownBy(() -> modelBackendService.renameKeepingFiles("model-1", "Lebka nová", T1))
                .isInstanceOf(BackendException.Conflict.class);

        assertThat(models.get("model-1").getName()).isEqualTo("Lebka třetí");
        assertThat(models.get("model-1").getUpdated()).isEqualTo(T2);
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
    }

    @Test
    void keepingFilesOfAVersionThatIsNoLongerStoredIsAConflict() {
        models.put(storedModel(file("newer-file", "Lebka", FileSenseType.MODEL), T2));

        assertThatThrownBy(() -> modelBackendService.renameKeepingFiles("model-1", "Lebka nová", T1))
                .isInstanceOf(BackendException.Conflict.class);

        assertThat(models.get("model-1").getName()).isEqualTo("Lebka");
        Mockito.verify(fileStorageService, Mockito.never()).deleteTree(Mockito.any());
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

        assertThatThrownBy(() -> modelBackendService.update("model-1", List.of(), metadata, "popis", false, null))
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

    private static final Instant T1 = Instant.parse("2026-11-01T10:00:00.123Z");
    private static final Instant T2 = Instant.parse("2026-11-01T10:05:00.456Z");

    private QuickModelEntity storedModel(ModelFileEntity root, Instant updated) {
        return QuickModelEntity.builder().id("model-1").name(root.getName()).creatorId("alice")
                .model(root).updated(updated).build();
    }

    private static QuickModelEntity copy(QuickModelEntity stored) {
        return QuickModelEntity.builder().id(stored.getId()).name(stored.getName())
                .creatorId(stored.getCreatorId()).creatorName(stored.getCreatorName())
                .description(stored.getDescription()).created(stored.getCreated()).updated(stored.getUpdated())
                .model(stored.getModel()).isAdvanced(stored.isAdvanced())
                .mainTexture(stored.getMainTexture()).otherTextures(stored.getOtherTextures()).build();
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
