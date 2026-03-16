package cz.uhk.zlesak.threejslearningapp.components.dialogs.leaveDialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeLeaveEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class BeforeLeaveActionDialogTest {
    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void leaveShouldPostponeNavigationAndOpenModalDialog() {
        BeforeLeaveEvent event = mock(BeforeLeaveEvent.class);
        BeforeLeaveEvent.ContinueNavigationAction action = mock(BeforeLeaveEvent.ContinueNavigationAction.class);
        when(event.postpone()).thenReturn(action);
        VaadinTestSupport.setCurrentUi();
        assertNotNull(UI.getCurrent());

        assertDoesNotThrow(() -> BeforeLeaveActionDialog.leave(event));

        verify(event).postpone();
    }

    @Test
    void leaveWithConsumerShouldUseOverloadWithoutExecutingConsumerImmediately() {
        BeforeLeaveEvent event = mock(BeforeLeaveEvent.class);
        BeforeLeaveEvent.ContinueNavigationAction action = mock(BeforeLeaveEvent.ContinueNavigationAction.class);
        when(event.postpone()).thenReturn(action);
        AtomicReference<BeforeLeaveEvent.ContinueNavigationAction> captured = new AtomicReference<>();
        VaadinTestSupport.setCurrentUi();
        assertNotNull(UI.getCurrent());

        assertDoesNotThrow(() -> BeforeLeaveActionDialog.leave(event, captured::set));

        verify(event).postpone();
        verifyNoInteractions(action);
    }
}
