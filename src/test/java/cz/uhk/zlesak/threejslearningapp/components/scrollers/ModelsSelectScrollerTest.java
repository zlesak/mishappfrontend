package cz.uhk.zlesak.threejslearningapp.components.scrollers;

import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextException;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ModelsSelectScrollerTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUiWithBeans(Map.of(
                ModelService.class, mock(ModelService.class)
        ));
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void initSelectsShouldCreateAndRemoveSubchapterSelectors() {
        ModelsSelectScroller scroller = new ModelsSelectScroller();
        scroller.initSelects(new LinkedHashMap<>(Map.of("sub-1", "Prvni", "sub-2", "Druhy")));
        Map<String, ?> layouts = getOtherLayouts(scroller);

        assertEquals(2, layouts.size());

        scroller.initSelects(Map.of("sub-2", "Druhy"));

        assertEquals(1, layouts.size());
        assertTrue(layouts.containsKey("sub-2"));
    }

    @Test
    void getAllModelsMappedToChapterHeaderBlockIdShouldRequireMainModel() {
        ModelsSelectScroller scroller = new ModelsSelectScroller();

        ApplicationContextException ex = assertThrows(ApplicationContextException.class,
                scroller::getAllModelsMappedToChapterHeaderBlockId);

        assertEquals("Hlavní model není vybrán!", ex.getMessage());
    }

    @Test
    void updateModelSelectShouldPopulateMainAndSubchapterSelections() {
        ModelsSelectScroller scroller = new ModelsSelectScroller();
        scroller.initSelects(Map.of("sub-1", "Podkapitola"));

        QuickModelEntity mainModel = TestFixtures.model("meta-main", "model-main", "Main", null, List.of());
        QuickModelEntity subModel = TestFixtures.model("meta-sub", "model-sub", "Sub", null, List.of());

        scroller.updateModelSelect("main", mainModel);
        scroller.updateModelSelect("sub-1", subModel);

        Select<QuickModelEntity> mainSelect = getSelectField(scroller, "mainModelSelect");
        assertEquals(mainModel, mainSelect.getValue());

        Map<String, ?> layouts = getOtherLayouts(scroller);
        Object container = layouts.get("sub-1");
        Select<QuickModelEntity> subSelect = getSelectField(container, "select");
        assertEquals(subModel, subSelect.getValue());
    }

    @Test
    void getAllModelsMappedToChapterHeaderBlockIdShouldReturnSelectedModels() {
        ModelsSelectScroller scroller = new ModelsSelectScroller();
        scroller.initSelects(Map.of("sub-1", "Podkapitola"));

        QuickModelEntity mainModel = TestFixtures.model("meta-main", "model-main", "Main", null, List.of());
        QuickModelEntity subModel = TestFixtures.model("meta-sub", "model-sub", "Sub", null, List.of());

        scroller.updateModelSelect(null, mainModel);
        scroller.updateModelSelect("sub-1", subModel);

        Map<String, QuickModelEntity> result = scroller.getAllModelsMappedToChapterHeaderBlockId();

        assertEquals(2, result.size());
        assertEquals(mainModel, result.get("main"));
        assertEquals(subModel, result.get("sub-1"));
    }

    private Object getField(Object target, String name) {
        try {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getOtherLayouts(Object target) {
        return (Map<String, ?>) getField(target, "otherModelsHorizontalLayouts");
    }

    @SuppressWarnings("unchecked")
    private Select<QuickModelEntity> getSelectField(Object target, String name) {
        return (Select<QuickModelEntity>) getField(target, name);
    }
}
