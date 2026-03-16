package cz.uhk.zlesak.threejslearningapp.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * DateFormater Class - Provides utility methods for formatting dates
 */
public class DateFormater {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Formats an Instant to a string representation.
     *
     * @param instant the Instant to format
     * @return the formatted date string
     */
    public static String formatDate(Instant instant) {
        return DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }
}
