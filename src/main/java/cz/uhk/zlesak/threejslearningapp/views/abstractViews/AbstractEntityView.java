package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.AttachEvent;
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
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
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
    protected HorizontalLayout entitySide = new HorizontalLayout(entityContentNavigation, entityContent);

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

    public AbstractEntityView(String pageTitleKey) {
        super(pageTitleKey);
        this.skipBeforeLeaveDialog = true;

        modelSide.add(modelDiv);
        modelSide.setSizeFull();
        modelSide.setPadding(false);
        modelSide.getStyle().set("flex-grow", "1");
        modelSide.addClassName(LumoUtility.Gap.MEDIUM);
        getContent().add(modelSide);
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

        quickModelEntityMap.forEach((key, quickModelEntity) -> {
            boolean show = Objects.equals(key, "main");
            loadSingleModelWithTextures(quickModelEntity, null, key, show);
        });
    }

    /**
     * Loads a single model along with its associated textures by firing UploadFileEvent events.
     *
     * @param quickModelEntity the QuickModelEntity containing the model and texture information
     */
    protected void loadSingleModelWithTextures(QuickModelEntity quickModelEntity, String questionId, String key, boolean... showImmediately) {
        String modelId = quickModelEntity.getModel().getId();
        if (questionId != null) {
            ComponentUtil.fireEvent(
                    UI.getCurrent(),
                    new ThreeJsActionEvent(
                            UI.getCurrent(),
                            modelId,
                            null,
                            ThreeJsActions.REMOVE,
                            true,
                            questionId
                    )
            );
        }

        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.MODEL, quickModelEntity.getModel().getId(),
                AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getModel().getId(), "model"),
                quickModelEntity.getModel().getName(), false, questionId, quickModelEntity.getMainTexture() != null || quickModelEntity.isAdvanced(), Objects.equals(key, "main")));


        if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            for (var texture : quickModelEntity.getOtherTextures()) {
                String otherTextureUrl = AbstractFileApiClient.getStreamBeEndpointUrl(texture.getTextureFileId(), "texture");
                ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.OTHER, texture.getTextureFileId(), otherTextureUrl, texture.getName(), false, questionId, true));
                if (texture.getCsvContent() != null && !texture.getCsvContent().isEmpty()) {
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.CSV, texture.getTextureFileId(), texture.getCsvContent(), texture.getName(), false, questionId, true));
                }
            }
        }

        if (quickModelEntity.getMainTexture() != null) {
            ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.MAIN, quickModelEntity.getMainTexture().getTextureFileId(),
                    AbstractFileApiClient.getStreamBeEndpointUrl(quickModelEntity.getMainTexture().getTextureFileId(), "texture"),
                    quickModelEntity.getMainTexture().getName(), false, questionId, true));
        }
        if (showImmediately.length > 0 && showImmediately[0]) {
            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, "main", ThreeJsActions.SHOW_MODEL, true, questionId));
        }
        if (quickModelEntity.getMainTexture() == null && quickModelEntity.isAdvanced() && questionId != null && quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()){
            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, quickModelEntity.getOtherTextures().getFirst().getTextureFileId(), ThreeJsActions.SWITCH_OTHER_TEXTURE, true, questionId));
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


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelLoadEvent.class,
                event -> loadSingleModelWithTextures(event.getQuickModelEntity(), event.getQuestionId(), event.getQuickModelEntity().getModel().getId(), true)
        ));
    }
}
