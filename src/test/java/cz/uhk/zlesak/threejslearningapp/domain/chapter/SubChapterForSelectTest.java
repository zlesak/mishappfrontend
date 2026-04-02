package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SubChapterForSelectTest {

    @Test
    void recordAccessors_shouldReturnCorrectValues() {
        SubChapterForSelect record = new SubChapterForSelect("id-1", "Sub Chapter", "model-1");

        assertEquals("id-1", record.id());
        assertEquals("Sub Chapter", record.text());
        assertEquals("model-1", record.modelId());
    }

    @Test
    void primary_shouldReturnId() {
        SubChapterForSelect record = new SubChapterForSelect("sub-42", "Name", "model-x");

        assertEquals("sub-42", record.primary());
    }

    @Test
    void secondary_shouldReturnEmptyString() {
        SubChapterForSelect record = new SubChapterForSelect("sub-1", "Name", "model-1");

        assertEquals("", record.secondary());
    }

    @Test
    void mainItem_shouldReturnFalse() {
        SubChapterForSelect record = new SubChapterForSelect("sub-1", "Name", "model-1");

        assertFalse(record.mainItem());
    }
}
