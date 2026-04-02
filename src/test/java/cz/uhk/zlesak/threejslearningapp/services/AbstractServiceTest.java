package cz.uhk.zlesak.threejslearningapp.services;

import cz.uhk.zlesak.threejslearningapp.api.contracts.IApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbstractServiceTest {
    private IApiClient<QuickModelEntity, QuickModelEntity, String> apiClient;
    private TestService service;

    @BeforeEach
    void setUp() {
        apiClient = mockApiClient();
        service = new TestService(apiClient);
    }

    @Test
    void createShouldValidateTransformAndReturnId() throws Exception {
        when(apiClient.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String id = service.create(QuickModelEntity.builder().id("created").name("Femur").build());

        assertEquals("created", id);
        assertEquals(1, service.validated);
        assertEquals(1, service.finalized);
    }

    @Test
    void readAndReadQuickShouldCacheById() throws Exception {
        when(apiClient.read("entity-1")).thenReturn(QuickModelEntity.builder().id("entity-1").name("One").build());
        when(apiClient.readQuick("quick-1")).thenReturn(QuickModelEntity.builder().id("quick-1").name("Quick").build());

        QuickModelEntity first = service.read("entity-1");
        QuickModelEntity second = service.read("entity-1");
        QuickModelEntity quickFirst = service.readQuick("quick-1");
        QuickModelEntity quickSecond = service.readQuick("quick-1");

        assertSame(first, second);
        assertSame(quickFirst, quickSecond);
        verify(apiClient, times(1)).read("entity-1");
        verify(apiClient, times(1)).readQuick("quick-1");
    }

    @Test
    void readEntitiesShouldReturnPageAndWrapFailures() throws Exception {
        FilterParameters<String> parameters = new FilterParameters<>();
        parameters.setPageRequest(PageRequest.of(0, 3));
        parameters.setFilter("search");
        when(apiClient.readEntities(parameters)).thenReturn(new PageResult<>(List.of(
                QuickModelEntity.builder().id("one").build()
        ), 1L, 0));

        assertEquals(1L, service.readEntities(parameters).total());

        when(apiClient.readEntities(parameters)).thenThrow(new IllegalStateException("boom"));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.readEntities(parameters));
        assertEquals("Chyba při získávání entit: boom", exception.getMessage());
    }

    @Test
    void updateShouldValidateIdAndWrapValidationErrors() throws Exception {
        when(apiClient.update(any(), any())).thenReturn(QuickModelEntity.builder().id("updated").build());

        assertEquals("updated", service.update("entity-9", QuickModelEntity.builder().name("Updated").build()));

        RuntimeException blankId = assertThrows(RuntimeException.class, () -> service.update("", QuickModelEntity.builder().name("x").build()));
        assertEquals("Chyba při validaci entity před aktualizací: ID entity nesmí být prázdné.", blankId.getMessage());

        RuntimeException invalidEntity = assertThrows(RuntimeException.class, () -> service.update("entity-9", QuickModelEntity.builder().name("invalid").build()));
        assertEquals("Chyba při validaci entity před aktualizací: invalid", invalidEntity.getMessage());
    }

    @Test
    void deleteShouldValidateIdAndWrapFailures() throws Exception {
        when(apiClient.delete("entity-1")).thenReturn(true);
        assertTrue(service.delete("entity-1"));

        RuntimeException blankId = assertThrows(RuntimeException.class, () -> service.delete(""));
        assertEquals("Chyba při mazání entity: ID entity nesmí být prázdné.", blankId.getMessage());

        when(apiClient.delete("entity-2")).thenThrow(new IllegalStateException("nope"));
        RuntimeException apiError = assertThrows(RuntimeException.class, () -> service.delete("entity-2"));
        assertEquals("Chyba při mazání entity: nope", apiError.getMessage());
    }

    @Test
    void create_shouldWrapValidationException() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.create(QuickModelEntity.builder().id("bad").name("invalid").build()));

        assertTrue(ex.getMessage().startsWith("Chyba při vytváření entity"));
    }

    @Test
    void create_shouldWrapApiClientException() throws Exception {
        when(apiClient.create(any())).thenThrow(new IllegalStateException("api down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.create(QuickModelEntity.builder().id("x").name("Valid").build()));

        assertTrue(ex.getMessage().startsWith("Chyba při vytváření entity"));
    }

    @Test
    void read_shouldThrowForBlankId() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.read(""));

        assertTrue(ex.getMessage().startsWith("Chyba při získávání entity"));
    }

    @Test
    void read_shouldThrowForNullId() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.read(null));

        assertTrue(ex.getMessage().startsWith("Chyba při získávání entity"));
    }

    @Test
    void readQuick_shouldThrowForBlankId() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.readQuick(""));

        assertTrue(ex.getMessage().startsWith("Chyba při získávání quick entity"));
    }

    @Test
    void readQuick_shouldThrowForNullId() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.readQuick(null));

        assertTrue(ex.getMessage().startsWith("Chyba při získávání quick entity"));
    }

    @Test
    void delete_shouldReturnFalseWhenApiClientReturnsFalse() throws Exception {
        when(apiClient.delete("entity-false")).thenReturn(false);

        assertFalse(service.delete("entity-false"));
    }

    @Test
    void update_shouldThrowWhenApiClientFails() throws Exception {
        when(apiClient.update(any(), any())).thenThrow(new IllegalStateException("update failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.update("entity-x", QuickModelEntity.builder().name("Valid").build()));

        assertTrue(ex.getMessage().startsWith("Chyba při aktualizaci kapitoly"));
    }

    private static final class TestService extends AbstractService<QuickModelEntity, QuickModelEntity, String> {
        private int validated;
        private int finalized;

        private TestService(IApiClient<QuickModelEntity, QuickModelEntity, String> apiClient) {
            super(apiClient);
        }

        @Override
        protected QuickModelEntity validateCreateEntity(QuickModelEntity createEntity) {
            validated++;
            if ("invalid".equals(createEntity.getName())) {
                throw new RuntimeException("invalid");
            }
            return createEntity;
        }

        @Override
        protected QuickModelEntity createFinalEntity(QuickModelEntity createEntity) {
            finalized++;
            return createEntity;
        }
    }

    @SuppressWarnings("unchecked")
    private static IApiClient<QuickModelEntity, QuickModelEntity, String> mockApiClient() {
        return (IApiClient<QuickModelEntity, QuickModelEntity, String>) mock(IApiClient.class);
    }
}
