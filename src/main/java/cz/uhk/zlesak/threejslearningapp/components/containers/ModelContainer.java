package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;

import java.util.ArrayList;
import java.util.List;

/**
 * ModelDiv is a custom Div component that contains a ThreeJsComponent for rendering 3D models,
 * along with an overlay progress bar and description for loading actions coming back from the ThreeJsComponent.
 *
 * @see ThreeJsComponent
 */
public class ModelContainer extends Div {
    private final ProgressBar overlayProgressBar;
    private final Div overlayBackground;
    private final Span actionDescription;
    public final ThreeJsComponent renderer;
    public final ModelTextureAreaSelectContainer modelTextureAreaSelectContainer;
    protected final List<Registration> registrations = new ArrayList<>();

    /**
     * Constructor for ModelDiv component.
     */
    public ModelContainer() {
        super();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("height", "100vh");
        getStyle().set("width", "100%");
        getStyle().set("overflow", "hidden");

        renderer = new ThreeJsComponent();
        modelTextureAreaSelectContainer = new ModelTextureAreaSelectContainer();
        overlayBackground = getOverlayBackgroundDiv();
        overlayProgressBar = getOverlayProgressBar();
        actionDescription = getActionDescriptionSpan();
        Div rendererContainer = getRendererContainer();

        add(modelTextureAreaSelectContainer, rendererContainer);
    }

    /**
     * Creates and configures the container Div for the ThreeJsComponent renderer,
     * including the overlay background, progress bar, and action description.
     *
     * @return the configured container Div
     */
    private Div getRendererContainer() {
        Div rendererContainer = new Div(renderer, overlayBackground, overlayProgressBar, actionDescription);
        rendererContainer.setId("modelDiv");
        rendererContainer.getStyle().set("flex", "1 1 auto");
        rendererContainer.getStyle().set("height", "auto");
        rendererContainer.getStyle().set("max-height", "none");
        rendererContainer.getStyle().set("width", "100%");
        rendererContainer.getStyle().set("min-height", "0");
        rendererContainer.getStyle().set("position", "relative");
        rendererContainer.getStyle().set("overflow", "hidden");
        rendererContainer.getStyle().remove("z-index");

        return rendererContainer;
    }

    /**
     * Creates and configures the overlay ProgressBar component.
     * The progress bar is styled to be centered and overlayed on top of the renderer.
     *
     * @return the configured ProgressBar instance
     */
    private ProgressBar getOverlayProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.getStyle().set("position", "absolute");
        progressBar.getStyle().set("top", "50%");
        progressBar.getStyle().set("left", "50%");
        progressBar.getStyle().set("transform", "translate(-50%, -50%)");
        progressBar.getStyle().set("z-index", "11");
        progressBar.setWidth("300px");
        return progressBar;
    }

    /**
     * Creates and configures the Span component for displaying action descriptions.
     * The span is styled to be centered and overlayed on top of the renderer, below the progress bar.
     *
     * @return the configured Span instance
     */
    private Span getActionDescriptionSpan() {
        Span actionDescriptionSpan = new Span();
        actionDescriptionSpan.getStyle().set("position", "absolute");
        actionDescriptionSpan.getStyle().set("top", "55%");
        actionDescriptionSpan.getStyle().set("left", "50%");
        actionDescriptionSpan.getStyle().set("transform", "translate(-50%, -50%)");
        actionDescriptionSpan.getStyle().set("z-index", "11");
        actionDescriptionSpan.setWidth("300px");
        return actionDescriptionSpan;
    }

    /**
     * Creates and configures the overlay background Div component.
     * The background is styled to cover the entire renderer area with a semi-transparent overlay.
     *
     * @return the configured Div instance
     */
    private Div getOverlayBackgroundDiv() {
        Div background = new Div();
        background.setVisible(false);
        background.getStyle().set("position", "absolute");
        background.getStyle().set("top", "0");
        background.getStyle().set("left", "0");
        background.getStyle().set("width", "100%");
        background.getStyle().set("height", "100%");
        background.getStyle().set("background", "rgba(0,0,0,0.3)");
        background.getStyle().set("z-index", "10");
        return background;
    }

    /**
     * Shows the overlay progress bar and action description.
     * This method makes the overlay background, progress bar, and action description visible.
     */
    private void showOverlayProgressBar(String description) {
        actionDescription.setText(description);
        overlayBackground.setVisible(true);
        overlayProgressBar.setVisible(true);
        actionDescription.setVisible(true);
    }

    /**
     * Hides the overlay progress bar and action description.
     * This method makes the overlay background, progress bar, and action description invisible.
     */
    private void hideOverlayProgressBar() {
        overlayBackground.setVisible(false);
        overlayProgressBar.setVisible(false);
        actionDescription.setVisible(false);
    }
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsDoingActions.class,
                event -> showOverlayProgressBar(event.getDescription())
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsFinishedActions.class,
                event -> hideOverlayProgressBar()
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
