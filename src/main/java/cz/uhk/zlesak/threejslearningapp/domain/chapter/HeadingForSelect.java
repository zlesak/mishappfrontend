package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondaryMain;

/**
 * HeadingForSelect Record - Represents a heading item for selection within a subchapter.
 *
 * @param id           heading block id
 * @param subchapterId subchapter block id
 * @param name         heading name
 * @see HasPrimarySecondaryMain for primary/secondary/main item structure
 */
public record HeadingForSelect(String id, String subchapterId, String name) implements HasPrimarySecondaryMain {
    /**
     * @return returns the primary identifier (subchapterId)
     */
    @Override
    public String primary() {
        return subchapterId;
    }

    /**
     * @return returns the secondary identifier (id)
     */
    @Override
    public String secondary() {
        return id;
    }

    /**
     * @return returns false as headings are not main items
     */
    @Override
    public boolean mainItem() {
        return false;
    }
}
