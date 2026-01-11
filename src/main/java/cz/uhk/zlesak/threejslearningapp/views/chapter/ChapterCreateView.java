package cz.uhk.zlesak.threejslearningapp.views.chapter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import cz.uhk.zlesak.threejslearningapp.components.forms.CreateChapterForm;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.CreateChapterEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractChapterView;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Scope;

import java.util.Map;

/**
 * ChapterCreateView for creating a new chapter.
 * Extends AbstractChapterView and provides functionality to create chapters.
 */
@Slf4j
@Route("createChapter")
@Tag("create-chapter")
@Scope("prototype")
@RolesAllowed(value = "TEACHER")
public class ChapterCreateView extends AbstractChapterView {

    /**
     * Constructor for CreateChapterView.
     * Initializes the view with necessary services for creating chapters.
     *
     * @param chapterService service for handling chapter-related operations
     */
    @Autowired
    public ChapterCreateView(ChapterService chapterService) {
        super("page.title.createChapterView", true, false, chapterService);
        configureVisibility();
        setupChapterForm();
    }

    /**
     * Configures visibility of inherited components.
     */
    private void configureVisibility() {
        searchTextField.setVisible(false);
        editorjs.toggleReadOnlyMode(false);
    }

    /**
     * Sets up the chapter creation form.
     */
    private void setupChapterForm() {
        CreateChapterForm createChapterForm = new CreateChapterForm(editorjs, mdEditor);
        entityContent.add(createChapterForm);
        secondaryNavigation.init(editorjs);
    }

    /**
     * Handles the CreateChapterEvent.
     * Retrieves data from the editor and creates a new chapter.
     * Upon successful creation, navigates to the newly created chapter view.
     *
     * @param event the create chapter event
     */
    private void createChapterConsumer(CreateChapterEvent event) {
        try {
            secondaryNavigation.setMainContentTabSelected();
            Map<String, QuickModelEntity> allModels = secondaryNavigation.getModelsScroller().getAllModelsMappedToChapterHeaderBlockId();
            retrieveEditorDataAndCreateChapter(allModels);
        } catch (ApplicationContextException ex) {
            log.error("Chapter creation error: {}", ex.getMessage(), ex);
            new ErrorNotification(text("error.chapterSaveFailed") + ": " + ex.getMessage(), 5000);
        } catch (Exception ex) {
            log.error("Unexpected error during chapter creation: {}", ex.getMessage(), ex);
            new ErrorNotification(text("error.unexpectedChapterCreationError") + ": " + ex.getMessage(), 5000);
        }
    }

    /**
     * Retrieves editor data and creates the chapter.
     *
     * @param allModels map of all selected models
     */
    private void retrieveEditorDataAndCreateChapter(Map<String, QuickModelEntity> allModels) {
        editorjs.getData()
                .whenComplete((bodyData, error) -> {
                    if (error != null) {
                        log.error("Error retrieving editor data: {}", error.getMessage(), error);
                        throw new ApplicationContextException(text("error.editorDataRetrievalFailed") + ": " + error.getMessage());
                    }

                    createChapterAndNavigate(bodyData, allModels);
                });
    }

    /**
     * Creates the chapter and navigates to it.
     *
     * @param bodyData  the chapter content
     * @param allModels map of all selected models
     */
    private void createChapterAndNavigate(String bodyData, Map<String, QuickModelEntity> allModels) {
        try {
            String chapterId = service.create(
                    ChapterEntity.builder()
                            .name(nameTextField.getValue().trim())
                            .modelHeaderMap(allModels)
                            .content(bodyData)
                            .models(allModels.values().stream().toList())
                            .build()).getId();

            skipBeforeLeaveDialog = true;
            UI.getCurrent().navigate("chapter/" + chapterId);
        } catch (Exception e) {
            log.error("Error creating chapter: {}", e.getMessage(), e);
            throw new ApplicationContextException(text("error.chapterCreationFailed") + ": " + e.getMessage(), e);
        }
    }

    /**
     * Handles component attachment to the UI.
     * Registers a listener for CreateChapterEvent.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                CreateChapterEvent.class,
                this::createChapterConsumer
        ));

        registrations.add(ComponentUtil.addListener(
                attachEvent.getUI(),
                ModelSelectedFromDialogEvent.class,
                event -> {
                    secondaryNavigation.getModelsScroller().updateModelSelect(
                            event.getBlockId(),
                            event.getSelectedModel()
                    );
                    loadSingleModelWithTextures(event.getSelectedModel(), event.getBlockId(), event.getSelectedModel().getModel().getId(), true);
                }
        ));
    }
}

