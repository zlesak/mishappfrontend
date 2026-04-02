package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HeadingForSelectTest {

    @Test
    void recordAccessors_shouldReturnCorrectValues() {
        HeadingForSelect record = new HeadingForSelect("h-1", "sub-1", "Introduction");

        assertEquals("h-1", record.id());
        assertEquals("sub-1", record.subchapterId());
        assertEquals("Introduction", record.name());
    }

    @Test
    void primary_shouldReturnSubchapterId() {
        HeadingForSelect record = new HeadingForSelect("h-2", "sub-99", "Overview");

        assertEquals("sub-99", record.primary());
    }

    @Test
    void secondary_shouldReturnId() {
        HeadingForSelect record = new HeadingForSelect("h-3", "sub-1", "Details");

        assertEquals("h-3", record.secondary());
    }

    @Test
    void mainItem_shouldReturnFalse() {
        HeadingForSelect record = new HeadingForSelect("h-4", "sub-1", "Summary");

        assertFalse(record.mainItem());
    }
}

