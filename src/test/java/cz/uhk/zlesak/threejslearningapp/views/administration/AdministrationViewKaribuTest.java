package cz.uhk.zlesak.threejslearningapp.views.administration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.tabs.TabSheet;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.QuickQuizEntity;
import cz.uhk.zlesak.threejslearningapp.services.ChapterService;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import cz.uhk.zlesak.threejslearningapp.services.QuizService;
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

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findButtonByText;
import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.findFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class AdministrationViewKaribuTest {
    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ChapterService chapterService;

    @MockitoBean
    private ModelService modelService;

    @MockitoBean
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        when(chapterService.readEntities(any())).thenReturn(new PageResult<>(List.of(chapter()), 1L, 0));
        when(modelService.readEntities(any())).thenReturn(new PageResult<>(List.of(model()), 1L, 0));
        when(quizService.readEntities(any())).thenReturn(new PageResult<>(List.of(quiz()), 1L, 0));
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void selectedTabShouldDriveCreateButtonTextAndListingRefresh() {
        AdministrationView view = new AdministrationView(chapterService, modelService, quizService);
        UI.getCurrent().add(view);

        TabSheet tabSheet = findFirst(view, TabSheet.class);
        Button createChapterButton = findButtonByText(view, "Vytvořit kapitolu");

        assertEquals(0, tabSheet.getSelectedIndex());
        assertEquals("Vytvořit kapitolu", createChapterButton.getText());

        tabSheet.setSelectedIndex(1);

        assertEquals("Vytvořit model", createChapterButton.getText());
        verify(modelService, atLeastOnce()).readEntities(any());

        tabSheet.setSelectedIndex(2);

        assertEquals("Vytvořit kvíz", createChapterButton.getText());
        verify(quizService, atLeastOnce()).readEntities(any());

        tabSheet.setSelectedIndex(0);

        assertEquals("Vytvořit kapitolu", createChapterButton.getText());
        verify(chapterService, atLeastOnce()).readEntities(any());
    }

    private ChapterEntity chapter() {
        return ChapterEntity.builder()
                .id("chapter-1")
                .name("Kosti")
                .creatorId("teacher")
                .build();
    }

    private QuickModelEntity model() {
        return QuickModelEntity.builder()
                .metadataId("model-1")
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").build())
                .build();
    }

    private QuickQuizEntity quiz() {
        return QuickQuizEntity.builder()
                .id("quiz-1")
                .name("Lebka test")
                .timeLimit(10)
                .build();
    }
}
