package cz.uhk.zlesak.threejslearningapp.api;

import cz.uhk.zlesak.threejslearningapp.backend.service.FileStorageService;
import cz.uhk.zlesak.threejslearningapp.backend.service.ModelBackendService;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ModelApiClientTest {

    private static final Instant BASE = Instant.parse("2026-11-01T10:00:00.123Z");

    private final ModelBackendService backend = Mockito.mock(ModelBackendService.class);
    private final ModelApiClient client = new ModelApiClient(backend, Mockito.mock(FileStorageService.class));

    @Test
    void aVersionThatKeepsTheStoredFilesUploadsNothing() {
        Mockito.when(backend.renameKeepingFiles("model-1", "Lebka", BASE))
                .thenReturn(QuickModelEntity.builder().id("model-1").build());

        client.update("model-1", ModelEntity.builder().name("Lebka").updated(BASE).keepStoredFiles(true).build());

        Mockito.verify(backend).renameKeepingFiles("model-1", "Lebka", BASE);
        Mockito.verify(backend, Mockito.never()).update(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    void anUploadIsCheckedAgainstTheVersionItWasBasedOn() {
        Mockito.when(backend.update(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.any())).thenReturn(QuickModelEntity.builder().id("model-1").build());

        client.update("model-1", ModelEntity.builder().name("Lebka").updated(BASE)
                .inputStreamMultipartFile(new InputStreamMultipartFile(
                        new ByteArrayInputStream(new byte[]{1}), "lebka.glb", "lebka.glb"))
                .build());

        Mockito.verify(backend).update(Mockito.eq("model-1"), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.eq(BASE));
        assertThat(Mockito.mockingDetails(backend).getInvocations()).noneMatch(call ->
                call.getMethod().getName().equals("renameKeepingFiles"));
    }
}
