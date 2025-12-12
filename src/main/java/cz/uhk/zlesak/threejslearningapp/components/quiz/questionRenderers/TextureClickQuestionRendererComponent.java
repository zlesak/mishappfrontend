package cz.uhk.zlesak.threejslearningapp.components.quiz.questionRenderers;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import cz.uhk.zlesak.threejslearningapp.api.clients.AbstractFileApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.TextureClickSubmissionData;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.TextureClickedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderer component for Texture Click type quiz questions.
 */
public class TextureClickQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private String clickedColor = null;
    protected final List<Registration> registrations = new ArrayList<>();
    private final TextureClickQuestionData question;
    private final Div colorPreview = new Div();

    /**
     * Constructor for TextureClickQuestionRendererComponent.
     * @param question the TextureClickQuestionData containing question details
     */
    TextureClickQuestionRendererComponent(TextureClickQuestionData question) {
        this.question = question;
        add(new Span(
                text("quiz.textureClick.instruction") + ": " + question.getQuestionText()
        ));
        Button selectColorButton = new Button(text("Vybrat barvu"));
        String modelUrl = AbstractFileApiClient.getStreamBeEndpointUrl(question.getModelId(), "model");
        String textureUrl = AbstractFileApiClient.getStreamBeEndpointUrl(question.getTextureId(), "texture");
        selectColorButton.addClickListener(e -> ComponentUtil.fireEvent(UI.getCurrent(), new ModelLoadEvent(UI.getCurrent(), modelUrl, textureUrl, question.getModelId(), question.getQuestionId())));

        colorPreview.getStyle()
                .set("width", "100px")
                .set("height", "30px")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px")
                .set("display", "inline-block")
                .set("margin-left", "10px");
        colorPreview.setVisible(false);

        add(selectColorButton);
        add(colorPreview);
    }

    /**
     * Generates submission data based on the user's interaction.
     * @return TextureClickSubmissionData containing the user's selected color and question details
     */
    @Override
    public TextureClickSubmissionData getSubmissionData() {
        return TextureClickSubmissionData.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .hexColor(clickedColor)
                .modelId(question.getModelId())
                .textureId(question.getTextureId())
                .build();
    }

    /**
     * Handles component attachment to the UI.
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                TextureClickedEvent.class,
                event -> {
                    clickedColor = event.getHexColor();
                    colorPreview.getStyle().set("background-color", clickedColor);
                    colorPreview.setVisible(true);
                    answerChangedListener.accept(getSubmissionData());
                }
        ));
    }

    /**
     * Handles component detachment from the UI.
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
