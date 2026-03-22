package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.api.clients.AbstractApiClient;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.containers.ModelContainer;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.leaveDialogs.BeforeLeaveActionDialog;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActionEvent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsActions;
import cz.uhk.zlesak.threejslearningapp.services.AbstractService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;

import java.util.Objects;

/**
 * AbstractEntityView Class - A base class for entity views in the application.
 * It extends AbstractView and provides common layout components for entity views.
 *
 * @param <S> the type of service associated with the entity view
 */
public abstract class AbstractEntityView<S extends AbstractService<?, ?, ?>> extends AbstractView<S> {
    protected VerticalLayout entityContent = new VerticalLayout();
    protected VerticalLayout modelSide = new VerticalLayout();
    protected SplitLayout splitLayout;

    protected ModelContainer modelDiv = new ModelContainer();
    protected boolean skipBeforeLeaveDialog;
    private ModelService modelService = SpringContextUtils.getBean(ModelService.class);

    /**
     * Constructor for AbstractEntityView.
     *
     * @param pageTitleKey          the title key for the page
     * @param skipBeforeLeaveDialog flag to skip the before leave dialog
     * @param service               the service associated with the view
     */
    public AbstractEntityView(String pageTitleKey, boolean skipBeforeLeaveDialog, S service) {
        super(pageTitleKey, service);
        this.skipBeforeLeaveDialog = skipBeforeLeaveDialog;

        entityContent.setSizeFull();
        entityContent.setPadding(false);
        entityContent.addClassName(LumoUtility.Gap.XSMALL);

        modelSide.add(modelDiv);
        modelSide.setSizeFull();
        modelSide.setPadding(false);
        modelSide.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Gap.MEDIUM);

        splitLayout = new SplitLayout(entityContent, modelSide);
        splitLayout.setSizeFull();
        splitLayout.addClassName(LumoUtility.Gap.MEDIUM);

        getContent().add(splitLayout);
    }

    /**
     * Constructor for AbstractEntityView.
     *
     * @param pageTitleKey the title key for the page
     * @param service      the service associated with the view
     */
    public AbstractEntityView(String pageTitleKey, S service) {
        super(pageTitleKey, service);
        this.skipBeforeLeaveDialog = true;

        modelSide.add(modelDiv);
        modelSide.setSizeFull();
        modelSide.setPadding(false);
        modelSide.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Gap.MEDIUM);
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
     * Loads a single model along with its associated textures by firing UploadFileEvent events.
     *
     * @param quickModelEntity the QuickModelEntity containing model and texture information
     * @param questionId       the question ID associated with the model (can be null)
     * @param key              the key identifying the model
     * @param showImmediately  optional flag to indicate if the model should be shown immediately after loading
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

        ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.MODEL,
                quickModelEntity.getMainTexture() != null ? quickModelEntity.getMainTexture().getId() : "main",
                AbstractApiClient.getStreamBeEndpointUrl(quickModelEntity.getModel().getId()),
                quickModelEntity.getModel().getName(), false, questionId, Objects.equals(key, "main")));


        if (quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            for (var texture : quickModelEntity.getOtherTextures()) {
                String otherTextureUrl = AbstractApiClient.getStreamBeEndpointUrl(texture.getId());
                ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.OTHER, texture.getId(), otherTextureUrl, texture.getName(), false, questionId, true));
                if (texture.getCsvContent() != null && !texture.getCsvContent().isEmpty()) {
                    ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.CSV, texture.getId(), texture.getCsvContent(), texture.getName(), false, questionId, true));
                }
            }
        }

        if (quickModelEntity.getMainTexture() != null) {
            ComponentUtil.fireEvent(UI.getCurrent(), new UploadFileEvent(UI.getCurrent(), modelId, FileType.MAIN, quickModelEntity.getMainTexture().getId(),
                    AbstractApiClient.getStreamBeEndpointUrl(quickModelEntity.getMainTexture().getId()),
                    quickModelEntity.getMainTexture().getName(), false, questionId, true));
        }
        if (showImmediately.length > 0 && showImmediately[0]) {
            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, quickModelEntity.getMainTexture() != null ? quickModelEntity.getMainTexture().getId() : null, ThreeJsActions.SHOW_MODEL, true, questionId));
        }
        if (quickModelEntity.getMainTexture() == null && questionId != null && quickModelEntity.getOtherTextures() != null && !quickModelEntity.getOtherTextures().isEmpty()) {
            ComponentUtil.fireEvent(UI.getCurrent(), new ThreeJsActionEvent(UI.getCurrent(), modelId, quickModelEntity.getOtherTextures().getFirst().getId(), ThreeJsActions.SWITCH_OTHER_TEXTURE, true, questionId));
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

    /**
     * Called when the component is attached to the UI.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelLoadEvent.class,
                event -> loadSingleModelWithTextures(
                        event.getQuickModelEntity(),
                        event.getQuestionId(),
                        event.getQuickModelEntity().getModel().getId(),
                        true
                )
        ));
    }
}
