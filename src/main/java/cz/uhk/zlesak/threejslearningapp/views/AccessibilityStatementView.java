package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PageHeader;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.IView;

/**
 * The accessibility statement the European Accessibility Act requires.
 *
 * <p>Its own public route rather than a page in the documentation: the documentation is behind the
 * login, and a statement nobody can read without an account is not published. Someone who cannot use
 * the login screen is exactly the person most likely to need this page.
 *
 * <p>The wording comes from {@code texts/accessibility_cs.json} so the operator can maintain it
 * without touching code. What is written there states the position honestly, including the parts that
 * are not yet conformant — a statement that claimed full conformance would be worse than none.
 */
@Route("accessibility")
@Tag("accessibility-statement-view")
@AnonymousAllowed
public class AccessibilityStatementView extends Composite<VerticalLayout> implements IView {

    /**
     * Builds the statement.
     */
    public AccessibilityStatementView() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setMaxWidth("70ch");
        content.getStyle().set("margin", "0 auto");

        content.add(new PageHeader(text("accessibility.title"), text("accessibility.updated")));
        content.add(paragraph("accessibility.intro"));

        content.add(section("accessibility.state.title"));
        content.add(paragraph("accessibility.state.body"));

        content.add(section("accessibility.exceptions.title"));
        UnorderedList exceptions = new UnorderedList(
                new ListItem(text("accessibility.exceptions.login")),
                new ListItem(text("accessibility.exceptions.modelDescription")),
                new ListItem(text("accessibility.exceptions.chapterContent"))
        );
        content.add(exceptions);

        content.add(section("accessibility.feedback.title"));
        content.add(paragraph("accessibility.feedback.body"));

        content.add(section("accessibility.enforcement.title"));
        content.add(paragraph("accessibility.enforcement.body"));

        Scroller scroller = new Scroller(content, Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();
        scroller.addClassName(LumoUtility.Padding.MEDIUM);

        getContent().setPadding(false);
        getContent().setSizeFull();
        getContent().add(scroller);
    }

    private H2 section(String key) {
        H2 heading = new H2(text(key));
        heading.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.NONE);
        return heading;
    }

    private Paragraph paragraph(String key) {
        Paragraph paragraph = new Paragraph(text(key));
        paragraph.addClassName(LumoUtility.Margin.Vertical.NONE);
        return paragraph;
    }

    @Override
    public String getPageTitle() {
        return text("page.title.accessibility");
    }
}
