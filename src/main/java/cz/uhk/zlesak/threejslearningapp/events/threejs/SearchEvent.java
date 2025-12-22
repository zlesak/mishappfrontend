package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import lombok.Getter;
import org.springframework.data.domain.Sort;

/**
 * Event fired when search is performed via the click of the button in FilterComponent.
 */
@Getter
public class SearchEvent extends ComponentEvent<UI> {
    private final String value;
    private final Sort.Direction sortDirection;
    private final String orderBy;

    public SearchEvent(String value, Sort.Direction sortDirection, String orderBy, UI source) {
        super(source, false);
        this.value = value;
        this.sortDirection = sortDirection;
        this.orderBy = orderBy;
    }
}
