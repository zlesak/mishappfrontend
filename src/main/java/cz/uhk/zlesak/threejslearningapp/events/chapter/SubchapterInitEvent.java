package cz.uhk.zlesak.threejslearningapp.events.chapter;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Triple;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;
import java.util.Map;

/**
 * Event to initialize subchapter with chapter contents for selects.
 */
@Getter
public class SubchapterInitEvent extends ComponentEvent<UI> {

    private final Map<Triple<String, String, String>, List<Tuple<String, String>>> chapterContentsForSelects;

    /**
     * Constructor for SubchapterInitEvent.
     * @param source the source UI component
     * @param chapterContentsForSelects map of chapter contents for selects
     * @param fromClient indicates if the event originated from the client side
     */
    public SubchapterInitEvent(UI source, Map<Triple<String, String, String>, List<Tuple<String, String>>> chapterContentsForSelects, boolean fromClient) {
        super(source, fromClient);
        this.chapterContentsForSelects = chapterContentsForSelects;
    }
}
