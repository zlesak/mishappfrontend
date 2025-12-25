package cz.uhk.zlesak.threejslearningapp.components.buttons;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import cz.uhk.zlesak.threejslearningapp.events.editor.MarkdownModeToggleEvent;

/**
 * A toggle button to switch between Markdown and Editor.js modes.
 * Fires a MarkdownModeToggleEvent with the current mode when clicked.
 */
public class MarkdownToggleButton extends AbstractButton<UI> {
    private boolean markdownMode = false;

    public MarkdownToggleButton() {
        super("markdownToggleButton.label.md", null, VaadinIcon.CODE, ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

        addClickListener(e -> changeMode());
        getElement().setProperty("title", text("markdownToggleButton.mdModeTooltip"));
    }

    private void changeMode() {
        markdownMode = !markdownMode;
        ComponentUtil.fireEvent(UI.getCurrent(), new MarkdownModeToggleEvent(UI.getCurrent(), markdownMode));
        if (markdownMode) {
            setText(text("markdownToggleButton.label.editorjs"));
            setIcon(new Icon(VaadinIcon.FILE_TEXT_O));
            getElement().setProperty("title", text("markdownToggleButton.editorjsToolTip"));
        } else {
            setText(text("markdownToggleButton.label.md"));
            setIcon(new Icon(VaadinIcon.CODE));
            getElement().setProperty("title", text("markdownToggleButton.mdModeTooltip"));
        }
    }
}
