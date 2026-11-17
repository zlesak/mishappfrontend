package cz.uhk.zlesak.threejslearningapp.components.notifications;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;

/**
 * Tells the user which cookies the application stores and lets them decline the optional one.
 *
 * <p>Two cookies exist: the session cookie carrying the login, and {@code themeMode}, written only
 * when the user themselves switches the colour scheme. Declining keeps the chosen scheme for the
 * current session and simply does not remember it for the next one.
 *
 * <p>Accept and decline carry the same visual weight on purpose. A decline hidden behind a link, or
 * styled as the lesser option, is not a free choice — and a consent control has to be operable and
 * understandable like any other.
 */
public class CookiesNotification extends Notification implements I18nAware {

    /** Records the answer, so the bar is shown once rather than on every visit. */
    private static final String CONSENT_COOKIE = "cookieConsent";

    /**
     * Constructor - shows the notice with an accept and a decline action.
     */
    public CookiesNotification() {
        super();
        Span message = new Span(text("notification.cookieConsent"));

        setPosition(Notification.Position.BOTTOM_CENTER);
        setDuration(0);

        Button acceptButton = new Button(text("notification.cookies.accept"), event -> answer("accepted"));
        acceptButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button declineButton = new Button(text("notification.cookies.decline"), event -> answer("rejected"));
        // Same size and prominence as accept; only the fill differs, so neither reads as the
        // expected answer.
        declineButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout layout = new HorizontalLayout(message, declineButton, acceptButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        add(layout);

        // The bar appears without the user asking for it, so it must not have to be hunted for:
        // focus moves into it as soon as it opens.
        addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                acceptButton.focus();
            }
        });
    }

    /**
     * Records the answer and closes the bar.
     *
     * @param answer {@code accepted} or {@code rejected}.
     */
    private void answer(String answer) {
        UI.getCurrent().getPage().executeJs(
                "document.cookie = $0 + '=' + $1 + '; path=/; max-age=31536000; SameSite=Lax';",
                CONSENT_COOKIE, answer);
        close();
    }
}
