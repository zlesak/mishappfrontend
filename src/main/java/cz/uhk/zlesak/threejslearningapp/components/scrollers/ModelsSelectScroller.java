package cz.uhk.zlesak.threejslearningapp.components.scrollers;

import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelSelectContainer;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import org.springframework.context.ApplicationContextException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A scroller component that contains selects for choosing 3D models for the main chapter and its sub-chapters.
 * Each select is paired with a button to open a dialog for selecting already created models.
 * The component allows dynamic initialization of selects based on existing sub-chapters.
 *
 */
public class ModelsSelectScroller extends Scroller implements I18nAware {
    private Select<QuickModelEntity> mainModelSelect;
    private final Map<String, ModelSelectContainer> otherModelsHorizontalLayouts = new HashMap<>();
    private final VerticalLayout scrollerLayout;
    private Consumer<Map<String, QuickModelEntity>> modelSelectConsumer;

    public ModelsSelectScroller() {
        super(new VerticalLayout(), ScrollDirection.VERTICAL);
        this.scrollerLayout = (VerticalLayout) getContent();
        setSizeFull();
    }

    /**
     * Set consumer to be called when a model is selected on any of the selects.
     *
     * @param onSelect Consumer to be called with the selected QuickModelEntity.
     */
    public void setModelSelectedConsumer(Consumer<Map<String, QuickModelEntity>> onSelect) {
        modelSelectConsumer = onSelect;
    }

    /**
     * Initialize selects for main model and other models for sub-chapters.
     *
     * @param subChapterForSelectRecords Map of sub-chapter IDs to their names for which model selects should be created.
     *                                   Records are returned from backend to ensure only existing sub-chapters have selects.
     */
    public void initSelects(Map<String, String> subChapterForSelectRecords) {
        if (this.mainModelSelect == null) {
            modelSelectHorizontalLayout(text("modelSelect.main.caption"), "", true);
        }

        otherModelsHorizontalLayouts.keySet().removeIf(id -> {
            if (!subChapterForSelectRecords.containsKey(id)) {
                this.scrollerLayout.remove(otherModelsHorizontalLayouts.get(id));
                return true;
            }
            return false;
        });

        subChapterForSelectRecords.forEach((id, text) -> {
            if (!otherModelsHorizontalLayouts.containsKey(id)) {
                modelSelectHorizontalLayout(text("modelSelect.other.caption") + text, id, false);
            }
        });
    }

    /**
     * Create horizontal layout with model select and button to choose already created model.
     *
     * @param label label for select component.
     * @param id    chapter header block ID for which the model is selected.
     * @param main  whether this is the main model select or sub-chapter model select.
     */
    private void modelSelectHorizontalLayout(String label, String id, boolean main) {
        ModelSelectContainer container = new ModelSelectContainer(label, id, main, modelSelectConsumer);

        if (main) {
            this.mainModelSelect = container.getSelect();
        } else {
            otherModelsHorizontalLayouts.putIfAbsent(id, container);
        }
        scrollerLayout.add(container);
    }

    /**
     * Get all selected models mapped to their chapter header block IDs.
     * main model is mapped to "main" key, not to any chapter header block ID.
     *
     * @return Map of chapter header block IDs to selected QuickModelEntity instances.
     * @throws ApplicationContextException if the main model is not selected or the main model select has not been initialized yet.
     */
    public Map<String, QuickModelEntity> getAllModelsMappedToChapterHeaderBlockId() throws ApplicationContextException {

        if (mainModelSelect == null || mainModelSelect.getValue() == null) {
            throw new ApplicationContextException("Hlavní model není vybrán!");
        }

        Map<String, QuickModelEntity> models = new HashMap<>();
        for (ModelSelectContainer container : otherModelsHorizontalLayouts.values()) {
            Select<QuickModelEntity> select = container.getSelect();
            QuickModelEntity selected = select.getValue();
            if (selected != null) {
                models.put(select.getElement().getAttribute("block-id"), selected);
            }
        }

        models.put("main", mainModelSelect.getValue());
        return models;
    }
}
