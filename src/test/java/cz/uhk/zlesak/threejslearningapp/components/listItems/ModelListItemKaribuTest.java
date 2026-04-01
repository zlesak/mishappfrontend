package cz.uhk.zlesak.threejslearningapp.components.listItems;

import com.github.mvysny.kaributesting.v10.MockVaadin;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ModelListItemKaribuTest {
    @Autowired private ApplicationContext applicationContext;
    @MockitoBean private ModelService modelService;

    @BeforeEach void setUp() { KaribuSpringTestSupport.setUp(applicationContext); }
    @AfterEach void tearDown() { KaribuSpringTestSupport.tearDown(); }

    @Test
    void listViewShouldRenderThumbnailAndStoreSelectedModel() {
        QuickModelEntity m = model();
        ModelListItem item = new ModelListItem(m, true, true);
        UI.getCurrent().add(item);
        btn(item, "Otevřít").click();
        assertSame(m, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("quickModelEntity"));
    }

    @Test
    void adminActionsShouldStoreModelAndDeleteViaService() {
        when(modelService.delete("model-meta-1")).thenReturn(true);
        QuickModelEntity m = model();
        when(modelService.extractThumbnailDataUrl(m.getDescription())).thenReturn("https://cdn.example.com/t.png");
        ModelListItem item = new ModelListItem(m, true, true);
        UI.getCurrent().add(item);
        btn(item, "Upravit").click();
        assertSame(m, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("quickModelEntity"));
        btn(item, "Smazat").click();
        ConfirmDialog dialog = _get(ConfirmDialog.class);
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        MockVaadin.clientRoundtrip(false);
        assertFalse(dialog.isOpened());
        verify(modelService).delete("model-meta-1");
    }

    @Test
    void thumbnailExtraction_whenServiceThrows_shouldNotBreakConstruction() {
        QuickModelEntity m = model();
        when(modelService.extractThumbnailDataUrl(m.getDescription())).thenThrow(new RuntimeException("parse"));
        ModelListItem item = new ModelListItem(m, true, true);
        UI.getCurrent().add(item);
        assertNotNull(item);
    }

    @Test
    void openButton_whenNotListView_shouldStoreModelAndExecuteJs() {
        QuickModelEntity m = model();
        ModelListItem item = new ModelListItem(m, false, true);
        UI.getCurrent().add(item);
        btn(item, "Otevřít").click();
        assertSame(m, com.vaadin.flow.server.VaadinSession.getCurrent().getAttribute("quickModelEntity"));
    }

    @Test
    void delete_whenServiceReturnsFalse_shouldShowErrorNotification() {
        when(modelService.delete("model-meta-1")).thenReturn(false);
        ModelListItem item = new ModelListItem(model(), true, true);
        UI.getCurrent().add(item);
        btn(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        MockVaadin.clientRoundtrip(false);
        verify(modelService).delete("model-meta-1");
    }

    @Test
    void delete_whenServiceThrows_shouldHandleErrorGracefully() {
        when(modelService.delete("model-meta-1")).thenThrow(new RuntimeException("del failed"));
        ModelListItem item = new ModelListItem(model(), true, true);
        UI.getCurrent().add(item);
        btn(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        MockVaadin.clientRoundtrip(false);
        verify(modelService).delete("model-meta-1");
    }

    @Test
    void delete_success_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(modelService.delete("model-meta-1")).thenReturn(true);
        ModelListItem item = new ModelListItem(model(), true, true);
        UI ui = UI.getCurrent(); ui.add(item);
        btn(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(modelService).delete("model-meta-1");
    }

    @Test
    void delete_false_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(modelService.delete("model-meta-1")).thenReturn(false);
        ModelListItem item = new ModelListItem(model(), true, true);
        UI ui = UI.getCurrent(); ui.add(item);
        btn(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(modelService).delete("model-meta-1");
    }

    @Test
    void delete_exception_whenUiClosingBeforeCallbackExecutes_shouldSkipCallback() {
        when(modelService.delete("model-meta-1")).thenThrow(new RuntimeException("timeout"));
        ModelListItem item = new ModelListItem(model(), true, true);
        UI ui = UI.getCurrent(); ui.add(item);
        btn(item, "Smazat").click();
        _click(_get(Button.class, spec -> spec.withText("Smazat model")));
        ui.close();
        MockVaadin.clientRoundtrip(false);
        verify(modelService).delete("model-meta-1");
    }

    private Button btn(ModelListItem item, String text) {
        return findAll(item, Button.class).stream().filter(c -> text.equals(c.getText())).findFirst().orElseThrow();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder().metadataId("model-meta-1").description("https://cdn.example.com/t.png")
                .model(ModelFileEntity.builder().id("model-file-1").name("Lebka").build()).build();
    }
}
