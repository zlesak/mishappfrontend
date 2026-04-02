package cz.uhk.zlesak.threejslearningapp.views.model;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFileEntity;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ModelDetailViewKaribuTest {
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
    void shouldForwardToListingWhenModelIdMissing() {
        ModelDetailView view = new ModelDetailView();
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters());

        view.beforeEnter(event);

        verify(event).forwardTo(ModelListingView.class);
    }

    @Test
    void shouldLoadModelAfterNavigation() {
        when(modelService.read("model-1")).thenReturn(model());

        ModelDetailView view = new ModelDetailView();
        UI.getCurrent().add(view);
        view.afterNavigation(afterNavigationEvent());

        verify(modelService).read("model-1");
        assertEquals("MISH - Model", view.getPageTitle());
    }

    @Test
    void shouldForwardToListingWhenModelIdParameterPresentButMissing() {
        ModelDetailView view = new ModelDetailView();
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        when(event.getRouteParameters()).thenReturn(new RouteParameters("otherId", "someValue"));

        view.beforeEnter(event);

        verify(event).forwardTo(ModelListingView.class);
    }

    @Test
    void afterNavigation_whenModelIsNull_shouldHandleGracefully() {
        when(modelService.read("model-1")).thenReturn(null);

        ModelDetailView view = new ModelDetailView();
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.afterNavigation(afterNavigationEvent()));
        verify(modelService).read("model-1");
    }

    @Test
    void afterNavigation_whenServiceThrows_shouldNotPropagateException() {
        when(modelService.read("model-1")).thenThrow(new RuntimeException("backend error"));

        ModelDetailView view = new ModelDetailView();
        UI.getCurrent().add(view);

        assertDoesNotThrow(() -> view.afterNavigation(afterNavigationEvent()));
        verify(modelService).read("model-1");
    }

    private AfterNavigationEvent afterNavigationEvent() {
        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("modelId", "model-1"));
        return event;
    }

    private ModelEntity model() {
        return ModelEntity.builder()
                .metadataId("model-1")
                .model(ModelFileEntity.builder()
                        .id("file-1")
                        .name("Lebka")
                        .related(List.of())
                        .build())
                .otherTextures(List.of())
                .build();
    }
}
