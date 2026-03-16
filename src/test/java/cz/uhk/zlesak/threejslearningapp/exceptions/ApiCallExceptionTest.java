package cz.uhk.zlesak.threejslearningapp.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiCallExceptionTest {

    @Test
    void shouldExposeMetadataAndReadableToString() {
        IllegalStateException cause = new IllegalStateException("boom");
        ApiCallException exception = new ApiCallException(
                "Request failed",
                "chapter-7",
                "/api/chapter/7",
                HttpStatus.BAD_REQUEST,
                "{\"error\":\"invalid\"}",
                cause
        );

        assertEquals("Request failed", exception.getMessage());
        assertEquals("chapter-7", exception.getChapterId());
        assertEquals("/api/chapter/7", exception.getRequest());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("{\"error\":\"invalid\"}", exception.getResponseBody());
        assertSame(cause, exception.getCause());
        assertTrue(exception.toString().contains("chapter-7"));
        assertTrue(exception.toString().contains("/api/chapter/7"));
    }
}
