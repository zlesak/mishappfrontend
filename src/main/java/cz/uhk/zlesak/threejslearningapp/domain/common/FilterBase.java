package cz.uhk.zlesak.threejslearningapp.domain.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Base class for filters used in various entities.
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FilterBase {
    String Name;
    String CreatorId;
    Instant CreatedFrom;
    Instant CreatedTo;
}
