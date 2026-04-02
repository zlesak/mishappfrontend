package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.clients.DocumentationApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntryIndex;
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

    @Test
    void getEntriesForRoles_shouldFilterByExplicitRoles() throws Exception {
        String json = """
                [
                  {"id":"ef-1","type":"ef-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"ef-2","type":"ef-type","title":"Student","content":"{}","roles":["ROLE_STUDENT"]},
                  {"id":"ef-3","type":"ef-type","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_ef.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> adminEntries = documentationService.getEntriesForRoles(List.of("ROLE_ADMIN"));
        List<DocumentationEntry> studentEntries = documentationService.getEntriesForRoles(List.of("ROLE_STUDENT"));

        assertTrue(adminEntries.stream().anyMatch(e -> "ef-1".equals(e.getId())));
        assertTrue(adminEntries.stream().anyMatch(e -> "ef-3".equals(e.getId())));
        assertFalse(adminEntries.stream().anyMatch(e -> "ef-2".equals(e.getId())));
        assertTrue(studentEntries.stream().anyMatch(e -> "ef-2".equals(e.getId())));
        assertFalse(studentEntries.stream().anyMatch(e -> "ef-1".equals(e.getId())));
    }

    @Test
    void getEntriesForRoles_shouldReturnOnlyPublicEntriesForEmptyRoles() throws Exception {
        String json = """
                [
                  {"id":"ep-1","type":"ep-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"ep-2","type":"ep-type","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_ep.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> entries = documentationService.getEntriesForRoles(List.of());

        assertTrue(entries.stream().anyMatch(e -> "ep-2".equals(e.getId())));
        assertFalse(entries.stream().anyMatch(e -> "ep-1".equals(e.getId())));
    }

    @Test
    void getEntryIndexes_shouldReturnDocumentationEntryIndexList() throws Exception {
        String json = """
                [{"id":"idx-1","type":"idx-type","title":"IndexEntry","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_idx.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntryIndex> indexes = documentationService.getEntryIndexes();

        assertTrue(indexes.stream().anyMatch(i -> "idx-1".equals(i.getId())));
        assertTrue(indexes.stream().filter(i -> "idx-1".equals(i.getId()))
                .allMatch(i -> "idx-type".equals(i.getType()) && "IndexEntry".equals(i.getTitle())));
    }

    @Test
    void getEntryIndexesForRoles_shouldFilterByRole() throws Exception {
        String json = """
                [
                  {"id":"ir-admin","type":"ir-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"ir-public","type":"ir-type","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_ir.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntryIndex> adminIndexes = documentationService.getEntryIndexesForRoles(List.of("ROLE_ADMIN"));
        List<DocumentationEntryIndex> emptyIndexes = documentationService.getEntryIndexesForRoles(List.of());

        assertTrue(adminIndexes.stream().anyMatch(i -> "ir-admin".equals(i.getId())));
        assertTrue(adminIndexes.stream().anyMatch(i -> "ir-public".equals(i.getId())));
        assertFalse(emptyIndexes.stream().anyMatch(i -> "ir-admin".equals(i.getId())));
        assertTrue(emptyIndexes.stream().anyMatch(i -> "ir-public".equals(i.getId())));
    }

    @Test
    void getAllEntriesByTypeForSave_shouldReturnAllEntriesOfType() throws Exception {
        String json = """
                [
                  {"id":"at-admin","type":"at-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"at-public","type":"at-type","title":"Public","content":"{}","roles":[]},
                  {"id":"at-other","type":"other-at-type","title":"Other","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_at.json"), json, StandardCharsets.UTF_8);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("student", "pass", "ROLE_STUDENT"));

        List<DocumentationEntry> result = documentationService.getAllEntriesByTypeForSave("at-type");

        assertEquals(2, result.stream().filter(e -> "at-type".equals(e.getType())).count());
        assertTrue(result.stream().anyMatch(e -> "at-admin".equals(e.getId())));
        assertTrue(result.stream().anyMatch(e -> "at-public".equals(e.getId())));
        assertFalse(result.stream().anyMatch(e -> "at-other".equals(e.getId())));
    }

    @Test
    void getAllEntriesByTypeForSave_shouldReturnEmptyForNullType() {
        assertTrue(documentationService.getAllEntriesByTypeForSave(null).isEmpty());
    }

    @Test
    void getEntryIndexesByType_shouldReturnIndexesMatchingType() throws Exception {
        String json = """
                [
                  {"id":"bi-1","type":"bi-type","title":"BIdx","content":"{}","roles":[]},
                  {"id":"bi-2","type":"other-bi-type","title":"Other","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_bi.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntryIndex> result = documentationService.getEntryIndexesByType("bi-type");

        assertTrue(result.stream().anyMatch(i -> "bi-1".equals(i.getId())));
        assertFalse(result.stream().anyMatch(i -> "bi-2".equals(i.getId())));
    }

    @Test
    void getEntryIndexesByType_shouldReturnEmptyForNullType() {
        assertTrue(documentationService.getEntryIndexesByType(null).isEmpty());
    }

    @Test
    void getEntryIndexesByTypeForRoles_shouldApplyTypeAndRoleFilter() throws Exception {
        String json = """
                [
                  {"id":"tri-1","type":"tri-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"tri-2","type":"tri-type","title":"Public","content":"{}","roles":[]},
                  {"id":"tri-3","type":"other-tri-type","title":"Other","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_tri.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntryIndex> result = documentationService.getEntryIndexesByTypeForRoles("tri-type", List.of("ROLE_ADMIN"));

        assertTrue(result.stream().anyMatch(i -> "tri-1".equals(i.getId())));
        assertTrue(result.stream().anyMatch(i -> "tri-2".equals(i.getId())));
        assertFalse(result.stream().anyMatch(i -> "tri-3".equals(i.getId())));
    }

    @Test
    void getEntryIndexesByTypeForRoles_shouldReturnEmptyForNullType() {
        assertTrue(documentationService.getEntryIndexesByTypeForRoles(null, List.of("ROLE_ADMIN")).isEmpty());
    }

    @Test
    void getEntryDetail_shouldReturnEntryByIdForCurrentUser() throws Exception {
        String json = """
                [{"id":"det-1","type":"det-type","title":"Detail","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_det.json"), json, StandardCharsets.UTF_8);

        DocumentationEntry result = documentationService.getEntryDetail("det-1");

        assertNotNull(result);
        assertEquals("det-1", result.getId());
        assertEquals("Detail", result.getTitle());
    }

    @Test
    void getEntryDetail_shouldReturnNullForNonExistentId() throws Exception {
        String json = """
                [{"id":"det-exist","type":"det-type","title":"Existing","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_det_ne.json"), json, StandardCharsets.UTF_8);

        assertNull(documentationService.getEntryDetail("det-nonexistent"));
    }

    @Test
    void getEntryDetail_shouldReturnNullForBlankId() {
        assertNull(documentationService.getEntryDetail(""));
    }

    @Test
    void getEntryDetail_shouldReturnNullForNullId() {
        assertNull(documentationService.getEntryDetail(null));
    }

    @Test
    void getEntryDetailForRoles_shouldReturnNullWhenRoleNotSatisfied() throws Exception {
        String json = """
                [{"id":"rdet-1","type":"rdet-type","title":"Restricted","content":"{}","roles":["ROLE_ADMIN"]}]
                """;
        Files.writeString(tempDir.resolve("documentation_rdet.json"), json, StandardCharsets.UTF_8);

        assertNull(documentationService.getEntryDetailForRoles("rdet-1", List.of("ROLE_STUDENT")));
    }

    @Test
    void getEntryDetailForRoles_shouldReturnEntryWhenRoleMatches() throws Exception {
        String json = """
                [{"id":"rdet-2","type":"rdet2-type","title":"RoleDetail","content":"{}","roles":["ROLE_ADMIN"]}]
                """;
        Files.writeString(tempDir.resolve("documentation_rdet2.json"), json, StandardCharsets.UTF_8);

        DocumentationEntry result = documentationService.getEntryDetailForRoles("rdet-2", List.of("ROLE_ADMIN"));

        assertNotNull(result);
        assertEquals("rdet-2", result.getId());
    }

    @Test
    void getEntriesByTypeForRoles_shouldFilterByTypeAndRole() throws Exception {
        String json = """
                [
                  {"id":"tr-1","type":"tr-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"tr-2","type":"tr-type","title":"Public","content":"{}","roles":[]},
                  {"id":"tr-3","type":"other-tr-type","title":"Other","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_tr.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> result = documentationService.getEntriesByTypeForRoles("tr-type", List.of("ROLE_STUDENT"));

        assertEquals(1, result.stream().filter(e -> "tr-type".equals(e.getType())).count());
        assertEquals("tr-2", result.stream().filter(e -> "tr-type".equals(e.getType())).findFirst().get().getId());
    }

    @Test
    void getEntriesByTypeForRoles_shouldReturnEmptyForUnknownType() throws Exception {
        String json = """
                [{"id":"unk-1","type":"known-type","title":"Known","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_unk.json"), json, StandardCharsets.UTF_8);

        assertTrue(documentationService.getEntriesByTypeForRoles("unknown-type", List.of()).isEmpty());
    }

    @Test
    void getAllEntriesByTypeForSave_shouldBypassRoleRestrictionsForType() throws Exception {
        String json = """
                [
                  {"id":"byr-1","type":"byr-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]},
                  {"id":"byr-2","type":"byr-type","title":"Public","content":"{}","roles":[]}
                ]
                """;
        Files.writeString(tempDir.resolve("documentation_byr.json"), json, StandardCharsets.UTF_8);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("student", "pass", "ROLE_STUDENT"));

        List<DocumentationEntry> roleFiltered = documentationService.getEntriesByTypeForRoles("byr-type", List.of("ROLE_STUDENT"));
        List<DocumentationEntry> allForSave = documentationService.getAllEntriesByTypeForSave("byr-type");

        assertFalse(roleFiltered.stream().anyMatch(e -> "byr-1".equals(e.getId())));
        assertTrue(allForSave.stream().anyMatch(e -> "byr-1".equals(e.getId())));
        assertTrue(allForSave.stream().anyMatch(e -> "byr-2".equals(e.getId())));
    }

    @Test
    void normalizeRoles_withROLE_prefixedUserRole_shouldMatchEntryWithUnprefixedRole() throws Exception {
        String json = """
                [{"id":"nr-1","type":"nr-type","title":"Teacher","content":"{}","roles":["teacher"]}]
                """;
        Files.writeString(tempDir.resolve("documentation_nr.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> result = documentationService.getEntriesForRoles(List.of("ROLE_TEACHER"));

        assertEquals(1, result.stream().filter(e -> "nr-1".equals(e.getId())).count());
    }

    @Test
    void getEntriesByTypeForRoles_withNullType_shouldReturnEmptyList() {
        assertTrue(documentationService.getEntriesByTypeForRoles(null, List.of()).isEmpty());
    }

    @Test
    void loadFromExternalStorage_whenDirectoryDoesNotExist_shouldCreateDirectory() {
        Path nonExistentDir = tempDir.resolve("new-docs-dir");
        ReflectionTestUtils.setField(documentationService, "storagePath", nonExistentDir.toString());

        documentationService.getEntries();

        assertTrue(Files.exists(nonExistentDir));
        assertTrue(Files.isDirectory(nonExistentDir));
    }

    @Test
    void allowedForRoles_withNullRolesOnEntry_shouldBeAccessibleToUserWithRoles() throws Exception {
        String json = """
                [{"id":"nullr-1","type":"nullr-type","title":"NullRoles","content":"{}","roles":null}]
                """;
        Files.writeString(tempDir.resolve("documentation_nullr.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> result = documentationService.getEntriesForRoles(List.of("ROLE_STUDENT"));

        assertTrue(result.stream().anyMatch(e -> "nullr-1".equals(e.getId())));
    }

    @Test
    void allowedForRoles_withEmptyRolesOnEntry_shouldBeAccessibleToUnauthenticatedUser() throws Exception {
        String json = """
                [{"id":"emptyr-1","type":"emptyr-type","title":"EmptyRoles","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_emptyr.json"), json, StandardCharsets.UTF_8);

        List<DocumentationEntry> result = documentationService.getEntriesForRoles(List.of());

        assertTrue(result.stream().anyMatch(e -> "emptyr-1".equals(e.getId())));
    }

    @Test
    void refresh_shouldInvalidateCacheAndReloadEntries() throws Exception {
        String initialJson = """
                [{"id":"cref-1","type":"cref-type","title":"Initial","content":"{}","roles":[]}]
                """;
        Files.writeString(tempDir.resolve("documentation_cref.json"), initialJson, StandardCharsets.UTF_8);
        assertEquals(1, documentationService.getEntriesByType("cref-type").size());
        assertNotNull(ReflectionTestUtils.getField(documentationService, "cachedEntries"));

        Files.writeString(tempDir.resolve("documentation_cref.json"),
                """
                [{"id":"cref-2","type":"cref-type","title":"Updated","content":"{}","roles":[]}]
                """, StandardCharsets.UTF_8);
        documentationService.refresh();

        assertEquals("cref-2", documentationService.getEntriesByType("cref-type").getFirst().getId());
    }

    @Test
    void normalizeRoles_withAdministratorRole_shouldAliasToAdmin() throws Exception {
        String json = """
                [{"id":"alias-1","type":"alias-type","title":"Admin","content":"{}","roles":["ROLE_ADMIN"]}]
                """;
        Files.writeString(tempDir.resolve("documentation_alias.json"), json, StandardCharsets.UTF_8);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "pass", "ROLE_ADMINISTRATOR"));

        List<DocumentationEntry> result = documentationService.getEntriesByType("alias-type");

        assertEquals(1, result.size());
        assertEquals("alias-1", result.getFirst().getId());
    }
}
