package cz.uhk.zlesak.threejslearningapp.components.inputs;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FilterComponentTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void searchFieldChangesShouldToggleControlsAndFireResetSearchEvent() {
        FilterComponent component = new FilterComponent();
        UI.getCurrent().add(component);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(component, SearchEvent.class, fired::set);

        component.getSearchField().setValue("atlas");

        assertFalse(orderBySelect(component).isEnabled());
        assertFalse(directionSelect(component).isEnabled());
        assertTrue(searchButton(component).isEnabled());

        component.getSearchField().clear();

        assertTrue(orderBySelect(component).isEnabled());
        assertTrue(directionSelect(component).isEnabled());
        assertFalse(searchButton(component).isEnabled());
        assertEquals("", fired.get().getValue());
    }

    @Test
    void searchButtonAndSelectsShouldEmitSearchEvent() {
        FilterComponent component = new FilterComponent();
        UI.getCurrent().add(component);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(component, SearchEvent.class, fired::set);

        component.getSearchField().setValue("bone");
        directionSelect(component).setValue(Sort.Direction.DESC);
        orderBySelect(component).setValue("Created");
        searchButton(component).click();

        assertEquals("bone", fired.get().getValue());
        assertEquals(Sort.Direction.DESC, fired.get().getSortDirection());
        assertEquals("Created", fired.get().getOrderBy());
    }

    @Test
    void setSearchFieldValueAndFieldExtractionShouldUseAllowedNonIdFields() throws Exception {
        FilterComponent component = new FilterComponent();

        component.setSearchFieldValue("lebka");

        assertEquals("lebka", component.getSearchField().getValue());

        Method method = FilterComponent.class.getDeclaredMethod("extractFieldNames", Class.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) method.invoke(component, cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity.class);

        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("created"));
        assertTrue(fields.contains("updated"));
        assertTrue(fields.contains("description"));
        assertFalse(fields.contains("id"));
        assertFalse(fields.contains("creatorId"));
    }

    @SuppressWarnings("unchecked")
    private Select<String> orderBySelect(FilterComponent component) {
        try {
            var field = FilterComponent.class.getDeclaredField("orderBySelect");
            field.setAccessible(true);
            return (Select<String>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Select<Sort.Direction> directionSelect(FilterComponent component) {
        try {
            var field = FilterComponent.class.getDeclaredField("searchDirectionSelect");
            field.setAccessible(true);
            return (Select<Sort.Direction>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Button searchButton(FilterComponent component) {
        try {
            var field = FilterComponent.class.getDeclaredField("createButton");
            field.setAccessible(true);
            return (Button) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

