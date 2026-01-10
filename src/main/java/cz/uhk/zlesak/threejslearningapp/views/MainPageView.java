package cz.uhk.zlesak.threejslearningapp.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.IView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;

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
        VerticalLayout mainLayout = getContent();
        mainLayout.setWidthFull();
        mainLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        mainLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        createHeroSection(mainLayout);

        Hr divider1 = new Hr();
        divider1.setWidth("80%");
        mainLayout.add(divider1);

        createFeaturesSection(mainLayout);

        Hr divider2 = new Hr();
        divider2.setWidth("80%");
        mainLayout.add(divider2);

        createShowcaseSection(mainLayout);

        Hr divider3 = new Hr();
        divider3.setWidth("80%");
        mainLayout.add(divider3);

        createAboutAndCollaborationSection(mainLayout);
    }

    private void createHeroSection(VerticalLayout parent) {
        HorizontalLayout hero = new HorizontalLayout();
        hero.setWidthFull();
        hero.setMinHeight("calc(100vh - 100px)");
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        hero.setPadding(true);
        hero.setSpacing(true);

        Div logoWrapper = new Div();
        logoWrapper.setWidth("50%");
        logoWrapper.setHeightFull();
        logoWrapper.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Display.FLEX);

        Image logo = new Image(DownloadHandler.forServletResource("/img/MISH_big.png"), "MISH Logo");
        logo.setMaxWidth("90%");
        logo.setMaxHeight("80vh");
        logo.getStyle().set("object-fit", "contain");

        logoWrapper.add(logo);

        VerticalLayout textContent = new VerticalLayout();
        textContent.setWidth("50%");
        textContent.setSpacing(false);
        textContent.setPadding(true);
        textContent.setAlignItems(FlexComponent.Alignment.START);
        textContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);


        H1 title = new H1(text("applicationTitle"));
        title.addClassNames(LumoUtility.Margin.Bottom.SMALL, LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.LineHeight.MEDIUM);

        H2 subtitle = new H2(text("welcomeMessage"));
        subtitle.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.FontSize.XLARGE, LumoUtility.TextColor.SECONDARY);

        Paragraph description = new Paragraph(text("description"));
        description.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.LARGE, LumoUtility.TextAlignment.JUSTIFY);
        description.getStyle().set("max-width", "600px");

        HorizontalLayout buttons = new HorizontalLayout();
        Button startBtn = new Button(text("cta.start"), new Icon(VaadinIcon.ACADEMY_CAP));
        startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        startBtn.addClickListener(e -> UI.getCurrent().navigate(ChapterListingView.class));

        Button modelsBtn = new Button(text("cta.models"), new Icon(VaadinIcon.CUBE));
        modelsBtn.addThemeVariants(ButtonVariant.LUMO_LARGE);
        modelsBtn.addClickListener(e -> UI.getCurrent().navigate(ModelListingView.class));

        buttons.add(startBtn, modelsBtn);

        textContent.add(title, subtitle, description, buttons);

        hero.add(logoWrapper, textContent);
        parent.add(hero);
    }

    private void createFeaturesSection(VerticalLayout parent) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        section.setPadding(true);

        H2 title = new H2(text("features.title"));
        title.addClassNames(LumoUtility.Margin.Bottom.XLARGE);

        FlexLayout cardsLayout = new FlexLayout();
        cardsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cardsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardsLayout.getStyle().set("gap", "var(--lumo-space-xl)");
        cardsLayout.setWidthFull();
        cardsLayout.setMaxWidth("1200px");

        cardsLayout.add(
                createFeatureCard(VaadinIcon.CUBE, "features.models.title", "features.models.desc"),
                createFeatureCard(VaadinIcon.BOOK, "features.chapters.title", "features.chapters.desc"),
                createFeatureCard(VaadinIcon.QUESTION, "features.quizzes.title", "features.quizzes.desc"),
                createFeatureCard(VaadinIcon.LAPTOP, "features.platform.title", "features.platform.desc")
        );

        section.add(title, cardsLayout);
        parent.add(section);
    }

    private VerticalLayout createFeatureCard(VaadinIcon icon, String titleKey, String descKey) {
        VerticalLayout card = new VerticalLayout();
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setWidth("250px");
        card.addClassNames(LumoUtility.TextAlignment.CENTER);

        Icon i = icon.create();
        i.setSize("48px");
        i.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Bottom.MEDIUM);

        H3 title = new H3(text(titleKey));
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.SMALL);

        Paragraph desc = new Paragraph(text(descKey));
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.NONE);

        card.add(i, title, desc);
        return card;
    }

    private void createShowcaseSection(VerticalLayout parent) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        section.setPadding(true);

        H2 title = new H2(text("showcase.title"));
        Paragraph desc = new Paragraph(text("showcase.desc"));
        title.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        FlexLayout showcaseGrid = new FlexLayout();
        showcaseGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        showcaseGrid.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        showcaseGrid.getStyle().set("gap", "var(--lumo-space-l)");
        showcaseGrid.setWidthFull();
        showcaseGrid.setMaxWidth("1200px");

        // Placeholders for GIFs TODO: Replace with actual GIF/Image components from aplication functionality
        showcaseGrid.add(createGifPlaceholder("showcase.gif1.title"));
        showcaseGrid.add(createGifPlaceholder("showcase.gif2.title"));
        showcaseGrid.add(createGifPlaceholder("showcase.gif3.title"));

        section.add(title, desc, showcaseGrid);
        parent.add(section);
    }

    private VerticalLayout createGifPlaceholder(String titleKey) {
        VerticalLayout container = new VerticalLayout();
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setWidth("350px");

        Div placeholder = new Div();
        placeholder.setWidth("100%");
        placeholder.setHeight("200px");
        placeholder.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        placeholder.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        placeholder.getStyle().set("display", "flex");
        placeholder.getStyle().set("align-items", "center");
        placeholder.getStyle().set("justify-content", "center");

        // TODO: Replace with real Image component pointing to GIF
        Span label = new Span("GIF/Image Placeholder");
        label.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);
        placeholder.add(label);

        H4 title = new H4(text(titleKey));
        title.addClassNames(LumoUtility.Margin.Top.SMALL);

        container.add(placeholder, title);
        return container;
    }

    private void createAboutAndCollaborationSection(VerticalLayout parent) {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setAlignItems(FlexComponent.Alignment.CENTER);
        section.setPadding(true);
        section.setSpacing(true);
        section.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.Vertical.XLARGE);

        FlexLayout container = new FlexLayout();
        container.setWidthFull();
        container.setMaxWidth("1200px");
        container.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.addClassNames(LumoUtility.Gap.XLARGE);

        VerticalLayout aboutCol = new VerticalLayout();
        aboutCol.setPadding(false);
        aboutCol.getStyle().set("flex", "1");
        aboutCol.getStyle().set("min-width", "300px");

        H2 aboutTitle = new H2(text("about.title"));
        aboutTitle.addClassNames(LumoUtility.Margin.Top.NONE);

        Paragraph aboutDesc = new Paragraph(text("about.description"));
        aboutDesc.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.TextColor.SECONDARY);

        aboutCol.add(aboutTitle, aboutDesc);

        VerticalLayout rightCol = new VerticalLayout();
        rightCol.setPadding(false);
        rightCol.getStyle().set("flex", "1");
        rightCol.getStyle().set("min-width", "300px");

        H3 collabTitle = new H3(text("collaboration.title"));
        collabTitle.addClassNames(LumoUtility.Margin.Top.NONE);

        Paragraph collabDesc = new Paragraph(text("collaboration.description"));
        collabDesc.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        H4 targetsTitle = new H4(text("forWho.title"));
        targetsTitle.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        UnorderedList list = new UnorderedList();
        list.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Padding.Left.LARGE);

        ListItem studentItem = new ListItem(text("target.students"));
        ListItem teacherItem = new ListItem(text("target.teachers"));

        list.add(studentItem, teacherItem);

        rightCol.add(collabTitle, collabDesc, new Hr(), targetsTitle, list);

        container.add(aboutCol, rightCol);
        section.add(container);
        parent.add(section);
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
