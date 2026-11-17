package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * The heading of a route: its name as the only H1, an optional line of context below it and a slot
 * for the primary action on the right.
 *
 * <p>Every screen needs an H1 — a screen reader user navigates by headings, and a page whose first
 * heading is an H2 or H3 reads as a fragment of some larger document that is not there. The listings,
 * the administration centre and the quiz detail had no H1 at all.
 *
 * <p>The meta line doubles as the place where a changed result count is announced, so it carries
 * {@code role="status"} and exists in the DOM from the start: a live region added at the same moment
 * as its text is not announced by most screen readers.
 */
public class PageHeader extends Header implements I18nAware {

    private final H1 heading = new H1();
    private final Span meta = new Span();
    private final HorizontalLayout actionSlot = new HorizontalLayout();
    private boolean metaVisuallyHidden = false;

    /**
     * Constructs the header with a title and no context line.
     *
     * @param title the route name, shown as the H1
     */
    public PageHeader(String title) {
        this(title, null);
    }

    /**
     * Constructs the header.
     *
     * @param title the route name, shown as the H1
     * @param metaText one line of context below the title, may be {@code null} or blank
     */
    public PageHeader(String title, String metaText) {
        addClassName("page-header");

        heading.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
        heading.getStyle().set("margin", "0");
        heading.setText(title == null ? "" : title);

        meta.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        meta.getElement().setAttribute("role", "status");
        setMeta(metaText);

        VerticalLayout textColumn = new VerticalLayout(heading, meta);
        textColumn.addClassName("page-header-text");
        textColumn.setPadding(false);
        textColumn.setSpacing(false);
        textColumn.getStyle().set("gap", "var(--lumo-space-xs)");
        textColumn.getStyle().set("min-width", "0");

        actionSlot.addClassName("page-header-actions");
        actionSlot.setPadding(false);
        actionSlot.setSpacing(true);
        actionSlot.setVisible(false);

        getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-items", "flex-end")
                .set("justify-content", "space-between")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("padding", "var(--lumo-space-m) var(--lumo-space-m) 0");

        add(textColumn, actionSlot);
    }

    /**
     * Replaces the title.
     *
     * @param title the new route name
     */
    public void setTitleText(String title) {
        heading.setText(title == null ? "" : title);
    }

    /**
     * Replaces the context line. A blank value hides it without removing the live region, so a later
     * update is still announced.
     *
     * @param metaText the line to show, may be {@code null} or blank
     */
    public void setMeta(String metaText) {
        String safe = metaText == null ? "" : metaText.trim();
        meta.setText(safe);
        if (metaVisuallyHidden) {
            return;
        }
        meta.getStyle().set("display", safe.isEmpty() ? "none" : "inline");
    }

    /**
     * Puts components into the action slot on the right, replacing whatever was there.
     *
     * @param components the actions, none hides the slot
     */
    public void setActions(Component... components) {
        actionSlot.removeAll();
        if (components == null || components.length == 0) {
            actionSlot.setVisible(false);
            return;
        }
        actionSlot.add(components);
        actionSlot.setVisible(true);
    }

    /**
     * Keeps the meta line as an announcement only, without showing it.
     *
     * <p>For screens where the same figure is already on display somewhere better — a listing states
     * its range in the pagination bar — but where the change still has to be announced. Clipped rather
     * than {@code display: none}, because a hidden element is not read out at all.
     */
    public void setMetaVisuallyHidden() {
        meta.getStyle()
                .set("position", "absolute")
                .set("width", "1px")
                .set("height", "1px")
                .set("overflow", "hidden")
                .set("clip-path", "inset(50%)")
                .set("white-space", "nowrap");
        metaVisuallyHidden = true;
    }

    /**
     * Hides or shows the H1 while leaving the meta line in place. Used where this header is nested
     * inside a screen that already has a title of its own — a second H1 leaves a screen reader user
     * unable to tell which one names the page — but where the result count still needs announcing.
     *
     * @param visible whether to show the H1
     */
    public void setHeadingVisible(boolean visible) {
        heading.setVisible(visible);
    }

    /**
     * Moves keyboard focus to the title. Used after an action replaces the screen contents — without
     * it focus stays on a button that no longer exists and the user hears nothing.
     */
    public void focusHeading() {
        heading.getElement().setAttribute("tabindex", "-1");
        heading.getElement().callJsFunction("focus");
    }

    /**
     * @return the H1, for tests and for callers that need to reference it by id.
     */
    public H1 getHeading() {
        return heading;
    }
}
