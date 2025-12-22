package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelClearEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelTextureChangeEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelUploadEvent;
import cz.uhk.zlesak.threejslearningapp.events.texture.OtherTextureLoadedEvent;
import cz.uhk.zlesak.threejslearningapp.events.texture.OtherTextureRemovedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

/**
 * AbstractModelView, abstract view for displaying and managing 3D models.
 */
@Slf4j
@Scope("prototype")
public abstract class AbstractModelView extends AbstractEntityView {
    protected final ModelUploadForm modelUploadForm;
    private Map<String, QuickModelEntity> quickModelEntity;

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey the key for the page title
     */
    public AbstractModelView(String pageTitleKey) {
        this(pageTitleKey, true);
    }

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey          the key for the page title
     * @param skipBeforeLeaveDialog flag to skip before-leave dialog
     */
    public AbstractModelView(String pageTitleKey, boolean skipBeforeLeaveDialog) {
        super(pageTitleKey, skipBeforeLeaveDialog);
        modelUploadForm = new ModelUploadForm();
        modelUploadForm.setWidthFull();
        entityContent.add(modelUploadForm);
    }

    /**
     * Called when the component is attached to the UI.
     * Registers event listeners for various model and texture events.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // ModelUploadEvent listener
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelUploadEvent.class,
                event -> {
                    quickModelEntity = Map.of(event.getModelId(), QuickModelEntity.builder()
                            .model(ModelFileEntity.builder().id(event.getModelId()).name(event.getModelName()).build())
                            .mainTexture(QuickTextureEntity.builder().textureFileId(event.getTextureName()).name(event.getTextureName()).build())
                            .build());
                    modelDiv.renderer.loadModel(event.getModel(), event.getTexture(), event.getModelId());
                    if (!event.isAdvanced()) {
                        modelDiv.renderer.showModel(event.getModelId());
                    }
                }
        ));

        // ModelClearEvent listener
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelClearEvent.class,
                event -> modelDiv.renderer.clear()
        ));

        // OtherTextureLoadedEvent listener
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                OtherTextureLoadedEvent.class,
                event -> modelDiv.renderer.addOtherTextures(event.getBase64Textures(), "modelId")
        ));

        // OtherTextureRemovedEvent listener
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                OtherTextureRemovedEvent.class,
                event -> modelDiv.renderer.removeOtherTexture("modelId", event.getName())
        ));

        // ModelTextureChangeEvent listener
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelTextureChangeEvent.class,
                event -> {
                    QuickModelEntity model = quickModelEntity.get("modelId");
                    model.setMainTexture(event.getQuickTextureEntity().get("main"));

                    List<QuickTextureEntity> textures = event.getQuickTextureEntity().entrySet().stream()
                            .filter(e -> !e.getKey().equals("main")).map(Map.Entry::getValue)
                            .toList();

                    model.setOtherTextures(textures);
                    modelDiv.modelTextureAreaSelectContainer.initializeData(quickModelEntity);
                }
        ));
    }
}
