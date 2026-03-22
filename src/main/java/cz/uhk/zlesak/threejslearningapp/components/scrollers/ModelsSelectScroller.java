package cz.uhk.zlesak.threejslearningapp.components.scrollers;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelSelectContainer;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.RemoveFileEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContextException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    public ModelsSelectScroller() {
        super(new VerticalLayout(), ScrollDirection.VERTICAL);
        this.scrollerLayout = (VerticalLayout) getContent();
        setSizeFull();
    }

    /**
     * Initialize selects for main model and other models for sub-chapters.
     *
     * @param subChapterForSelectRecords Map of sub-chapter IDs to their names for which model selects should be created.
     *                                   Records are returned from backend to ensure only existing sub-chapters have selects.
     */
    public void initSelects(Map<String, String> subChapterForSelectRecords) {
        if (this.mainModelSelect == null) {
            modelSelectHorizontalLayout(text("modelSelect.main.caption"), "main", true);
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
        ModelSelectContainer container = new ModelSelectContainer(label, id, main, true);

        Button removeButton = getButton(container);

        container.getSelect().addValueChangeListener(event -> {
            if (event.getValue() == null || event.getValue().getModel() == null) {
                removeButton.setVisible(false);
                return;
            }
            boolean visible = event.getValue().getModel().getId() != null;
            removeButton.setVisible(visible);
        });

        if (main) {
            this.mainModelSelect = container.getSelect();
        } else {
            otherModelsHorizontalLayouts.putIfAbsent(id, container);
            removeButton.setVisible(false);
            container.add(removeButton);
        }
        scrollerLayout.add(container);
    }

    private @NonNull Button getButton(ModelSelectContainer container) {
        Button removeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        removeButton.addClickListener(e -> {
            AtomicInteger modelInstances = new AtomicInteger();
            getAllModelsMappedToChapterHeaderBlockId(false).values().forEach(model -> {
                if (container.getSelect().getValue() != null && model.getMetadataId().equals(container.getSelect().getValue().getMetadataId())) {
                    modelInstances.getAndIncrement();
                }
            });

            if (modelInstances.get() == 1 && container.getSelect().getValue() != null && container.getSelect().getValue().getModel() != null) {
                ComponentUtil.fireEvent(UI.getCurrent(), new RemoveFileEvent(UI.getCurrent(), container.getSelect().getValue().getModel().getId(), FileType.MODEL, container.getSelect().getValue().getModel().getId(), true));
            }
            container.getSelect().setValue(null);
            container.getSelect().clear();
        });
        return removeButton;
    }

    /**
     * Get all selected models mapped to their chapter header block IDs.
     * main model is mapped to "main" key, not to any chapter header block ID.
     *
     * @return Map of chapter header block IDs to selected QuickModelEntity instances.
     * @throws ApplicationContextException if the main model is not selected or the main model select has not been initialized yet.
     */
    public Map<String, QuickModelEntity> getAllModelsMappedToChapterHeaderBlockId(boolean... checkMainModel) throws ApplicationContextException {
        Map<String, QuickModelEntity> models = new HashMap<>();
        if  (checkMainModel.length == 0 || checkMainModel[0]) {
            if (mainModelSelect == null || mainModelSelect.getValue() == null) {
                throw new ApplicationContextException("Hlavní model není vybrán!");
            }
            models.put("main", mainModelSelect.getValue());
        }

        for (ModelSelectContainer container : otherModelsHorizontalLayouts.values()) {
            Select<QuickModelEntity> select = container.getSelect();
            QuickModelEntity selected = select.getValue();
            if (selected != null) {
                models.put(select.getElement().getAttribute("block-id"), selected);
            }
        }
        return models;
    }

    /**
     * Updates the model select for a given blockId with the selected model.
     * This is called when a model is selected from the dialog.
     *
     * @param blockId the blockId of the select to update
     * @param model   the selected model
     */
    public void updateModelSelect(String blockId, QuickModelEntity model) {
        if (blockId == null || blockId.isEmpty() || blockId.equals("main")) {
            if (mainModelSelect != null) {
                mainModelSelect.setItems(model);
                mainModelSelect.setValue(model);
            }
        } else {
            ModelSelectContainer container = otherModelsHorizontalLayouts.get(blockId);
            if (container != null) {
                Select<QuickModelEntity> select = container.getSelect();
                select.setItems(model);
                select.setValue(model);
            }
        }
    }
}

