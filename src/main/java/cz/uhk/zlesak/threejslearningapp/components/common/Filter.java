package cz.uhk.zlesak.threejslearningapp.components.common;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.inputs.textFields.SearchTextField;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FilterComponent provides filtering functionality with a search field and a search button.
 */
public class Filter extends HorizontalLayout implements I18nAware {
    @Getter
    private final SearchTextField searchField;
    private final Select<Sort.Direction> searchDirectionSelect;
    private final Select<String> orderBySelect;
    private final Button createButton;


    /**
     * Constructor for FilterComponent.
     * Initializes the filter component with a search field and a search button.
     */
    public Filter() {
        super();

        setWidthFull();
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.START);

        this.searchField = createSearchField();
        this.searchDirectionSelect = getSortDirectionSelect();
        this.orderBySelect = getOrderBySelect();
        this.createButton = getSearchButton();

        add(orderBySelect, searchDirectionSelect, searchField, createButton);
    }

    /**
     * Creates and configures the search text field.
     *
     * @return the configured search text field
     */
    private SearchTextField createSearchField() {
        SearchTextField searchField = new SearchTextField(text("filter.search.placeholder"));
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(event -> {
            if (event.getValue() != null && !event.getValue().isEmpty()) {
                orderBySelect.setEnabled(false);
                searchDirectionSelect.setEnabled(false);
                createButton.setEnabled(true);

            } else {
                orderBySelect.setEnabled(true);
                searchDirectionSelect.setEnabled(true);
                createButton.setEnabled(false);
                ComponentUtil.fireEvent(UI.getCurrent(), new SearchEvent(searchField.getValue(), searchDirectionSelect.getValue(), orderBySelect.getValue(), UI.getCurrent()));
            }
        });
        return searchField;
    }

    /**
     * Creates and configures the search button.
     *
     * @return the configured search button
     */
    private Button getSearchButton() {
        Button searchButton = new Button(text("button.search"));
        searchButton.addClickListener(e ->
                ComponentUtil.fireEvent(UI.getCurrent(), new SearchEvent(searchField.getValue(), searchDirectionSelect.getValue(), orderBySelect.getValue(), UI.getCurrent())));
        searchButton.setEnabled(false);
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
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
        select.setHelperText(text("filter.orderBy.label"));
        select.setItemLabelGenerator(name -> text("filter." + name.toLowerCase() + ".label"));
        select.addValueChangeListener(event ->
                ComponentUtil.fireEvent(UI.getCurrent(), new SearchEvent(searchField.getValue(), searchDirectionSelect.getValue(), orderBySelect.getValue(), UI.getCurrent())));
        return select;
    }

    /**
     * Creates and configures a Select component for sorting direction.
     *
     * @return the configured Select component for sorting direction
     */
    private Select<Sort.Direction> getSortDirectionSelect() {
        Select<Sort.Direction> sortDirectionSelect = new Select<>();
        sortDirectionSelect.setItems(Sort.Direction.values());
        sortDirectionSelect.setValue(Sort.Direction.ASC);
        sortDirectionSelect.setHelperText(text("filter.sort.label"));
        sortDirectionSelect.setItemLabelGenerator(direction -> switch (direction) {
            case ASC -> text("filter.sort.direction.asc");
            case DESC -> text("filter.sort.direction.desc");
        });
        sortDirectionSelect.addValueChangeListener(event ->
                ComponentUtil.fireEvent(UI.getCurrent(), new SearchEvent(searchField.getValue(), searchDirectionSelect.getValue(), orderBySelect.getValue(), UI.getCurrent())));
        return sortDirectionSelect;
    }

    /**
     * Extracts field names from the given class (i.e.Entity class) and its superclasses.
     * Only includes fields of allowed types and excludes synthetic fields and fields named "id".
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
}

