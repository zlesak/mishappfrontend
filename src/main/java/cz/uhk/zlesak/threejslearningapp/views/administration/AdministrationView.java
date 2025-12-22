package cz.uhk.zlesak.threejslearningapp.views.administration;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
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

@Slf4j
@Route("administration")
@Tag("administration-view")
@Scope("prototype")
@RolesAllowed({"ADMIN", "TEACHER"})
public class AdministrationView extends AbstractView {

    private final ChapterService chapterService;
    private final ModelService modelService;
    private final QuizService quizService;

    private TabSheet navigationTabs;

    private Tab chaptersTab;
    private Tab modelsTab;
    private Tab quizzesTab;

    private ChapterListingView chapterListingView;
    private ModelListingView modelListingView;
    private QuizListingView quizListingView;

    @Autowired
    public AdministrationView(ChapterService chapterService, ModelService modelService, QuizService quizService) {
        super("page.title.administrationView");
        this.chapterService = chapterService;
        this.modelService = modelService;
        this.quizService = quizService;

        buildLayout();
    }

    private void buildLayout() {
        chaptersTab = new Tab(text("administration.tab.chapters"));
        modelsTab = new Tab(text("administration.tab.models"));
        quizzesTab = new Tab(text("administration.tab.quizzes"));

        chapterListingView = new ChapterListingView(chapterService);
        modelListingView = new ModelListingView(modelService);
        quizListingView = new QuizListingView(quizService);

        navigationTabs = new TabSheet();
        navigationTabs.add(chaptersTab, chapterListingView);
        navigationTabs.add(modelsTab, modelListingView);
        navigationTabs.add(quizzesTab, quizListingView);
        navigationTabs.setWidthFull();


        Button createButton = new Button(text("button.createChapter"), VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClickListener(e -> navigateToCreate());
        navigationTabs.setSuffixComponent(createButton);

        navigationTabs.addSelectedChangeListener(event -> {
            Tab selectedTab = navigationTabs.getSelectedTab();
            if (selectedTab == chaptersTab) {
                createButton.setText(text("button.createChapter"));
                chapterListingView.listEntities();
            } else if (selectedTab == modelsTab) {
                createButton.setText(text("button.createModel"));
                modelListingView.listEntities();
            } else if (selectedTab == quizzesTab) {
                createButton.setText(text("button.createQuiz"));
                quizListingView.listEntities();
            }
        });

        getContent().add(navigationTabs);
        getContent().setSizeFull();
    }

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

