package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

import java.util.Map;

/**
 * AbstractModelView, abstract view for displaying and managing 3D models.
 */
@Slf4j
@Scope("prototype")
public abstract class AbstractModelView extends AbstractEntityView {
    protected final ModelUploadForm modelUploadForm;
    private Map<String, QuickModelEntity> quickModelEntity;

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey the key for the page title
     */
    public AbstractModelView(String pageTitleKey) {
        this(pageTitleKey, true);
    }

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey          the key for the page title
     * @param skipBeforeLeaveDialog flag to skip before-leave dialog
     */
    public AbstractModelView(String pageTitleKey, boolean skipBeforeLeaveDialog) {
        super(pageTitleKey, skipBeforeLeaveDialog);
        modelUploadForm = new ModelUploadForm();
        modelUploadForm.setWidthFull();
        entityContent.add(modelUploadForm);
    }
}
