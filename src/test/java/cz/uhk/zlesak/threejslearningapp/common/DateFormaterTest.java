package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateFormaterTest {
    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void formatDate_shouldUseSystemDefaultZoneAndExpectedPattern() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        String formatted = DateFormater.formatDate(Instant.parse("2024-01-02T03:04:00Z"));

        assertEquals("02.01.2024 03:04", formatted);
    }
}
