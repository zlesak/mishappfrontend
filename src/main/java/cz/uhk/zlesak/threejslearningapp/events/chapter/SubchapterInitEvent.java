package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import lombok.Getter;

import java.util.List;

/**
 * SubchapterInitEvent represents an event triggered when a subchapter is initialized.
 * It extends ComponentEvent and includes information about the subchapter ID, text, and model ID
 */
@Getter
public class SubchapterInitEvent extends ComponentEvent<UI> {

    private  final List<SubChapterForSelect> subChapterForSelectList;

    /**
     * Constructor for SubchapterInitEvent.
     * @param source the UI source of the event
     * @param subChapterForSelectList the list of SubChapterForSelect objects associated with the event
     * @param fromClient indicates if the event originated from the client side
     */
    public SubchapterInitEvent(UI source, List<SubChapterForSelect> subChapterForSelectList, boolean fromClient) {
        super(source, fromClient);
        this.subChapterForSelectList = subChapterForSelectList;
    }
}
