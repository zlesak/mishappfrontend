package cz.uhk.zlesak.threejslearningapp.views.administration;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.backend.service.CurrentUserProvider;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PageHeader;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractView;
import cz.uhk.zlesak.threejslearningapp.views.chapter.ChapterListingView;
import cz.uhk.zlesak.threejslearningapp.views.model.ModelListingView;
import cz.uhk.zlesak.threejslearningapp.views.quizes.QuizListingView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * AdministrationView is the main view for administration tasks.
 * It provides tabs for managing chapters, models, and quizzes.
 * Uses ChapterService, ModelService, and QuizService for data operations.
 * Uses listing views for each entity type.
 */
@Slf4j
@Route("administration")
@Tag("administration-view")
@Scope("prototype")
@RolesAllowed({"ADMIN", "TEACHER"})
public class AdministrationView extends AbstractView<ChapterService> {

    private final ChapterService chapterService;
    private final ModelService modelService;
    private final QuizService quizService;
    private final CurrentUserProvider currentUserProvider;

    private TabSheet navigationTabs;
    private PageHeader pageHeader;
    private Span actionAnnouncement;

    private Tab chaptersTab;
    private Tab modelsTab;
    private Tab quizzesTab;

    private ChapterListingView chapterListingView;
    private ModelListingView modelListingView;
    private QuizListingView quizListingView;

    /**
     * Constructor for AdministrationView.
     * @param chapterService the chapter service
     * @param modelService the model service
     * @param quizService the quiz service
     * @param currentUserProvider resolves which role the signed-in user holds
     */
    @Autowired
    public AdministrationView(ChapterService chapterService, ModelService modelService, QuizService quizService,
                              CurrentUserProvider currentUserProvider) {
        super("page.title.administrationView", chapterService);
        this.chapterService = chapterService;
        this.modelService = modelService;
        this.quizService = quizService;
        this.currentUserProvider = currentUserProvider;

        buildLayout();
    }

    /**
     * Builds the layout of the AdministrationView.
     */
    private void buildLayout() {
        chaptersTab = new Tab(text("administration.tab.chapters"));
        modelsTab = new Tab(text("administration.tab.models"));
        quizzesTab = new Tab(text("administration.tab.quizzes"));

        chapterListingView = new ChapterListingView(chapterService);
        modelListingView = new ModelListingView(modelService);
        quizListingView = new QuizListingView(quizService);

        chapterListingView.setAdministrationView(true);
        modelListingView.setAdministrationView(true);
        quizListingView.setAdministrationView(true);

        // A count in the tab label, not a badge: a number of chapters is a quantity, not a state.
        chapterListingView.setTotalListener(total -> setTabCount(chaptersTab, "administration.tab.chapters", total));
        modelListingView.setTotalListener(total -> setTabCount(modelsTab, "administration.tab.models", total));
        quizListingView.setTotalListener(total -> setTabCount(quizzesTab, "administration.tab.quizzes", total));

        navigationTabs = new TabSheet();
        navigationTabs.addClassName("admin-tabsheet");
        navigationTabs.add(chaptersTab, chapterListingView);
        navigationTabs.add(modelsTab, modelListingView);
        navigationTabs.add(quizzesTab, quizListingView);
        navigationTabs.setSizeFull();

        Button createButton = new Button(text("button.createChapter"), VaadinIcon.PLUS.create());
        createButton.addClassName("admin-create-button");
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> navigateToCreate());
        navigationTabs.setSuffixComponent(createButton);

        navigationTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = navigationTabs.getSelectedTab();
            if (selectedTab == chaptersTab) {
                setCreateAction(createButton, "button.createChapter");
                chapterListingView.listEntities();
            } else if (selectedTab == modelsTab) {
                setCreateAction(createButton, "button.createModel");
                modelListingView.listEntities();
            } else if (selectedTab == quizzesTab) {
                setCreateAction(createButton, "button.createQuiz");
                quizListingView.listEntities();
            }
        });

        // The suffix button silently relabels itself when the tab changes. Someone listening rather
        // than looking hears the tab name and then nothing, so the new action is announced too.
        actionAnnouncement = new Span();
        actionAnnouncement.getElement().setAttribute("role", "status");
        actionAnnouncement.addClassName(LumoUtility.FontSize.SMALL);
        actionAnnouncement.getStyle()
                .set("position", "absolute")
                .set("width", "1px")
                .set("height", "1px")
                .set("overflow", "hidden")
                .set("clip-path", "inset(50%)");

        pageHeader = new PageHeader(text("page.heading.administrationView"), signedInRole());

        getContent().add(pageHeader, actionAnnouncement, navigationTabs);
        getContent().setSizeFull();
        getContent().setSpacing(true);
        getContent().setPadding(false);
    }

    private void setCreateAction(Button createButton, String labelKey) {
        String label = text(labelKey);
        createButton.setText(label);
        if (actionAnnouncement != null) {
            actionAnnouncement.setText(text("administration.action.changed", label));
        }
    }

    private void setTabCount(Tab tab, String labelKey, long total) {
        tab.setLabel(text(labelKey) + " (" + total + ")");
    }

    /**
     * @return the signed-in user's role, for the header's context line, or {@code null} when it cannot
     *         be determined.
     */
    private String signedInRole() {
        if (currentUserProvider.hasRole("ADMIN")) {
            return text("administration.role.admin");
        }
        if (currentUserProvider.hasRole("TEACHER")) {
            return text("administration.role.teacher");
        }
        return null;
    }

    /**
     * Navigates to the create view based on the selected tab.
     */
    private void navigateToCreate() {
        Tab selectedTab = navigationTabs.getSelectedTab();
        if (selectedTab == chaptersTab) {
            getUI().ifPresent(ui -> ui.navigate("createChapter"));
        } else if (selectedTab == modelsTab) {
            getUI().ifPresent(ui -> ui.navigate("createModel"));
        } else if (selectedTab == quizzesTab) {
            getUI().ifPresent(ui -> ui.navigate("createQuiz"));
        }
    }
}
