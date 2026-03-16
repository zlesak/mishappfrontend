package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class InputStreamMultipartFileTest {

    @Test
    void constructor_shouldReadBytesAndUseDisplayNameFallback() throws Exception {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                .fileName("hello.txt")
                .displayName(null)
                .build();

        assertEquals("hello.txt", file.getName());
        assertEquals("hello.txt", file.getDisplayName());
        assertEquals("hello.txt", file.getOriginalFilename());
        assertEquals(5, file.getSize());
        assertFalse(file.isEmpty());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), file.getBytes());
        assertEquals("hello", new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        file.setDisplayName("Readable");
        assertEquals("Readable", file.getDisplayName());
        file.setDisplayName(null);
        assertEquals("hello.txt", file.getDisplayName());
    }

    @Test
    void constructor_shouldHandleNullInputStream() {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(null)
                .fileName("empty.bin")
                .displayName("Empty")
                .build();

        assertTrue(file.isEmpty());
        assertEquals(0, file.getSize());
    }
}
