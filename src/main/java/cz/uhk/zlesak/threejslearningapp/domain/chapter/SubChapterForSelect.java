package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.HasPrimarySecondaryMain;

/**
 * SubChapterForComboBoxRecord - a record class representing a sub-chapter for a select component.
 * This class is used to encapsulate the ID and text of a sub-chapter, typically for UI components.
 *
 * @param id      the unique identifier of the sub-chapter
 * @param text    the display text of the sub-chapter
 * @param modelId the id of the model the subchapter belongs to
 */
public record SubChapterForSelect(String id, String text, String modelId) implements HasPrimarySecondaryMain {
    /**
     * @return returns the primary value, which is the sub-chapter ID.
     */
    @Override
    public String primary() {
        return id;
    }

    /**
     * @return returns the secondary value, which is an empty string.
     */
    @Override
    public String secondary() {
        return "";
    }

    /**
     * @return returns if this is a main item, which is always false in this context.
     */
    @Override
    public boolean mainItem() {
        return false;
    }

}

