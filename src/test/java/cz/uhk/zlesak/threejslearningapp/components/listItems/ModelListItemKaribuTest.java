package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.ConfirmDialog;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ModelListItemKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ModelService modelService;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void listViewShouldRenderThumbnailAndStoreSelectedModel() {
        QuickModelEntity model = model();
        ModelListItem item = new ModelListItem(model, true, true);
        UI.getCurrent().add(item);

        button(item, "Otevřít").click();

        assertSame(model, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("quickModelEntity"));
    }

    @Test
    void adminActionsShouldStoreModelAndDeleteViaService() {
        when(modelService.delete("model-meta-1")).thenReturn(true);

        QuickModelEntity model = model();
        when(modelService.extractThumbnailDataUrl(model.getDescription())).thenReturn("https://cdn.example.com/thumb.png");
        ModelListItem item = new ModelListItem(model, true, true);
        UI.getCurrent().add(item);

        button(item, "Upravit").click();
        assertSame(model, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("quickModelEntity"));

        button(item, "Smazat").click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);

        _click(_get(Button.class, spec -> spec.withText("Smazat model")));

        assertFalse(dialog.isOpened());
        verify(modelService).delete("model-meta-1");
    }

    private Button button(ModelListItem item, String text) {
        return findAll(item, Button.class).stream()
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder()
                .metadataId("model-meta-1")
                .description("https://cdn.example.com/thumb.png")
                .model(ModelFileEntity.builder()
                        .id("model-file-1")
                        .name("Lebka")
                        .build())
                .build();
    }
}
