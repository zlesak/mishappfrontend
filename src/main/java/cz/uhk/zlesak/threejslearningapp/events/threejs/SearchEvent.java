package cz.uhk.zlesak.threejslearningapp.events.threejs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.time.Instant;

/**
 * Event fired when the user asks a listing to search, filter or re-sort.
 *
 * <p>The source is a plain {@link Component}: no listener ever looked at the source's type — they all
 * read the values below.
 */
@Getter
public class SearchEvent extends ComponentEvent<Component> {
    private final String value;
    private final Sort.Direction sortDirection;
    private final String orderBy;
    /** Author to narrow to, matched on the recorded name; {@code null} means every author. */
    private final String creatorName;
    /** Earliest creation date to include; {@code null} means no lower bound. */
    private final Instant createdFrom;
    /** Latest creation date to include; {@code null} means no upper bound. */
    private final Instant createdTo;
    /** Name of a model the entity has to contain; only chapters have models, elsewhere {@code null}. */
    private final String modelName;

    /**
     * @param value         Search text entered by the user.
     * @param sortDirection Requested sort direction.
     * @param orderBy       Field name to sort by.
     * @param source        The toolbar firing the event.
     */
    public SearchEvent(String value, Sort.Direction sortDirection, String orderBy, Component source) {
        this(value, sortDirection, orderBy, source, null, null, null, null);
    }

    /**
     * @param value         Search text entered by the user.
     * @param sortDirection Requested sort direction.
     * @param orderBy       Field name to sort by.
     * @param source        The toolbar firing the event.
     * @param creatorName   Author to narrow to, or {@code null}.
     * @param createdFrom   Earliest creation date, or {@code null}.
     * @param createdTo     Latest creation date, or {@code null}.
     * @param modelName     Model the entity has to contain, or {@code null}.
     */
    @Builder
    public SearchEvent(String value, Sort.Direction sortDirection, String orderBy, Component source,
                       String creatorName, Instant createdFrom, Instant createdTo, String modelName) {
        super(source, false);
        this.value = value;
        this.sortDirection = sortDirection;
        this.orderBy = orderBy;
        this.creatorName = creatorName;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.modelName = modelName;
    }
}
