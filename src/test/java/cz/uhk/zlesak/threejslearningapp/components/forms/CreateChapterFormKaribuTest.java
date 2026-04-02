package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class CreateChapterFormKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void constructor_shouldComposeUploadAndCreateActions() {
        CreateChapterForm form = new CreateChapterForm(new EditorJs(true));
        UI.getCurrent().add(form);

        Button uploadedButton = (Button) form.getComponentAt(0);

        assertSame(form.getMoodleZipFileUpload(), form.getComponentAt(1));
        assertSame(form.getCreateChapterButton(), form.getComponentAt(2));
        assertEquals("Vytvořit kapitolu", form.getCreateChapterButton().getText());
        assertFalse(uploadedButton.isVisible());
        assertEquals(FlexComponent.Alignment.STRETCH, form.getAlignItems());
    }
}

