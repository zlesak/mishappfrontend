package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.HeadingForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ScrollToElement;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;

/**
 * HeadingListingSelect Class - A select component for listing headings within subchapters.
 */
public class HeadingListingSelect extends GenericSelect<HeadingForSelect, SubchapterInitEvent> {

    /**
     * Constructor for HeadingListingSelect.
     */
    public HeadingListingSelect() {
        super("headingSelect.caption", heading -> heading.name() != null ? heading.name() : "", HeadingForSelect.class, true);
    }

    /**
     * Creates a custom change event based on the value change event.
     * @param event the value change event
     * @return the created component event
     */
    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<HeadingForSelect> event) {
        return new ScrollToElement(UI.getCurrent(), event.getValue().id(), event.isFromClient());
    }

    /**
     * Handles the item addition ingoing change event action.
     * @param subchapterInitEvent event containing chapter contents for selects
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(SubchapterInitEvent subchapterInitEvent) {
        subchapterInitEvent.getChapterContentsForSelects().forEach((subChapterTuple, headingList) -> {
            String subchapterId = subChapterTuple.getLeft();
            for (var headingTuple : headingList) {
                String headingId = headingTuple._1();
                String name = headingTuple._2();
                HeadingForSelect heading = new HeadingForSelect(headingId, subchapterId, name);
                items.putMultiple(heading.primary() + heading.secondary(), heading);
            }
        });
    }
}
