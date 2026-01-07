package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.*;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubChapterChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * ChapterSelect is a specialized GenericSelect component for selecting sub-chapters.
 * It handles the initialization and change events related to sub-chapters.
 */
@Scope("prototype")
public class ChapterSelect extends GenericSelect<SubChapterForSelect, SubchapterInitEvent> {

    protected final List<Registration> registrations = new ArrayList<>();

    /**
     * Constructor for ChapterSelect.
     */
    public ChapterSelect() {
        super("chapterSelect.caption", chapter -> chapter.text() != null ? chapter.text() : "", SubChapterForSelect.class, true);
    }

    /**
     * Creates a change event for sub-chapter selection changes.
     *
     * @param event the value change event containing old and new values
     * @return a SubChapterChangeEvent representing the change
     */
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<SubChapterForSelect> event) {
        return new SubChapterChangeEvent(UI.getCurrent(), event.getOldValue(), event.getValue(), event.isFromClient());
    }

    /**
     * Handles the addition of items in response to a SubchapterInitEvent.
     *
     * @param subchapterInitEvent event with items to be added to the select component
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(SubchapterInitEvent subchapterInitEvent) {
        for (SubChapterForSelect record : subchapterInitEvent.getSubChapterForSelectList()) {
            items.putMultiple(record.id(), record);
        }
        items.notifyChange(null, false);
        showRelevantItemsBasedOnContext("", "");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                SubchapterInitEvent.class, this::handleItemAdditionIngoingChangeEventAction
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
