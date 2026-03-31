package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.ThreeJsComponent;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsDoingActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsFinishedActions;
import cz.uhk.zlesak.threejslearningapp.events.threejs.ThreeJsLoadingProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * ModelDiv is a custom Div component that contains a ThreeJsComponent for rendering 3D models,
 * along with an overlay progress bar and description for loading actions coming back from the ThreeJsComponent.
 *
 * @see ThreeJsComponent
 */
public class ModelContainer extends Div {
    private static final int DESKTOP_BREAKPOINT = 1024;
    private final Button controlsToggleButton;
    private final ProgressBar overlayProgressBar;
    private final Div overlayBackground;
    private final Span actionDescription;
    public final ThreeJsComponent renderer;
    public final ModelTextureAreaSelectContainer modelTextureAreaSelectContainer;
    private boolean controlsExpanded = true;
    private boolean compactControlsExpanded = true;
    private String controlsStateKey = "";
    protected final List<Registration> registrations = new ArrayList<>();

    /**
     * Constructor for ModelDiv component.
     */
    public ModelContainer() {
        super();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("height", "100%");
        getStyle().set("width", "100%");
        getStyle().set("overflow", "hidden");

        renderer = new ThreeJsComponent();
        controlsToggleButton = new Button("3D filtry", VaadinIcon.ANGLE_DOWN.create());
        controlsToggleButton.addClassName("model-controls-toggle");
        controlsToggleButton.setWidthFull();
        controlsToggleButton.addClickListener(e -> setControlsExpanded(!controlsExpanded, true));
        modelTextureAreaSelectContainer = new ModelTextureAreaSelectContainer();
        overlayBackground = getOverlayBackgroundDiv();
        overlayProgressBar = getOverlayProgressBar();
        actionDescription = getActionDescriptionSpan();
        Div rendererContainer = getRendererContainer();

        add(controlsToggleButton, modelTextureAreaSelectContainer, rendererContainer);
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
        progressBar.setWidth("min(300px, 90vw)");
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
        actionDescriptionSpan.setWidth("min(300px, 90vw)");
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
        attachEvent.getUI().getPage().executeJs("return window.location.pathname;")
                .then(String.class, path -> {
                    controlsStateKey = "model.controls." + path;
                    attachEvent.getUI().getPage()
                            .executeJs("return window.innerWidth;")
                            .then(Integer.class, width -> {
                                int viewportWidth = width == null ? DESKTOP_BREAKPOINT : width;
                                if (viewportWidth >= DESKTOP_BREAKPOINT) {
                                    applyControlsModeForWidth(viewportWidth);
                                    return;
                                }
                                attachEvent.getUI().getPage()
                                        .executeJs("const raw = sessionStorage.getItem($0); return raw === null ? '' : raw;", controlsStateKey)
                                        .then(String.class, stored -> {
                                            if (stored != null && !stored.isBlank()) {
                                                compactControlsExpanded = Boolean.parseBoolean(stored);
                                            } else {
                                                compactControlsExpanded = viewportWidth > 599;
                                            }
                                            applyControlsModeForWidth(viewportWidth);
                                        });
                            });
                });
        registrations.add(attachEvent.getUI().getPage().addBrowserWindowResizeListener(
                event -> applyControlsModeForWidth(event.getWidth())
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsDoingActions.class,
                event -> {
                    if (event.getSource() != renderer) {
                        return;
                    }
                    showOverlayProgressBar(event.getDescription());
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsFinishedActions.class,
                event -> {
                    if (event.getSource() != renderer) {
                        return;
                    }
                    hideOverlayProgressBar();
                }
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ThreeJsLoadingProgress.class,
                event -> {
                    if (event.getSource() != renderer) {
                        return;
                    }
                    int percent = event.getPercent();
                    String desc = event.getDescription();
                    if (percent < 0) {
                        overlayProgressBar.setIndeterminate(true);
                    } else {
                        overlayProgressBar.setIndeterminate(false);
                        overlayProgressBar.setValue(percent / 100.0);
                        overlayProgressBar.setVisible(true);
                    }
                    if (desc != null && !desc.isBlank()) {
                        actionDescription.setText(desc);
                        actionDescription.setVisible(true);
                    }
                }
        ));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }

    private void setControlsExpanded(boolean expanded, boolean persist) {
        controlsExpanded = expanded;
        modelTextureAreaSelectContainer.setVisible(expanded);
        controlsToggleButton.setIcon(expanded ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
        controlsToggleButton.setText(expanded ? "3D filtry (skrýt)" : "3D filtry (zobrazit)");

        if (!persist || controlsStateKey == null || controlsStateKey.isBlank()) {
            return;
        }
        UI currentUi = UI.getCurrent();
        if (currentUi != null) {
            compactControlsExpanded = expanded;
            currentUi.getPage().executeJs("sessionStorage.setItem($0, $1);", controlsStateKey, String.valueOf(expanded));
        }
    }

    private void applyControlsModeForWidth(int viewportWidth) {
        boolean desktop = viewportWidth >= DESKTOP_BREAKPOINT;
        controlsToggleButton.setVisible(!desktop);
        if (desktop) {
            setControlsExpanded(true, false);
        } else {
            setControlsExpanded(compactControlsExpanded, false);
        }
    }
}
