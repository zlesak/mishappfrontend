package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import cz.uhk.zlesak.threejslearningapp.events.quiz.CreateQuizEvent;
import cz.uhk.zlesak.threejslearningapp.i18n.I18nAware;
import lombok.extern.slf4j.Slf4j;

/**
 * Button for creating a new quiz
 * When clicked, it fires a CreateQuizEvent
 */
@Slf4j
public class CreateQuizButton extends Button implements I18nAware {
    public CreateQuizButton() {
        super();
        setText(text("createQuizButton.label"));
        addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addClickListener(e -> ComponentUtil.fireEvent(UI.getCurrent(), new CreateQuizEvent(UI.getCurrent())));
    }
}

