package cz.uhk.zlesak.threejslearningapp.domain.common;

/**
 * Interface defining methods for objects that have primary, secondary, and main item properties.
 */
public interface HasPrimarySecondaryMain {
    String primary();
    String secondary();
    boolean mainItem();
}
