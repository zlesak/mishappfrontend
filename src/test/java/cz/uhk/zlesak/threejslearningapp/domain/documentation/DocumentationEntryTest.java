package cz.uhk.zlesak.threejslearningapp.domain.documentation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationEntryTest {

    @Test
    void toIndex_shouldReturnEmptyRoles_whenRolesIsNull() {
        DocumentationEntry entry = new DocumentationEntry("id-1", "chapter", "Title", "{}", null);

        DocumentationEntryIndex index = entry.toIndex();

        assertEquals("id-1", index.getId());
        assertEquals("chapter", index.getType());
        assertEquals("Title", index.getTitle());
        assertTrue(index.getRoles().isEmpty());
    }

    @Test
    void toIndex_shouldCopyRoles_whenRolesIsNonNull() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");
        DocumentationEntry entry = new DocumentationEntry("id-2", "quiz", "Quiz Title", "{}", roles);

        DocumentationEntryIndex index = entry.toIndex();

        assertEquals("id-2", index.getId());
        assertEquals(2, index.getRoles().size());
        assertTrue(index.getRoles().contains("ROLE_ADMIN"));
        assertTrue(index.getRoles().contains("ROLE_USER"));
    }

    @Test
    void toIndex_shouldReturnEmptyRoles_whenRolesIsEmptyList() {
        DocumentationEntry entry = new DocumentationEntry("id-3", "model", "Model", "{}", List.of());

        DocumentationEntryIndex index = entry.toIndex();

        assertTrue(index.getRoles().isEmpty());
    }
}
