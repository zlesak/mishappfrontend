package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import cz.uhk.zlesak.threejslearningapp.events.chapter.CreateChapterEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Button for creating a new chapter
 * When clicked, it fires a CreateChapterEvent
 */
@Slf4j
public class CreateChapterButton extends AbstractButton<UI> {
    public CreateChapterButton() {
        super("createChapterButton.label", new CreateChapterEvent(UI.getCurrent()), null, ButtonVariant.LUMO_PRIMARY);
    }
}
