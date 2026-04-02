package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.ComponentEvent;
import cz.uhk.zlesak.threejslearningapp.components.inputs.FilterComponent;
import lombok.Getter;
import org.springframework.data.domain.Sort;

/**
 * Event fired when search is performed via the click of the button in FilterComponent.
 */
@Getter
public class SearchEvent extends ComponentEvent<FilterComponent> {
    private final String value;
    private final Sort.Direction sortDirection;
    private final String orderBy;

    /**
     * @param value         Search text entered by the user.
     * @param sortDirection Requested sort direction.
     * @param orderBy       Field name to sort by.
     * @param source        The filter component firing the event.
     */
    public SearchEvent(String value, Sort.Direction sortDirection, String orderBy, FilterComponent source) {
        super(source, false);
        this.value = value;
        this.sortDirection = sortDirection;
        this.orderBy = orderBy;
    }
}
