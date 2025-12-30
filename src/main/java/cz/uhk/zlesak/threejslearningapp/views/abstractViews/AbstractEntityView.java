package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.api.clients.AbstractFileApiClient;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelContainer;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.leaveDialogs.BeforeLeaveActionDialog;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;

import java.util.Map;

/**
 * AbstractEntityView Class - A base class for entity views in the application.
 * It extends AbstractView and provides common layout components for entity views.
 *
 */
public abstract class AbstractEntityView extends AbstractView {
    protected VerticalLayout entityContentNavigation = new VerticalLayout();
    protected VerticalLayout entityContent = new VerticalLayout();
    protected final ModelContainer modelDiv = new ModelContainer();
    protected boolean skipBeforeLeaveDialog;

    /**
     * Constructor for AbstractEntityView.
     * Initializes the layout and components for entity views.
     */
    public AbstractEntityView(String pageTitleKey, boolean skipBeforeLeaveDialog) {
        super(pageTitleKey);
        this.skipBeforeLeaveDialog = skipBeforeLeaveDialog;
        entityContentNavigation.setPadding(false);
        entityContentNavigation.addClassName(LumoUtility.Gap.MEDIUM);
        entityContentNavigation.setVisible(false);
        entityContentNavigation.getStyle().set("flex", "0 1 auto");
        entityContentNavigation.getStyle().set("width", "fit-content");
        entityContentNavigation.getStyle().set("max-width", "250px");

        entityContent.setSizeFull();
        entityContent.setPadding(false);
        entityContent.addClassName(LumoUtility.Gap.MEDIUM);

        HorizontalLayout entitySide = new HorizontalLayout(entityContentNavigation, entityContent);
        entitySide.setSizeFull();
        entitySide.setPadding(false);
        entitySide.addClassName(LumoUtility.Gap.MEDIUM);
        entitySide.setFlexGrow(0, entityContentNavigation);
        entitySide.setFlexGrow(1, entityContent);

        VerticalLayout modelSide = new VerticalLayout(modelDiv);
        modelSide.setSizeFull();
        modelSide.setPadding(false);
        modelSide.getStyle().set("flex-grow", "1");
        modelSide.addClassName(LumoUtility.Gap.MEDIUM);

        SplitLayout splitLayout = new SplitLayout(entitySide, modelSide);
        splitLayout.setSizeFull();
        splitLayout.addClassName(LumoUtility.Gap.MEDIUM);

        getContent().add(splitLayout);
    }

    /**
     * Called before leaving the view.
     * Shows a confirmation dialog if there are unsaved changes.
     * Disposes of the renderer to free up memory.
     *
     * @param event before leave event with event details
     */
    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (skipBeforeLeaveDialog) {
            disposeRendererAndProceed(event);
            return;
        }
        BeforeLeaveActionDialog.leave(event, this::disposeRendererAndProceed);
    }

    /**
     * Loads multiple models along with their associated textures by firing UploadFileEvent events.
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects
     */
    protected void loadModelsWithTextures(Map<String, QuickModelEntity> quickModelEntityMap) {
        for (QuickModelEntity quickModelEntity : quickModelEntityMap.values()) {
            loadSingleModelWithTextures(quickModelEntity);
        }
    }
    /**
     * Loads a single model along with its associated textures by firing UploadFileEvent events.
     *
     * @param quickModelEntity the QuickModelEntity containing the model and texture information
     */
    protected void loadSingleModelWithTextures(QuickModelEntity quickModelEntity) {

        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.MODEL, quickModelEntity.getModel().getId(),
                AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getModel().getId()),
                quickModelEntity.getModel().getName(), quickModelEntity.getMainTexture() != null));


        if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            for (var texture : quickModelEntity.getOtherTextures()) {
                String otherTextureUrl = AbstractFileApiClient.getStreamBeEndpointUrl(texture.getTextureFileId());
                ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.OTHER, texture.getTextureFileId(), otherTextureUrl, texture.getName(), true));
            }
        }

        if (quickModelEntity.getMainTexture() != null) {
            ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.MAIN, quickModelEntity.getMainTexture().getTextureFileId(),
                    AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getMainTexture().getTextureFileId()),
                    quickModelEntity.getMainTexture().getName(), true));
        }
    }

    /**
     * Disposes of the renderer and proceeds with navigation.
     *
     * @param event the before leave event
     */
    protected void disposeRendererAndProceed(BeforeLeaveEvent event) {
        BeforeLeaveEvent.ContinueNavigationAction postponed = event.postpone();
        disposeRendererAndProceed(postponed);
    }

    /**
     * Disposes of the renderer and proceeds with navigation.
     *
     * @param postponed the postponed navigation action
     */
    protected void disposeRendererAndProceed(BeforeLeaveEvent.ContinueNavigationAction postponed) {
        var ui = UI.getCurrent();
        if (modelDiv.renderer != null) {
            modelDiv.renderer.dispose(() -> ui.access(postponed::proceed));
        } else {
            postponed.proceed();
        }
    }
}
