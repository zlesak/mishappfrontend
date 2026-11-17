package cz.uhk.zlesak.threejslearningapp.components.inputs;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The search, sort and filter controls above an entity listing.
 *
 * <p>Laid out by importance: the search field is what people reach for, so it comes first and takes the
 * room; what to sort by comes next; the direction is a single icon rather than a select box the width of
 * a sentence, because it has exactly two states.
 *
 * <p>Every control has a real label. Both selects used to describe themselves with
 * {@code setHelperText}, which renders below the field and is not the field's accessible name, and the
 * search field had only a placeholder — text that disappears the moment the user types. The placeholder
 * stays, as an example of what to enter.
 *
 * <p>Behind "Další filtry" sit the narrower questions: who wrote it, when, and — for chapters — which
 * model it contains. They are folded away because most searches do not need them, and a row of six
 * controls is harder to use than a row of three.
 */
public class ListingToolbar extends HorizontalLayout implements I18nAware {

    @Getter
    private final SearchTextField searchField;
    private final Select<String> orderBySelect;
    private final Button directionButton;
    private final Button createButton;
    private final Button advancedToggle;
    private final HorizontalLayout advancedFilters;
    private final TextField creatorField;
    private final DatePicker createdFromPicker;
    private final DatePicker createdToPicker;
    private final TextField modelNameField;
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private boolean advancedVisible = false;
    /** The browser's zone once it is known; the server's until then. See {@link #onAttach}. */
    private ZoneId viewerZone = ZoneId.systemDefault();

    /**
     * Constructs the toolbar without the chapter-only model filter.
     */
    public ListingToolbar() {
        this(false);
    }

    /**
     * Constructs the toolbar.
     *
     * @param withModelFilter whether to offer filtering by a contained model, which only chapters have
     */
    public ListingToolbar(boolean withModelFilter) {
        super();

        setWidthFull();
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.END);
        setWrap(true);
        addClassName("app-filter");
        getStyle().set("gap", "var(--lumo-space-s)");

        this.searchField = createSearchField();
        this.orderBySelect = getOrderBySelect();
        this.directionButton = createDirectionButton();
        this.createButton = getSearchButton();
        this.creatorField = createCreatorField();
        this.createdFromPicker = createDatePicker("filter.createdFrom.label");
        this.createdToPicker = createDatePicker("filter.createdTo.label");
        this.modelNameField = createModelNameField();
        this.advancedFilters = createAdvancedFilters(withModelFilter);
        this.advancedToggle = createAdvancedToggle();

        searchField.addClassName("app-filter-search");
        orderBySelect.addClassName("app-filter-order");
        createButton.addClassName("app-filter-submit");

        searchField.getStyle().set("flex", "3 1 18rem");
        orderBySelect.getStyle().set("flex", "1 1 12rem");

        HorizontalLayout primaryRow = new HorizontalLayout(searchField, createButton, orderBySelect, directionButton, advancedToggle);
        primaryRow.setWidthFull();
        primaryRow.setPadding(false);
        primaryRow.setWrap(true);
        primaryRow.setAlignItems(FlexComponent.Alignment.END);
        primaryRow.getStyle().set("gap", "var(--lumo-space-s)");
        primaryRow.expand(searchField);

        // The toolbar is two stacked rows, so the primary controls stay on one line and the optional
        // filters appear beneath them rather than pushing them apart.
        getStyle().set("flex-direction", "column").set("align-items", "stretch");
        add(primaryRow, advancedFilters);
    }

    /**
     * Creates and configures the search text field.
     *
     * @return the configured search text field
     */
    private SearchTextField createSearchField() {
        SearchTextField searchField = new SearchTextField("filter.search.placeholder");
        searchField.setLabel(text("filter.search.label"));
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.addValueChangeListener(event -> {
            boolean hasQuery = event.getValue() != null && !event.getValue().isEmpty();
            orderBySelect.setEnabled(!hasQuery);
            directionButton.setEnabled(!hasQuery);
            createButton.setEnabled(hasQuery);
            if (!hasQuery) {
                fireSearch();
            }
        });
        return searchField;
    }

    /**
     * The sort direction as one button with two states.
     *
     * <p>It was a select as wide as the field beside it, offering "Vzestupně" and "Sestupně" — two
     * options in a control built for many. The arrow says which way it is sorted; the accessible name
     * and {@code aria-pressed} say the same thing in words, because an arrow on its own does not.
     *
     * @return the toggle
     */
    private Button createDirectionButton() {
        Button button = new Button(VaadinIcon.ARROW_DOWN.create());
        button.addClassName("app-filter-direction");
        button.getStyle().set("min-width", "44px").set("min-height", "44px").set("flex", "0 0 auto");
        button.addClickListener(e -> {
            sortDirection = sortDirection == Sort.Direction.ASC ? Sort.Direction.DESC : Sort.Direction.ASC;
            applyDirectionState(button);
            fireSearch();
        });
        applyDirectionState(button);
        return button;
    }

    /**
     * Puts the arrow, the name and the pressed state in step with the current direction. Takes the
     * button as an argument because it also runs while the field is still being assigned.
     *
     * @param button the direction toggle
     */
    private void applyDirectionState(Button button) {
        boolean ascending = sortDirection == Sort.Direction.ASC;
        button.setIcon(ascending ? VaadinIcon.ARROW_DOWN.create() : VaadinIcon.ARROW_UP.create());
        String label = ascending ? text("filter.sort.direction.asc") : text("filter.sort.direction.desc");
        button.setAriaLabel(text("filter.sort.label") + ": " + label);
        button.setTooltipText(text("filter.sort.label") + ": " + label);
        button.getElement().setAttribute("aria-pressed", String.valueOf(!ascending));
    }

    private Button createAdvancedToggle() {
        Button toggle = new Button(text("filter.advanced.show"), VaadinIcon.ANGLE_DOWN.create());
        toggle.addClassName("app-filter-advanced-toggle");
        toggle.getStyle().set("min-height", "44px").set("flex", "0 0 auto");
        toggle.getElement().setAttribute("aria-expanded", "false");
        toggle.addClickListener(e -> {
            advancedVisible = !advancedVisible;
            advancedFilters.setVisible(advancedVisible);
            toggle.setText(advancedVisible ? text("filter.advanced.hide") : text("filter.advanced.show"));
            toggle.setIcon(advancedVisible ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
            toggle.getElement().setAttribute("aria-expanded", String.valueOf(advancedVisible));
        });
        return toggle;
    }

    private HorizontalLayout createAdvancedFilters(boolean withModelFilter) {
        modelNameField.setVisible(withModelFilter);

        HorizontalLayout row = new HorizontalLayout(creatorField, createdFromPicker, createdToPicker, modelNameField);
        row.addClassName("app-filter-advanced");
        row.setWidthFull();
        row.setPadding(false);
        row.setWrap(true);
        row.setAlignItems(FlexComponent.Alignment.END);
        row.getStyle().set("gap", "var(--lumo-space-s)");
        row.setVisible(false);
        return row;
    }

    private TextField createCreatorField() {
        TextField field = new TextField(text("filter.creator.label"));
        field.setPlaceholder(text("filter.creator.placeholder"));
        field.setClearButtonVisible(true);
        field.getStyle().set("flex", "1 1 12rem");
        field.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                fireSearch();
            }
        });
        return field;
    }

    private TextField createModelNameField() {
        TextField field = new TextField(text("filter.model.label"));
        field.setPlaceholder(text("filter.model.placeholder"));
        field.setClearButtonVisible(true);
        field.getStyle().set("flex", "1 1 12rem");
        field.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                fireSearch();
            }
        });
        return field;
    }

    private DatePicker createDatePicker(String labelKey) {
        DatePicker picker = new DatePicker(text(labelKey));
        picker.setClearButtonVisible(true);
        picker.getStyle().set("flex", "1 1 10rem");
        picker.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                fireSearch();
            }
        });
        return picker;
    }

    /**
     * Creates and configures the search button.
     *
     * @return the configured search button
     */
    private Button getSearchButton() {
        Button searchButton = new Button(text("button.search"));
        searchButton.addClickListener(e -> fireSearch());
        searchButton.setEnabled(false);
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.getStyle().set("min-height", "44px").set("flex", "0 0 auto");
        return searchButton;
    }

    /**
     * Creates and configures a Select component for ordering by field names.
     *
     * @return the configured Select component for ordering by field names
     */
    private Select<String> getOrderBySelect() {
        Select<String> select = new Select<>();
        List<String> fieldNames = extractFieldNames(AbstractEntity.class);
        select.setItems(fieldNames);
        if (!fieldNames.isEmpty()) {
            select.setValue(fieldNames.getFirst());
        }
        select.setLabel(text("filter.orderBy.label"));
        select.setItemLabelGenerator(name -> text("filter." + name.toLowerCase() + ".label"));
        select.addValueChangeListener(event -> fireSearch());
        return select;
    }

    /**
     * Fires the event carrying everything the toolbar currently asks for.
     */
    private void fireSearch() {
        ComponentUtil.fireEvent(this, SearchEvent.builder()
                .value(searchField.getValue())
                .sortDirection(sortDirection)
                .orderBy(orderBySelect.getValue())
                .source(this)
                .creatorName(trimmedOrNull(creatorField.getValue()))
                .createdFrom(startOfDay(createdFromPicker.getValue()))
                .createdTo(endOfDay(createdToPicker.getValue()))
                .modelName(modelNameField.isVisible() ? trimmedOrNull(modelNameField.getValue()) : null)
                .build());
    }

    private static String trimmedOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** The whole of the chosen day counts, so "from" starts at midnight in the viewer's own zone. */
    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(viewerZone).toInstant();
    }

    /** And "to" runs to the last moment of that day, not to its beginning. */
    private Instant endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX).atZone(viewerZone).toInstant();
    }

    /**
     * Picks up the browser's time zone, so a chosen day means that day where the user is.
     *
     * <p>The JVM's own zone is the server's -- UTC in the container -- which quietly dropped the
     * early hours of a day for anyone east of it: an item created at 00:30 in Prague is still the
     * previous day in UTC, so filtering "created from today" lost it. Vaadin caches the client
     * details after the first round trip, so the zone is known well before anyone has finished
     * picking a date; until then the server's zone stands in, as it did before.
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            String timeZoneId = details.getTimeZoneId();
            if (timeZoneId != null && ZoneId.getAvailableZoneIds().contains(timeZoneId)) {
                viewerZone = ZoneId.of(timeZoneId);
            }
        });
    }

    /**
     * Extracts field names from the given class (i.e.Entity class) and its superclasses.
     * Only includes fields of allowed types and excludes synthetic fields and fields named "textureId".
     *
     * @param clazz the class to extract field names from
     * @return a list of field names
     */
    private List<String> extractFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                String name = field.getName();
                if (field.isSynthetic() || !visited.add(name) || name.toLowerCase().contains("id")) {
                    continue;
                }
                boolean allowed = allowedTypedCheck(field);
                if (allowed) {
                    names.add(name);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return names;
    }

    /**
     * Checks if the field type is allowed for filtering.
     *
     * @param field the field to check
     * @return true if the field type is allowed, false otherwise
     */
    private static boolean allowedTypedCheck(Field field) {
        Class<?> type = field.getType();

        return type == String.class ||
                type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class ||
                type == Boolean.class || type == boolean.class ||
                type == Instant.class;
    }

    /**
     * Sets the value of the search field.
     *
     * @param value the value to set in the search field
     */
    public void setSearchFieldValue(String value) {
        this.searchField.setValue(value);
    }

    /**
     * Shows or hides the "contains model" filter. Only chapters contain models, so every other listing
     * leaves it out rather than offering a filter that can never match.
     *
     * @param visible whether to offer the filter
     */
    public void setModelFilterVisible(boolean visible) {
        modelNameField.setVisible(visible);
        if (!visible) {
            modelNameField.clear();
        }
    }
}
