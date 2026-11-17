package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The foot of a listing: how much of the result set is on screen and how to reach the rest.
 *
 * <p>Left: the count, and how many items to show at a time. Right: the pages. The two belong at
 * opposite ends because they answer different questions — where am I, and where can I go.
 */
public class PaginationComponent extends Nav implements I18nAware {

    /** WCAG 2.5.8 asks for at least 24 × 24 CSS px; touch guidance asks for more. */
    private static final String MIN_TARGET = "44px";

    /** Offered page sizes. Ten is the default the listing starts with. */
    private static final List<Integer> PAGE_SIZES = List.of(10, 20, 50, 100);

    private int currentPage;
    @Getter
    private int totalPages;
    private final int pageSize;
    private final long totalItems;
    private final Consumer<Integer> onPageChange;
    /** Notified when the user chooses a different page size. */
    @Setter
    private IntConsumer onPageSizeChange;

    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final HorizontalLayout pageButtons = new HorizontalLayout();
    private final Span rangeLabel = new Span();

    /**
     * Constructs the pagination component.
     *
     * @param page         zero-based current page index
     * @param limit        number of items per page
     * @param totalItems   total number of items in the result set
     * @param onPageChange callback invoked with the new zero-based page index on navigation
     */
    public PaginationComponent(int page, int limit, long totalItems, Consumer<Integer> onPageChange) {
        this.currentPage = page + 1;
        this.pageSize = limit;
        this.totalItems = totalItems;
        this.onPageChange = onPageChange;
        this.totalPages = (int) Math.ceil((double) totalItems / limit);

        // A run of numbered links is a navigation region; without a name a screen reader lists it as
        // one anonymous "navigation" among the others.
        getElement().setAttribute("aria-label", text("pagination.label"));
        addClassName("listing-pagination-bar");
        getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("gap", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("width", "100%");

        prevButton.setAriaLabel(text("pagination.previous"));
        nextButton.setAriaLabel(text("pagination.next"));
        sizeTarget(prevButton);
        sizeTarget(nextButton);

        prevButton.addClickListener(e -> goToPage(currentPage - 1));
        nextButton.addClickListener(e -> goToPage(currentPage + 1));

        pageButtons.setSpacing(true);
        pageButtons.setPadding(false);
        pageButtons.setWrap(true);
        pageButtons.setAlignItems(FlexComponent.Alignment.CENTER);

        updateButtons();
        updatePageNumbers();

        add(createStatusSide(), pageButtons);
    }

    /**
     * The left-hand side: what is on screen, and how much of it to show.
     *
     * @return the layout
     */
    private HorizontalLayout createStatusSide() {
        rangeLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        rangeLabel.setText(rangeText());

        Select<Integer> pageSizeSelect = new Select<>();
        pageSizeSelect.setItems(PAGE_SIZES);
        pageSizeSelect.setValue(PAGE_SIZES.contains(pageSize) ? pageSize : PAGE_SIZES.getFirst());
        pageSizeSelect.setLabel(text("pagination.pageSize"));
        pageSizeSelect.addClassName("listing-page-size");
        pageSizeSelect.setWidth("7.5rem");
        pageSizeSelect.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue() != null && onPageSizeChange != null) {
                onPageSizeChange.accept(event.getValue());
            }
        });

        HorizontalLayout side = new HorizontalLayout(pageSizeSelect, rangeLabel);
        side.setPadding(false);
        side.setSpacing(true);
        side.setAlignItems(FlexComponent.Alignment.CENTER);
        side.setWrap(true);
        return side;
    }

    /**
     * @return "Zobrazeno 11–20 z 42", or the empty-result wording when there is nothing to show.
     */
    private String rangeText() {
        if (totalItems <= 0) {
            return text("listing.meta.empty");
        }
        long first = (long) (currentPage - 1) * pageSize + 1;
        // Deleting the last item of the last page leaves the listing on a page that is now past
        // the end. There is no range to state there, and stating one anyway read backwards --
        // "21-11 of 11" -- so the count is given on its own instead.
        if (first > totalItems) {
            return text("pagination.rangeEmptyPage", totalItems);
        }
        long last = Math.min((long) currentPage * pageSize, totalItems);
        return text("pagination.range", first, last, totalItems);
    }

    private void updatePageNumbers() {
        pageButtons.removeAll();
        pageButtons.add(prevButton);

        if (totalPages <= 1) {
            pageButtons.add(createPageButton(1));
        } else if (totalPages == 2) {
            pageButtons.add(createPageButton(1));
            pageButtons.add(createPageButton(2));
        } else {
            pageButtons.add(createPageButton(1));
            if (currentPage > 3) {
                pageButtons.add(createEllipsis());
            }
            int start = Math.max(2, currentPage - 1);
            int end = Math.min(totalPages - 1, currentPage + 1);
            for (int i = start; i <= end; i++) {
                if (i == 1 || i == totalPages) continue;
                pageButtons.add(createPageButton(i));
            }
            if (currentPage < totalPages - 2) {
                pageButtons.add(createEllipsis());
            }
            pageButtons.add(createPageButton(totalPages));
        }
        pageButtons.add(nextButton);
        rangeLabel.setText(rangeText());
    }

    private Div createEllipsis() {
        Div ellipsis = new Div();
        ellipsis.setText("...");
        ellipsis.getStyle().set("padding", "0 8px");
        ellipsis.getStyle().set("color", "var(--lumo-secondary-text-color)");
        // Three dots read aloud as "dot dot dot" tell the listener nothing.
        ellipsis.getElement().setAttribute("aria-hidden", "true");
        return ellipsis;
    }

    private Button createPageButton(int pageNum) {
        Button btn = new Button(String.valueOf(pageNum));
        btn.addClickListener(e -> goToPage(pageNum));
        sizeTarget(btn);
        if (pageNum == currentPage) {
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            // Marked as current rather than disabled. Disabling drops the button out of the tab order,
            // so a keyboard user tabbing through the pager skips over the page they are on and loses
            // their place in the sequence.
            btn.getElement().setAttribute("aria-current", "page");
            btn.setAriaLabel(text("pagination.current", pageNum));
        } else {
            btn.setAriaLabel(text("pagination.page", pageNum));
        }
        return btn;
    }

    private void sizeTarget(Button button) {
        button.getStyle()
                .set("min-width", MIN_TARGET)
                .set("min-height", MIN_TARGET);
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages || page == currentPage) return;
        this.currentPage = page;
        updateButtons();
        updatePageNumbers();
        if (onPageChange != null) {
            onPageChange.accept(currentPage - 1);
        }
    }

    private void updateButtons() {
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }
}
