package cz.uhk.zlesak.threejslearningapp.domain.common;

/**
 * Interface defining methods for objects that have primary, secondary, and main item properties.
 * primary() - Returns the primary item as a String for filtering in keys of maps.
 * secondary() - Returns the secondary item as a String as s String for specific item.
 * mainItem() - Returns a boolean indicating if it is the main item as boolean for default value select as setValue.
 */
public interface HasPrimarySecondaryMain {
    String primary();
    String secondary();
    boolean mainItem();
}
