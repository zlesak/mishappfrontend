package cz.uhk.zlesak.threejslearningapp.components.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;

/**
 * SubchapterListingSelect Class - A select component for listing subchapters.
 */
public class SubchapterListingSelect extends GenericSelect<SubChapterForSelect, SubchapterInitEvent> {

    /**
     * Constructor for SubchapterListingSelect.
     */
    public SubchapterListingSelect() {
        super("subchapterSelect.caption", heading -> heading.text() != null ? heading.text() : "", SubChapterForSelect.class, true);

    }

    /**
     * Creates a custom change event based on the value change event.
     *
     * @param event the value change event
     * @return the created component event
     */
    @Override
    protected ComponentEvent<?> createChangeEvent(ValueChangeEvent<SubChapterForSelect> event) {
        String modelId = event.getValue() != null ? event.getValue().modelId() : "main";
        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, "main", ThreeJsActions.SHOW_MODEL, true, null));
        return new ShowSubchapterContentEvent(UI.getCurrent(), event.getValue() != null ? event.getValue().id() : null, null, event.isFromClient());
    }

    /**
     * Handles the item addition ingoing change event action.
     *
     * @param subchapterInitEvent containing chapter contents for selects
     */
    @Override
    public void handleItemAdditionIngoingChangeEventAction(SubchapterInitEvent subchapterInitEvent) {
        subchapterInitEvent.getChapterContentsForSelects().keySet().forEach(tuple -> {
            SubChapterForSelect subchapter = new SubChapterForSelect(tuple.getLeft(), tuple.getMiddle(), tuple.getRight());
            items.putMultiple(subchapter.primary() + subchapter.secondary(), subchapter);
        });
    }
}
