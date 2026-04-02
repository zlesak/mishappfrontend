package cz.uhk.zlesak.threejslearningapp.views.error;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport.*;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorViewsTest {

    @BeforeEach
    void setUp() {
        setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        clearCurrentUi();
    }

    @Test
    void notFoundView_shouldReturn404AndUseExceptionMessage() {
        NotFoundView view = new NotFoundView();
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        Location location = mock(Location.class);
        @SuppressWarnings("unchecked")
        ErrorParameter<NotFoundException> parameter = mock(ErrorParameter.class);

        when(event.getLocation()).thenReturn(location);
        when(location.getPath()).thenReturn("missing-page");
        when(parameter.getException()).thenReturn(new NotFoundException("Nenalezena custom cesta"));

        int statusCode = view.setErrorParameter(event, parameter);
        Paragraph message = findAll(view, Paragraph.class).getFirst();

        assertEquals(SC_NOT_FOUND, statusCode);
        assertEquals("Nenalezena custom cesta", message.getText());
    }

    @Test
    void accessDeniedView_shouldReturn403() {
        AccessDeniedView view = new AccessDeniedView();
        @SuppressWarnings("unchecked")
        ErrorParameter<AccessDeniedException> parameter = mock(ErrorParameter.class);

        int statusCode = view.setErrorParameter(mock(BeforeEnterEvent.class), parameter);

        assertEquals(SC_FORBIDDEN, statusCode);
    }

    @Test
    void errorView_shouldReturn500() {
        ErrorView view = new ErrorView();
        @SuppressWarnings("unchecked")
        ErrorParameter<Exception> parameter = mock(ErrorParameter.class);
        when(parameter.getException()).thenReturn(new RuntimeException("boom"));

        int statusCode = view.setErrorParameter(mock(BeforeEnterEvent.class), parameter);

        assertEquals(SC_INTERNAL_SERVER_ERROR, statusCode);
    }
}
