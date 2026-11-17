package cz.uhk.zlesak.threejslearningapp.components.quizComponents.questionRenderers;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.TextureMapHelper;
import cz.uhk.zlesak.threejslearningapp.domain.model.FileSenseType;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.question.TextureClickQuestionData;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.submission.TextureClickSubmissionData;
import cz.uhk.zlesak.threejslearningapp.domain.texture.QuickTextureEntity;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureAreaForSelect;
import cz.uhk.zlesak.threejslearningapp.events.file.FileType;
import cz.uhk.zlesak.threejslearningapp.events.file.UploadFileEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelLoadEvent;
import cz.uhk.zlesak.threejslearningapp.events.quiz.TextureClickedEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer component for Texture Click type quiz questions.
 *
 * <p>Answering used to require a mouse and nothing else: the only control was a button that loaded the
 * model, and the answer came from clicking into the 3D scene. There was no way to answer this kind of
 * question from a keyboard at all, which made every quiz containing one impossible to finish.
 *
 * <p>The second route is a list of the texture's named areas. The mapping already exists — the CSV
 * shipped with a texture pairs an area name with the hex colour the scene reports on a click — so no
 * new data is needed. Both routes end in the same {@code clickedColor} and fire the same listener, and
 * each reflects what the other did.
 */
public class TextureClickQuestionRendererComponent extends AbstractQuestionRendererComponent {
    private String clickedColor = null;
    protected final List<Registration> registrations = new ArrayList<>();
    private final TextureClickQuestionData question;
    private final Div colorPreview = new Div();
    private final Span selectedAreaName = new Span();
    private final Select<TextureAreaForSelect> areaSelect = new Select<>();
    private final Map<String, TextureAreaForSelect> areasByColor = new LinkedHashMap<>();
    private boolean syncingFromScene = false;

    /**
     * Constructor for TextureClickQuestionRendererComponent.
     *
     * @param question the TextureClickQuestionData containing question details
     */
    TextureClickQuestionRendererComponent(TextureClickQuestionData question) {
        this.question = question;
        Button selectColorButton = new Button(text("quiz.choose.color"));
        selectColorButton.addClickListener(e -> {

                    var quickModelEntity = QuickModelEntity.builder()
                            .id(question.getModelId())
                            .model(ModelFileEntity.builder()
                                    .id(question.getModelId())
                                    .related(List.of(
                                            ModelFileEntity.builder()
                                                    .id(question.getTextureId())
                                                    .senseType(FileSenseType.OTHER_TEXTURE)
                                                    .build()
                                    ))
                                    .build())
                            .otherTextures(List.of(
                                    QuickTextureEntity.builder().id(question.getTextureId()).build()
                            ))
                            .isAdvanced(true)
                            .build();

                    ComponentUtil.fireEvent(UI.getCurrent(), new ModelLoadEvent(UI.getCurrent(), quickModelEntity, question.getQuestionId()));
                }
        );

        areaSelect.setLabel(text("quiz.textureClick.area.label"));
        areaSelect.setHelperText(text("quiz.textureClick.area.helper"));
        areaSelect.setItemLabelGenerator(area -> area == null || area.areaName() == null ? "" : area.areaName());
        areaSelect.setEmptySelectionAllowed(true);
        areaSelect.setVisible(false);
        areaSelect.addValueChangeListener(event -> {
            if (syncingFromScene) {
                return;
            }
            TextureAreaForSelect area = event.getValue();
            if (area == null || area.hexColor() == null) {
                return;
            }
            applyAnswer(area.hexColor());
        });

        colorPreview.getStyle()
                .set("width", "100px")
                .set("height", "30px")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px")
                .set("display", "inline-block")
                .set("flex", "0 0 auto");
        // A coloured rectangle says nothing to anyone who cannot see it, or who cannot tell the colour
        // apart from its neighbours. The name of the chosen area beside it says what was chosen.
        colorPreview.getElement().setAttribute("aria-hidden", "true");
        selectedAreaName.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        HorizontalLayout answerRow = new HorizontalLayout(colorPreview, selectedAreaName);
        answerRow.setAlignItems(HorizontalLayout.Alignment.CENTER);
        answerRow.setPadding(false);
        answerRow.setSpacing(true);
        answerRow.getElement().setAttribute("role", "status");

        add(selectColorButton);
        add(areaSelect);
        add(answerRow);
    }

    /**
     * Records an answer and shows it, whichever route it arrived by.
     *
     * @param hexColor the colour of the chosen area
     */
    private void applyAnswer(String hexColor) {
        clickedColor = hexColor;
        colorPreview.getStyle().set("background-color", hexColor);
        colorPreview.setVisible(true);

        TextureAreaForSelect area = areasByColor.get(normalizeColor(hexColor));
        selectedAreaName.setText(area != null && area.areaName() != null
                ? area.areaName()
                : text("quiz.textureClick.area.unnamed", hexColor));

        if (answerChangedListener != null) {
            answerChangedListener.accept(getSubmissionData());
        }
    }

    /**
     * Generates submission data based on the user's interaction.
     *
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
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                TextureClickedEvent.class,
                event -> {
                    if (!event.getQuestionId().equals(question.getQuestionId())) return;
                    applyAnswer(event.getHexColor());
                    selectAreaWithoutEcho(event.getHexColor());
                }
        ));

        // The area names ride along with the texture's CSV, which arrives once the model is loaded.
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                UploadFileEvent.class,
                event -> {
                    if (event.getFileType() != FileType.CSV) {
                        return;
                    }
                    if (event.getQuestionId() != null && !event.getQuestionId().equals(question.getQuestionId())) {
                        return;
                    }
                    addAreas(event);
                }
        ));
    }

    private void addAreas(UploadFileEvent event) {
        List<TextureAreaForSelect> parsed = new ArrayList<>();
        TextureMapHelper.csvParse(event.getModelId(), event.getBase64File(), parsed, event.getEntityId());

        for (TextureAreaForSelect area : parsed) {
            if (area.hexColor() == null || area.areaName() == null || area.areaName().isBlank()) {
                continue;
            }
            areasByColor.putIfAbsent(normalizeColor(area.hexColor()), area);
        }

        if (areasByColor.isEmpty()) {
            return;
        }
        areaSelect.setItems(areasByColor.values());
        areaSelect.setVisible(true);
        if (clickedColor != null) {
            selectAreaWithoutEcho(clickedColor);
        }
    }

    /**
     * Mirrors a scene click into the select without letting the select fire the answer again.
     *
     * @param hexColor the colour that was clicked
     */
    private void selectAreaWithoutEcho(String hexColor) {
        TextureAreaForSelect area = areasByColor.get(normalizeColor(hexColor));
        if (area == null) {
            return;
        }
        syncingFromScene = true;
        try {
            areaSelect.setValue(area);
        } finally {
            syncingFromScene = false;
        }
    }

    private static String normalizeColor(String hexColor) {
        return hexColor == null ? "" : hexColor.trim().toLowerCase();
    }

    /**
     * Handles component detachment from the UI.
     *
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
