package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * A button that toggles between light and dark theme.
 * Persists choice in a cookie (themeMode=light|dark) for 1 year.
 */
public class ThemeModeToggleButton extends Button {

    public ThemeModeToggleButton() {
        super();
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM, LumoUtility.Margin.MEDIUM, LumoUtility.Padding.MEDIUM);
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getElement().getClassList().add("theme-mode-toggle");
        addClickListener(e -> toggleTheme());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Initialize from cookie
        UI.getCurrent().getPage().executeJs(
                "const match = document.cookie.match('(^|;) ?themeMode=([^;]*)(;|$)'); return match ? match[2] : null;"
        ).then(String.class, value -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if ("dark".equals(value)) {
                themeList.add(Lumo.DARK);
                setIcon(VaadinIcon.SUN_O.create());
            } else {
                themeList.remove(Lumo.DARK);
                setIcon(VaadinIcon.MOON.create());
            }
        });
    }

    private void toggleTheme() {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        String mode;
        if (themeList.contains(Lumo.DARK)) {
            themeList.remove(Lumo.DARK);
            setIcon(VaadinIcon.MOON.create());
            mode = "light";
        } else {
            themeList.add(Lumo.DARK);
            setIcon(VaadinIcon.SUN_O.create());
            mode = "dark";
        }
        UI.getCurrent().getPage().executeJs(
                "document.cookie = 'themeMode=' + $0 + '; path=/; max-age=31536000';", mode);
    }
}

