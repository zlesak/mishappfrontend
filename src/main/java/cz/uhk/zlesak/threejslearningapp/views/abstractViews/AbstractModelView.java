package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.components.forms.ModelUploadForm;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

/**
 * AbstractModelView, abstract view for displaying and managing 3D models.
 */
@Slf4j
@Scope("prototype")
public abstract class AbstractModelView extends AbstractEntityView<ModelService> {
    protected final ModelUploadForm modelUploadForm = new ModelUploadForm();

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey the key for the page title
     */
    public AbstractModelView(String pageTitleKey) {
        super(pageTitleKey, SpringContextUtils.getBean(ModelService.class));
    }

    /**
     * Constructor for AbstractModelView.
     *
     * @param pageTitleKey          the key for the page title
     * @param skipBeforeLeaveDialog flag to skip before-leave dialog
     * @param service               the model service for handling model operations
     */
    public AbstractModelView(String pageTitleKey, boolean skipBeforeLeaveDialog, ModelService service) {
        super(pageTitleKey, skipBeforeLeaveDialog, service);
        modelUploadForm.setWidthFull();
        entityContent.add(modelUploadForm);
    }
}
