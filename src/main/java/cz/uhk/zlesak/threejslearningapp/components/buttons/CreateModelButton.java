package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelCreateEvent;

/**
 * Button for creating a new 3D model.
 * When clicked, it fires a ModelCreateEvent with the model name and advanced settings flag.
 */
public class CreateModelButton extends AbstractButton<UI> {

    public CreateModelButton(ModelUploadForm modelUploadForm) {
        super("button.createModel", new ModelCreateEvent(UI.getCurrent(), modelUploadForm.getModelName().getValue().trim(), modelUploadForm.getIsAdvanced().getValue()), VaadinIcon.PLUS_CIRCLE, ButtonVariant.LUMO_PRIMARY);
    }
}

