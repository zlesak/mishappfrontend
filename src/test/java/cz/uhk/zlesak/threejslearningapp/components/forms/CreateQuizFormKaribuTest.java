package cz.uhk.zlesak.threejslearningapp.components.forms;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs.ChapterListDialog;
import cz.uhk.zlesak.threejslearningapp.components.listItems.ChapterListItem;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuestionTypeEnum;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.mvysny.kaributesting.v10.LocatorJ._assert;
import static com.github.mvysny.kaributesting.v10.LocatorJ._assertOne;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class CreateQuizFormKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(
                ChapterEntity.builder().id("chapter-1").name("Anatomie").build()
        ), 1L, 0));
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void constructor_shouldInitializeDefaultQuizInputs() {
        CreateQuizForm form = new CreateQuizForm();
        UI.getCurrent().add(form);

        assertEquals(QuestionTypeEnum.SINGLE_CHOICE, form.getQuestionTypeSelect().getValue());
        assertEquals(0, form.getTimeLimit());
        assertEquals("Vybrat kapitolu", form.getChooseChapterButton().getText());
        assertEquals("Vytvořit kvíz", form.getSaveQuizButton().getText());
    }

    @Test
    void addQuestionListener_shouldReceiveSelectedType() {
        CreateQuizForm form = new CreateQuizForm();
        UI.getCurrent().add(form);
        AtomicReference<QuestionTypeEnum> selectedType = new AtomicReference<>();

        form.getQuestionTypeSelect().setValue(QuestionTypeEnum.MATCHING);
        form.setAddQuestionListener(selectedType::set);
        form.getAddQuestionButton().click();

        assertEquals(QuestionTypeEnum.MATCHING, selectedType.get());
    }

    @Test
    void getQuestionTypeLabel_shouldReturnLocalizedValuesForAllSupportedTypes() {
        CreateQuizForm form = new CreateQuizForm();
        UI.getCurrent().add(form);

        assertEquals("Jedna správná odpověď", form.getQuestionTypeLabel(QuestionTypeEnum.SINGLE_CHOICE));
        assertEquals("Více správných odpovědí", form.getQuestionTypeLabel(QuestionTypeEnum.MULTIPLE_CHOICE));
        assertEquals("Otevřená odpověď", form.getQuestionTypeLabel(QuestionTypeEnum.OPEN_TEXT));
        assertEquals("Přiřazování", form.getQuestionTypeLabel(QuestionTypeEnum.MATCHING));
        assertEquals("Seřazování", form.getQuestionTypeLabel(QuestionTypeEnum.ORDERING));
        assertEquals("Kliknutí na texturu", form.getQuestionTypeLabel(QuestionTypeEnum.TEXTURE_CLICK));
    }

    @Test
    void chooseChapterButton_shouldOpenSelectionDialog() {
        CreateQuizForm form = new CreateQuizForm();
        UI.getCurrent().add(form);

        form.getChooseChapterButton().click();

        _assertOne(ChapterListDialog.class);
        _assert(ChapterListItem.class, 1);
    }

    @Test
    void setQuizData_shouldPopulateFieldsAndSelectedChapter() {
        CreateQuizForm form = new CreateQuizForm();
        UI.getCurrent().add(form);
        ChapterEntity chapter = ChapterEntity.builder().id("chapter-1").name("Anatomie").build();

        form.setQuizData("Kvíz kostra", "Procvičování kostí", 15, chapter);

        assertEquals("Kvíz kostra", form.getName());
        assertEquals("Procvičování kostí", form.getDescription());
        assertEquals(15, form.getTimeLimit());
        assertEquals("chapter-1", form.getSelectedChapter());
    }
}
