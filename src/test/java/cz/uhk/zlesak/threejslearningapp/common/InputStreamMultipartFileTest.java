package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
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
    void getContentType_shouldReturnApplicationOctetStream() {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
                .fileName("data.bin")
                .build();

        assertEquals("application/octet-stream", file.getContentType());
    }

    @Test
    void transferTo_shouldThrowUnsupportedOperationException() {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .fileName("file.bin")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> file.transferTo(new File("dest.bin")));
    }

    @Test
    void getBytes_shouldReturnDefensiveCopy() {
        byte[] original = "copy".getBytes(StandardCharsets.UTF_8);
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(new ByteArrayInputStream(original))
                .fileName("copy.txt")
                .build();

        byte[] first = file.getBytes();
        first[0] = (byte) 0xFF;
        byte[] second = file.getBytes();

        assertEquals('c', (char) second[0]);
    }

    @Test
    void getInputStream_shouldReturnFreshStreamForEachCall() throws Exception {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8)))
                .fileName("stream.txt")
                .build();

        InputStream s1 = file.getInputStream();
        s1.readAllBytes();
        InputStream s2 = file.getInputStream();

        assertEquals("stream", new String(s2.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void constructor_withBothFileNameAndDisplayNameNull_shouldUseEmptyDisplayName() {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(null)
                .fileName(null)
                .displayName(null)
                .build();

        assertEquals("", file.getDisplayName());
        assertEquals("", file.getName());
    }

    @Test
    void setDisplayName_withNonNullValue_shouldUpdateDisplayName() {
        InputStreamMultipartFile file = InputStreamMultipartFile.builder()
                .inputStream(null)
                .fileName("test.txt")
                .displayName("Old Name")
                .build();

        file.setDisplayName("New Name");

        assertEquals("New Name", file.getDisplayName());
    }
}

