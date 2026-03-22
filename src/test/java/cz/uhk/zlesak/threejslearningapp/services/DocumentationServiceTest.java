package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.clients.DocumentationApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentationServiceTest {
    @TempDir
    Path tempDir;

    private DocumentationService documentationService;

    @BeforeEach
    void setUp() {
        DocumentationApiClient documentationApiClient = mock(DocumentationApiClient.class);
        documentationService = new DocumentationService(documentationApiClient, new ObjectMapper());
        ReflectionTestUtils.setField(documentationService, "storagePath", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getEntriesByType_shouldFilterByCurrentUserRole() throws Exception {
        String json = """
                [
                  {"id":"e-1","type":"test-student-type","title":"Student","content":"{}","roles":["ROLE_STUDENT"]},
                  {"id":"e-2","type":"test-admin-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"e-3","type":"test-public-type","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_test.json"), json, StandardCharsets.UTF_8);

        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pass", "ROLE_STUDENT");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(1, documentationService.getEntriesByType("test-student-type").size());
        assertEquals(0, documentationService.getEntriesByType("test-admin-type").size());
        assertEquals(1, documentationService.getEntriesByType("test-public-type").size());
    }

    @Test
    void saveAll_shouldPersistToConfiguredStorage() {
        DocumentationEntry entry = new DocumentationEntry("save-1", "test-persist-type", "Persisted", "{}", List.of());

        documentationService.saveAll(List.of(entry));

        Path storedFile = tempDir.resolve("documentation_cs.json");
        assertTrue(Files.exists(storedFile));
        assertEquals(1, documentationService.getEntriesByType("test-persist-type").size());
    }

    @Test
    void saveAll_shouldWrapCriticalStorageFailures() throws Exception {
        Path blockedPath = tempDir.resolve("blocked");
        Files.writeString(blockedPath, "occupied", StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(documentationService, "storagePath", blockedPath.toString());
        DocumentationEntry entry = new DocumentationEntry("save-1", "test-persist-type", "Persisted", "{}", List.of());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> documentationService.saveAll(List.of(entry)));

        assertEquals("Documentation save failure", thrown.getMessage());
    }

    @Test
    void getEntries_shouldSupportSingleJsonEntryAndUnauthenticatedUser() throws Exception {
        String json = """
                {"id":"single-1","type":"single-type","title":"Single","content":"{}","roles":[]}
                """;
        Files.writeString(tempDir.resolve("documentation_single.json"), json, StandardCharsets.UTF_8);

        assertEquals(1, documentationService.getEntries().size());
        assertEquals(1, documentationService.getEntriesByType("single-type").size());
    }

    @Test
    void refresh_shouldDropCacheAndIgnoreInvalidExternalJson() throws Exception {
        Files.writeString(tempDir.resolve("documentation_invalid.json"), "not-json", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("documentation_valid.json"),
                """
                [{"id":"valid-1","type":"refresh-type","title":"Valid","content":"{}","roles":[]}]
                """, StandardCharsets.UTF_8);

        assertEquals(1, documentationService.getEntriesByType("refresh-type").size());

        Files.writeString(tempDir.resolve("documentation_valid.json"),
                """
                [{"id":"valid-2","type":"refresh-type","title":"Updated","content":"{}","roles":[]}]
                """, StandardCharsets.UTF_8);
        documentationService.refresh();

        assertEquals("valid-2", documentationService.getEntriesByType("refresh-type").getFirst().getId());
    }

    @Test
    void validateCreateEntity_shouldThrowNotImplemented() {
        DocumentationEntry entry = new DocumentationEntry("id", "type", "Title", "{}", List.of());

        assertThrows(RuntimeException.class, () -> documentationService.validateCreateEntity(entry));
    }

    @Test
    void createFinalEntity_shouldThrowNotImplemented() {
        DocumentationEntry entry = new DocumentationEntry("id", "type", "Title", "{}", List.of());

        assertThrows(RuntimeException.class, () -> documentationService.createFinalEntity(entry));
    }

    @Test
    void getEntriesByType_shouldReturnEmptyForNullType() {
        assertTrue(documentationService.getEntriesByType(null).isEmpty());
    }

    @Test
    void getEntries_shouldIgnoreEntriesWithoutIdsAndAllowEntriesWithNullRoles() throws Exception {
        String json = """
                [
                  null,
                  {"id":null,"type":"ignored-type","title":"Ignored","content":"{}","roles":[]},
                  {"id":"entry-1","type":"kept-type","title":"Kept","content":"{}","roles":null}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_partial.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> entries = documentationService.getEntries();

        assertEquals(1, entries.size());
        assertEquals("entry-1", entries.getFirst().getId());
    }

    @Test
    void getEntries_shouldReturnNoRestrictedEntriesWhenSecurityContextFails() throws Exception {
        String json = """
                [{"id":"entry-1","type":"restricted","title":"Restricted","content":"{}","roles":["ROLE_ADMIN"]}]
                """;
        Files.writeString(tempDir.resolve("documentation_restricted.json"), json, StandardCharsets.UTF_8);
        SecurityContext brokenContext = mock(SecurityContext.class);
        when(brokenContext.getAuthentication()).thenThrow(new RuntimeException("broken"));
        SecurityContextHolder.setContext(brokenContext);

        assertTrue(documentationService.getEntries().isEmpty());
    }

    @Test
    void getAllEntriesForSave_shouldBypassRoleFiltering() throws Exception {
        String json = """
                [
                  {"id":"entry-admin","type":"restricted","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"entry-public","type":"public","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_all_for_save.json"), json, StandardCharsets.UTF_8);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("student", "pass", "ROLE_STUDENT"));

        List<DocumentationEntry> visibleEntries = documentationService.getEntries();
        List<DocumentationEntry> allEntries = documentationService.getAllEntriesForSave();

        assertTrue(visibleEntries.stream().noneMatch(entry -> "entry-admin".equals(entry.getId())));
        assertTrue(allEntries.stream().anyMatch(entry -> "entry-admin".equals(entry.getId())));
        assertTrue(allEntries.stream().anyMatch(entry -> "entry-public".equals(entry.getId())));
    }

    @Test
    void getEntries_shouldTreatRoleAdministratorAsRoleAdminAlias() throws Exception {
        String json = """
                [
                  {"id":"entry-admin","type":"restricted","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_admin_alias.json"), json, StandardCharsets.UTF_8);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("administrator", "pass", "ROLE_ADMINISTRATOR"));

        List<DocumentationEntry> entries = documentationService.getEntriesByType("restricted");
        assertEquals(1, entries.size());
        assertEquals("entry-admin", entries.getFirst().getId());
    }
}
