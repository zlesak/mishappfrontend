package cz.uhk.zlesak.threejslearningapp.components.inputs;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingToolbarTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void everyControlHasAVisibleLabel() {
        // The selects described themselves with setHelperText, which renders below the field and is not
        // the field's accessible name, and the search field had only a placeholder — which vanishes as
        // soon as the user types.
        ListingToolbar toolbar = new ListingToolbar();

        assertEquals("Hledat výraz", toolbar.getSearchField().getLabel());
        assertEquals("Řadit podle", orderBySelect(toolbar).getLabel());
        assertTrue(orderBySelect(toolbar).getHelperText() == null || orderBySelect(toolbar).getHelperText().isBlank());
        // The direction is an icon, so its name has to be spelled out for it.
        assertEquals("Směr řazení: Vzestupně", directionButton(toolbar).getAriaLabel().orElseThrow());
    }

    @Test
    void thePlaceholderSurvivesAsAnExampleNotAsTheLabel() {
        ListingToolbar toolbar = new ListingToolbar();

        assertEquals("např. mozek", toolbar.getSearchField().getPlaceholder());
        assertFalse(toolbar.getSearchField().getLabel().isBlank());
    }

    @Test
    void theDirectionIsOneButtonWithTwoStates() {
        // It was a select as wide as the field beside it, offering exactly two options.
        ListingToolbar toolbar = new ListingToolbar();
        UI.getCurrent().add(toolbar);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(toolbar, SearchEvent.class, fired::set);

        Button direction = directionButton(toolbar);
        assertEquals("false", direction.getElement().getAttribute("aria-pressed"));

        direction.click();

        assertEquals(Sort.Direction.DESC, fired.get().getSortDirection());
        assertEquals("true", direction.getElement().getAttribute("aria-pressed"));
        assertEquals("Směr řazení: Sestupně", direction.getAriaLabel().orElseThrow());

        direction.click();
        assertEquals(Sort.Direction.ASC, fired.get().getSortDirection());
    }

    @Test
    void searchFieldChangesShouldToggleControlsAndFireResetSearchEvent() {
        ListingToolbar toolbar = new ListingToolbar();
        UI.getCurrent().add(toolbar);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(toolbar, SearchEvent.class, fired::set);

        toolbar.getSearchField().setValue("atlas");

        assertFalse(orderBySelect(toolbar).isEnabled());
        assertFalse(directionButton(toolbar).isEnabled());
        assertTrue(searchButton(toolbar).isEnabled());

        toolbar.getSearchField().clear();

        assertTrue(orderBySelect(toolbar).isEnabled());
        assertTrue(directionButton(toolbar).isEnabled());
        assertFalse(searchButton(toolbar).isEnabled());
        assertEquals("", fired.get().getValue());
    }

    @Test
    void searchButtonAndSelectsShouldEmitSearchEvent() {
        ListingToolbar toolbar = new ListingToolbar();
        UI.getCurrent().add(toolbar);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(toolbar, SearchEvent.class, fired::set);

        toolbar.getSearchField().setValue("bone");
        orderBySelect(toolbar).setValue("Created");
        searchButton(toolbar).click();

        assertEquals("bone", fired.get().getValue());
        assertEquals("Created", fired.get().getOrderBy());
    }

    @Test
    void theAuthorAndDateFiltersTravelWithTheEvent() {
        ListingToolbar toolbar = new ListingToolbar();
        UI.getCurrent().add(toolbar);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(toolbar, SearchEvent.class, fired::set);

        field(toolbar, "creatorField").setValue("novakj");
        datePicker(toolbar, "createdFromPicker").setValue(LocalDate.of(2026, 3, 1));
        datePicker(toolbar, "createdToPicker").setValue(LocalDate.of(2026, 3, 31));
        searchButton(toolbar).setEnabled(true);
        searchButton(toolbar).click();

        SearchEvent event = fired.get();
        assertEquals("novakj", event.getCreatorName());
        assertNotNull(event.getCreatedFrom());
        assertNotNull(event.getCreatedTo());
        // The whole of the closing day counts, so "to" is later than "from" by more than the calendar
        // difference between the two dates.
        assertTrue(event.getCreatedTo().isAfter(event.getCreatedFrom()));
    }

    @Test
    void theModelFilterIsOfferedOnlyWhereModelsExist() {
        // Only chapters contain models; offering the filter elsewhere would be a filter that can never
        // match anything.
        ListingToolbar plain = new ListingToolbar();
        assertFalse(field(plain, "modelNameField").isVisible());

        ListingToolbar withModels = new ListingToolbar(true);
        assertTrue(field(withModels, "modelNameField").isVisible());

        UI.getCurrent().add(withModels);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(withModels, SearchEvent.class, fired::set);

        field(withModels, "modelNameField").setValue("lebka");
        searchButton(withModels).setEnabled(true);
        searchButton(withModels).click();

        assertEquals("lebka", fired.get().getModelName());
    }

    @Test
    void aHiddenModelFilterSendsNothing() {
        ListingToolbar toolbar = new ListingToolbar(true);
        UI.getCurrent().add(toolbar);
        AtomicReference<SearchEvent> fired = new AtomicReference<>();
        ComponentUtil.addListener(toolbar, SearchEvent.class, fired::set);

        field(toolbar, "modelNameField").setValue("lebka");
        toolbar.setModelFilterVisible(false);
        searchButton(toolbar).setEnabled(true);
        searchButton(toolbar).click();

        assertNull(fired.get().getModelName());
    }

    @Test
    void theExtraFiltersStayFoldedAwayUntilAskedFor() {
        // A row of six controls is harder to use than a row of three, and most searches never need the
        // narrower questions.
        ListingToolbar toolbar = new ListingToolbar();

        Button toggle = advancedToggle(toolbar);
        assertEquals("false", toggle.getElement().getAttribute("aria-expanded"));
        assertFalse(field(toolbar, "creatorField").getParent().orElseThrow().isVisible());

        toggle.click();

        assertEquals("true", toggle.getElement().getAttribute("aria-expanded"));
        assertTrue(field(toolbar, "creatorField").getParent().orElseThrow().isVisible());
    }

    @Test
    void setSearchFieldValueAndFieldExtractionShouldUseAllowedNonIdFields() throws Exception {
        ListingToolbar toolbar = new ListingToolbar();

        toolbar.setSearchFieldValue("lebka");

        assertEquals("lebka", toolbar.getSearchField().getValue());

        Method method = ListingToolbar.class.getDeclaredMethod("extractFieldNames", Class.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) method.invoke(toolbar, cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity.class);

        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("created"));
        assertTrue(fields.contains("updated"));
        assertTrue(fields.contains("description"));
        assertFalse(fields.contains("id"));
        assertFalse(fields.contains("creatorId"));
    }

    @SuppressWarnings("unchecked")
    private Select<String> orderBySelect(ListingToolbar toolbar) {
        return (Select<String>) read(toolbar, "orderBySelect");
    }

    private Button directionButton(ListingToolbar toolbar) {
        return (Button) read(toolbar, "directionButton");
    }

    private Button searchButton(ListingToolbar toolbar) {
        return (Button) read(toolbar, "createButton");
    }

    private Button advancedToggle(ListingToolbar toolbar) {
        return (Button) read(toolbar, "advancedToggle");
    }

    private TextField field(ListingToolbar toolbar, String name) {
        return (TextField) read(toolbar, name);
    }

    private DatePicker datePicker(ListingToolbar toolbar, String name) {
        return (DatePicker) read(toolbar, name);
    }

    private Object read(ListingToolbar toolbar, String fieldName) {
        try {
            var field = ListingToolbar.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(toolbar);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
