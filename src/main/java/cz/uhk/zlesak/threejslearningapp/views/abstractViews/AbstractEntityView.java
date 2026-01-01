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
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;

import java.util.Map;
import java.util.Objects;

/**
 * AbstractEntityView Class - A base class for entity views in the application.
 * It extends AbstractView and provides common layout components for entity views.
 *
 */
public abstract class AbstractEntityView extends AbstractView {
    protected VerticalLayout entityContentNavigation = new VerticalLayout();
    protected VerticalLayout entityContent = new VerticalLayout();
    protected VerticalLayout modelSide = new VerticalLayout();
    protected SplitLayout splitLayout;

    protected ModelContainer modelDiv = new ModelContainer();
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

        modelSide.add(modelDiv);
        modelSide.setSizeFull();
        modelSide.setPadding(false);
        modelSide.getStyle().set("flex-grow", "1");
        modelSide.addClassName(LumoUtility.Gap.MEDIUM);

        splitLayout = new SplitLayout(entitySide, modelSide);
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
     *
     * @param quickModelEntityMap a map of model IDs to QuickModelEntity objects
     */
    protected void loadModelsWithTextures(Map<String, QuickModelEntity> quickModelEntityMap) {

        quickModelEntityMap.forEach((key, quickModelEntity) -> loadSingleModelWithTextures(quickModelEntity, key));

        String modelToShow = quickModelEntityMap.get("main") != null ? quickModelEntityMap.get("main").getModel().getId() : quickModelEntityMap.values().stream().toList().getFirst().getModel().getId();

        ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelToShow, "main", ThreeJsActions.SHOW_MODEL, true));
    }

    /**
     * Loads a single model along with its associated textures by firing UploadFileEvent events.
     *
     * @param quickModelEntity the QuickModelEntity containing the model and texture information
     */
    protected void loadSingleModelWithTextures(QuickModelEntity quickModelEntity, String key, boolean... showImmediately) {

        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.MODEL, quickModelEntity.getModel().getId(),
                AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getModel().getId(), "model"),
                quickModelEntity.getModel().getName(), false, quickModelEntity.getMainTexture() != null, Objects.equals(key, "main")));


        if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            for (var texture : quickModelEntity.getOtherTextures()) {
                String otherTextureUrl = AbstractFileApiClient.getStreamBeEndpointUrl(texture.getTextureFileId(), "texture");
                ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.OTHER, texture.getTextureFileId(), otherTextureUrl, texture.getName(), false, true));
                if (!texture.getCsvContent().isEmpty()) {
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.CSV, texture.getTextureFileId(), texture.getCsvContent(), texture.getName(), false, true));
                }
            }
        }

        if (quickModelEntity.getMainTexture() != null) {
            ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), FileType.MAIN, quickModelEntity.getMainTexture().getTextureFileId(),
                    AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getMainTexture().getTextureFileId(), "texture"),
                    quickModelEntity.getMainTexture().getName(), false, true));
        }
        if (showImmediately.length > 0 && showImmediately[0]) {
            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), quickModelEntity.getModel().getId(), "main", ThreeJsActions.SHOW_MODEL, true));
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
