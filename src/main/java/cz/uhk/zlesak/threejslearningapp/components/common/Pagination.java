package cz.uhk.zlesak.threejslearningapp.components.common;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

import java.util.function.Consumer;

public class Pagination extends Div {
    private int currentPage;
    @Getter
    private int totalPages;
    private final Consumer<Integer> onPageChange;

    private final Button prevButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
    private final Button nextButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
    private final HorizontalLayout layout = new HorizontalLayout();

    public Pagination(int page, int limit, long totalItems, Consumer<Integer> onPageChange) {
        this.currentPage = page+1;
        this.onPageChange = onPageChange;
        this.totalPages = (int) Math.ceil((double) totalItems / limit);

        layout.setSpacing(true);
        layout.setPadding(false);

        prevButton.addClickListener(e -> goToPage(currentPage - 1));
        nextButton.addClickListener(e -> goToPage(currentPage + 1));

        updateButtons();
        updatePageNumbers();

        add(layout);
    }

    private void updatePageNumbers() {
        layout.removeAll();
        layout.add(prevButton);

        if (totalPages <= 1) {
            layout.add(createPageButton(1));
        } else if (totalPages == 2) {
            layout.add(createPageButton(1));
            layout.add(createPageButton(2));
        } else {
            layout.add(createPageButton(1));
            if (currentPage > 3) {
                layout.add(createEllipsis());
            }
            int start = Math.max(2, currentPage - 1);
            int end = Math.min(totalPages - 1, currentPage + 1);
            for (int i = start; i <= end; i++) {
                if (i == 1 || i == totalPages) continue;
                layout.add(createPageButton(i));
            }
            if (currentPage < totalPages - 2) {
                layout.add(createEllipsis());
            }
            layout.add(createPageButton(totalPages));
        }
        layout.add(nextButton);
    }

    private Div createEllipsis() {
        Div ellipsis = new Div();
        ellipsis.setText("...");
        ellipsis.getStyle().set("padding", "0 8px");
        ellipsis.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return ellipsis;
    }

    private Button createPageButton(int pageNum) {
        Button btn = new Button(String.valueOf(pageNum));
        btn.addClickListener(e -> goToPage(pageNum));
        if (pageNum == currentPage) {
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        btn.setEnabled(pageNum != currentPage);
        return btn;
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages) return;
        this.currentPage = page-1;
        updateButtons();
        updatePageNumbers();
        if (onPageChange != null) {
            onPageChange.accept(currentPage);
        }
    }

    private void updateButtons() {
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }
}
