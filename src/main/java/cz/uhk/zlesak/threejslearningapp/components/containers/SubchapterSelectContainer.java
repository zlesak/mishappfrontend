package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.HeadingListingSelect;
import cz.uhk.zlesak.threejslearningapp.components.inputs.selects.SubchapterListingSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Container component that holds subchapter and heading selection components.
 * It listens for events to initialize subchapter data and to show relevant content based on user selection
 */
@Scope("prototype")
@Getter
@Slf4j
public class SubchapterSelectContainer extends HorizontalLayout {
    protected final List<Registration> registrations = new ArrayList<>();

    SubchapterListingSelect subchapterListingSelect = new SubchapterListingSelect();
    HeadingListingSelect headingListingSelect = new HeadingListingSelect();

    /**
     * Constructor for SubchapterSelectContainer.
     */
    public SubchapterSelectContainer() {
        addClassName("subchapter-select-row");
        add(subchapterListingSelect, headingListingSelect);
        setWidthFull();
        setWrap(true);
    }

    /**
     * Overridden onAttach function to register event listeners when the component is attached.
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                SubchapterInitEvent.class, e -> {
                    subchapterListingSelect.handleItemAdditionIngoingChangeEventAction(e);
                    headingListingSelect.handleItemAdditionIngoingChangeEventAction(e);
                    subchapterListingSelect.showRelevantItemsBasedOnContext("","");
                }
        ));
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ShowSubchapterContentEvent.class, e -> headingListingSelect.showRelevantItemsBasedOnContext(e.getSubchapterId(), e.getSubchapterHeadingId())
        ));

    }

    /**
     * Overridden onDetach function to clean up event registrations when the component is detached.
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
