package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.HeadingForSelect;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.SubChapterForSelect;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ScrollToElement;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ShowSubchapterContentEvent;
import cz.uhk.zlesak.threejslearningapp.events.chapter.SubchapterInitEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectComponentsTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void headingListingSelectShouldLoadRelevantItemsCreateScrollEventAndHandleRemoval() {
        TestHeadingListingSelect select = new TestHeadingListingSelect();
        SubchapterInitEvent initEvent = new SubchapterInitEvent(
                UI.getCurrent(),
                Map.of(
                        Triple.of("sub-1", "model-1", "Sub 1"), List.of(new Tuple<>("heading-1", "Heading 1"), new Tuple<>("heading-2", "Heading 2")),
                        Triple.of("sub-2", "main", "Sub 2"), List.of(new Tuple<>("heading-3", "Heading 3"))
                ),
                true
        );

        select.handleItemAdditionIngoingChangeEventAction(initEvent);
        select.showRelevantItemsBasedOnContext("sub-1", "heading-1");

        assertTrue(select.isEnabled());
        assertEquals(2, select.getListDataView().getItemCount());

        HasValue.ValueChangeEvent<HeadingForSelect> changeEvent = mockValueChangeEvent();
        when(changeEvent.getValue()).thenReturn(new HeadingForSelect("heading-1", "sub-1", "Heading 1"));
        when(changeEvent.isFromClient()).thenReturn(true);

        ScrollToElement event = (ScrollToElement) select.exposeCreateChangeEvent(changeEvent);
        assertEquals("heading-1", event.getElement());

        select.handleItemRemoveIngoingChangeEventAction("heading-1", true);
        select.showRelevantItemsBasedOnContext("sub-1", "heading-1");
        assertEquals(1, select.getListDataView().getItemCount());
    }

    @Test
    void subchapterListingSelectShouldCreateUiEventsAndFallbackToMainModel() {
        TestSubchapterListingSelect select = new TestSubchapterListingSelect();
        AtomicReference<ThreeJsActionEvent> threeJsEvent = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ThreeJsActionEvent.class, threeJsEvent::set);

        SubchapterInitEvent initEvent = new SubchapterInitEvent(
                UI.getCurrent(),
                Map.of(
                        Triple.of("sub-1", "model-1", "Sub 1"), List.of(),
                        Triple.of("sub-2", "main", "Sub 2"), List.of()
                ),
                true
        );

        select.handleItemAdditionIngoingChangeEventAction(initEvent);
        select.showRelevantItemsBasedOnContext("sub-1", "");

        assertTrue(select.isEnabled());
        assertEquals(1, select.getListDataView().getItemCount());

        HasValue.ValueChangeEvent<SubChapterForSelect> changeEvent = mockValueChangeEvent();
        when(changeEvent.getValue()).thenReturn(new SubChapterForSelect("sub-1", "Sub 1", "model-1"));
        when(changeEvent.isFromClient()).thenReturn(true);

        ShowSubchapterContentEvent event = (ShowSubchapterContentEvent) select.exposeCreateChangeEvent(changeEvent);
        assertEquals("sub-1", event.getSubchapterId());
        assertNotNull(threeJsEvent.get());
        assertEquals("model-1", threeJsEvent.get().getModelId());
        assertEquals(ThreeJsActions.SHOW_MODEL, threeJsEvent.get().getAction());

        HasValue.ValueChangeEvent<SubChapterForSelect> mainChangeEvent = mockValueChangeEvent();
        when(mainChangeEvent.getValue()).thenReturn(null);
        when(mainChangeEvent.isFromClient()).thenReturn(true);

        ShowSubchapterContentEvent mainEvent = (ShowSubchapterContentEvent) select.exposeCreateChangeEvent(mainChangeEvent);
        assertNull(mainEvent.getSubchapterId());
        assertEquals("main", threeJsEvent.get().getModelId());
    }

    private static final class TestHeadingListingSelect extends HeadingListingSelect {
        ComponentEvent<?> exposeCreateChangeEvent(ValueChangeEvent<HeadingForSelect> event) {
            return createChangeEvent(event);
        }
    }

    private static final class TestSubchapterListingSelect extends SubchapterListingSelect {
        ComponentEvent<?> exposeCreateChangeEvent(ValueChangeEvent<SubChapterForSelect> event) {
            return createChangeEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> HasValue.ValueChangeEvent<T> mockValueChangeEvent() {
        return mock(HasValue.ValueChangeEvent.class);
    }
}
