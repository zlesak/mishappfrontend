package cz.uhk.zlesak.threejslearningapp.components.containers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.NoItemInfoComponent;
import cz.uhk.zlesak.threejslearningapp.components.commonComponents.PaginationComponent;
import cz.uhk.zlesak.threejslearningapp.components.listItems.QuizResultListItem;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuizResultFilter;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import cz.uhk.zlesak.threejslearningapp.services.QuizResultService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Embedded panel for showing quiz attempt history without route-based listing view lifecycle.
 */
public class QuizResultsHistoryPanel extends VerticalLayout implements I18nAware {
    private static final int PAGE_SIZE = 10;

    private final QuizResultService quizResultService;
    private final Executor ioExecutor;
    private final String quizId;
    /** The attempts, as a list: a run of attempts is a list, and a screen reader can then say how many. */
    private final UnorderedList itemsLayout = new UnorderedList();
    private final VerticalLayout paginationLayout = new VerticalLayout();
    private int currentPage = 0;

    public QuizResultsHistoryPanel(QuizResultService quizResultService, String quizId) {
        this.quizResultService = quizResultService;
        this.quizId = quizId;
        this.ioExecutor = SpringContextUtils.getBean(Executor.class);

        addClassName("quiz-results-history");
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        addClassNames(LumoUtility.Gap.SMALL);

        // An H2, one level under the route's H1. It was an H3 on a page that had neither, so the
        // heading outline started at level three and read as part of a document that was not there.
        H2 heading = new H2(text("page.info.quizResultsInfo"));
        heading.addClassName(LumoUtility.Margin.NONE);

        itemsLayout.setWidthFull();
        itemsLayout.addClassName("quiz-results-history-items");
        itemsLayout.getStyle()
                .set("list-style", "none")
                .set("margin", "0")
                .set("padding", "0");

        paginationLayout.setWidthFull();
        paginationLayout.setPadding(false);
        paginationLayout.setSpacing(false);
        paginationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationLayout.addClassName("quiz-results-history-pagination");

        add(heading, itemsLayout, paginationLayout);
        itemsLayout.add(new NoItemInfoComponent("page.info.loading"));
    }

    private void loadPage(int page) {
        currentPage = Math.max(0, page);
        itemsLayout.removeAll();
        paginationLayout.removeAll();
        itemsLayout.add(new NoItemInfoComponent("page.info.loading"));

        // Worker vlákno nemá vazbu na Vaadin session, přihlášení je proto nutné přenést ručně.
        SecurityContext capturedSecurityContext = SecurityContextHolder.getContext();

        CompletableFuture
                .supplyAsync(() -> {
                    SecurityContext previous = SecurityContextHolder.getContext();
                    SecurityContextHolder.setContext(capturedSecurityContext);
                    try {
                        return quizResultService.readEntities(buildFilter(currentPage));
                    } finally {
                        SecurityContextHolder.setContext(previous);
                    }
                }, ioExecutor)
                .whenComplete((result, error) -> {
                    UI ui = getUI().orElse(null);
                    if (ui == null || ui.isClosing()) {
                        return;
                    }
                    ui.access(() -> {
                        if (error != null) {
                            itemsLayout.removeAll();
                            itemsLayout.add(new NoItemInfoComponent("notification.apiError.default"));
                            return;
                        }
                        renderPage(result);
                    });
                });
    }

    private FilterParameters<QuizResultFilter> buildFilter(int page) {
        QuizResultFilter filter = QuizResultFilter.builder()
                .Name("")
                .quizId(quizId)
                .build();
        return new FilterParameters<>(PageRequest.of(page, PAGE_SIZE, Sort.Direction.DESC, "Created"), filter);
    }

    private void renderPage(PageResult<QuickQuizResult> pageResult) {
        itemsLayout.removeAll();
        paginationLayout.removeAll();

        List<QuickQuizResult> results = pageResult == null || pageResult.elements() == null
                ? List.of()
                : pageResult.elements().stream().toList();

        if (results.isEmpty()) {
            itemsLayout.add(new NoItemInfoComponent("page.info.noItemsFound"));
            return;
        }

        for (QuickQuizResult result : results) {
            ListItem item = new ListItem(new QuizResultListItem(result, false, quizId));
            item.getStyle().set("width", "100%");
            itemsLayout.add(item);
        }

        PaginationComponent pagination = new PaginationComponent(currentPage, PAGE_SIZE, pageResult.total(), this::loadPage);
        paginationLayout.add(pagination);
    }

    public void renderInitialPage(PageResult<QuickQuizResult> pageResult) {
        currentPage = 0;
        renderPage(pageResult);
    }
}
