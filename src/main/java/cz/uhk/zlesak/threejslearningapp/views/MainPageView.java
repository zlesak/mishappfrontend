package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.IView;

/**
 * Main page view of the application.
 * This view is accessible at the root route ("/").
 * It displays a welcome message and some introductory text.
 */
@Route("")
@Tag("main-page-view")
@AnonymousAllowed
public class MainPageView extends Composite<VerticalLayout> implements IView {
    /**
     * Constructor for MainPageView.
     * Initializes the layout with a welcome message and description.
     */
    public MainPageView() {
        H1 h1 = new H1(text("welcomeMessage"));
        h1.setWidth("max-content");

        Paragraph textSmall = new Paragraph(text("description"));
        textSmall.setWidth("max-content");
        textSmall.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        HorizontalLayout layoutRow = new HorizontalLayout();
        VerticalLayout layoutColumn = new VerticalLayout();
        Hr hr = new Hr();

        layoutColumn.setWidth("100%");
        layoutRow.add(layoutColumn);
        layoutColumn.add(h1, hr, textSmall);

        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        getContent().setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        getContent().setAlignItems(FlexComponent.Alignment.CENTER);
        getContent().setFlexGrow(1.0, layoutRow);
        getContent().add(layoutRow);
    }

    /**
     * Gets the title of the page.
     *
     * @return The page title as a string.
     */
    @Override
    public String getPageTitle() {
        return text("page.title.mainPageView");
    }
}
