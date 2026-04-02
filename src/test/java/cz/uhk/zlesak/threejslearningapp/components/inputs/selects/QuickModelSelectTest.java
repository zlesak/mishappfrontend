package cz.uhk.zlesak.threejslearningapp.components.inputs.selects;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickModelSelectTest {

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void constructor_shouldConfigureHelperTextAndBlockId() {
        QuickModelSelect select = new QuickModelSelect("My Label", "block-1");

        assertEquals("My Label", select.getHelperText());
        assertEquals("block-1", select.getElement().getAttribute("block-id"));
        assertTrue(select.isReadOnly());
    }

    @Test
    void itemLabelGenerator_shouldReturnModelName_whenEntityNonNull() throws Exception {
        QuickModelSelect select = new QuickModelSelect("Label", "block-2");
        ItemLabelGenerator<QuickModelEntity> gen = getItemLabelGenerator(select);

        QuickModelEntity entity = QuickModelEntity.builder()
                .model(ModelFileEntity.builder().id("m-1").name("MyModel").build())
                .build();

        assertEquals("MyModel", gen.apply(entity));
    }

    @Test
    void itemLabelGenerator_shouldReturnEmptyString_whenEntityIsNull() throws Exception {
        QuickModelSelect select = new QuickModelSelect("Label", "block-3");
        ItemLabelGenerator<QuickModelEntity> gen = getItemLabelGenerator(select);

        assertEquals("", gen.apply(null));
    }

    @SuppressWarnings("unchecked")
    private static ItemLabelGenerator<QuickModelEntity> getItemLabelGenerator(QuickModelSelect select)
            throws Exception {
        Field field = Select.class.getDeclaredField("itemLabelGenerator");
        field.setAccessible(true);
        return (ItemLabelGenerator<QuickModelEntity>) field.get(select);
    }
}
