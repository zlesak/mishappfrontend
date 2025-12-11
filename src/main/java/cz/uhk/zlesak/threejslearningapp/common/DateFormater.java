package cz.uhk.zlesak.threejslearningapp.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateFormater {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public static String formatDate(Instant instant) {
        return DATE_FORMATTER.format(instant);
    }
}
