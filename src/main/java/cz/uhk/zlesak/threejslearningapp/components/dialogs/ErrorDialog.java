package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class ErrorDialog extends Div {

    public ErrorDialog(VaadinIcon icon, String title, String message, String details) {
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("align-items", "center");
        getStyle().set("justify-content", "center");
        setHeight("100%");
        getStyle().set("text-align", "center");
        getStyle().set("margin", "auto");

        Icon errorIcon = new Icon(icon);
        errorIcon.setSize("100px");
        errorIcon.getStyle().set("color", "var(--lumo-error-color)");

        Button backButton = new Button("Zpět na hlavní stránku", new Icon(VaadinIcon.HOME));
        backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backButton.addClickListener(e -> backButton.getUI().ifPresent(ui -> ui.navigate("")));

        add(errorIcon, new H1(title), new Paragraph(message), new Paragraph(details), backButton);
    }
}
