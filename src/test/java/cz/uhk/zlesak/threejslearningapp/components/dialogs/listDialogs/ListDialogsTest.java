package cz.uhk.zlesak.threejslearningapp.components.dialogs.listDialogs;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.events.chapter.ChapterSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.events.model.ModelSelectedFromDialogEvent;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import cz.uhk.zlesak.threejslearningapp.testsupport.VaadinTestSupport;
import cz.uhk.zlesak.threejslearningapp.views.abstractViews.AbstractListingView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ListDialogsTest {

    private static final Objenesis OBJENESIS = new ObjenesisStd();

    @BeforeEach
    void setUp() {
        VaadinTestSupport.setCurrentUi();
    }

    @AfterEach
    void tearDown() {
        VaadinTestSupport.clearCurrentUi();
    }

    @Test
    void chapterListDialog_fireEntitySelectedEvent_shouldFireCorrectEventOnUi() throws Exception {
        ChapterListDialog dialog = OBJENESIS.newInstance(ChapterListDialog.class);
        setField(dialog, "blockId", "block-chapter-1");

        AtomicReference<ChapterSelectedFromDialogEvent> capturedEvent = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ChapterSelectedFromDialogEvent.class, capturedEvent::set);

        ChapterEntity chapter = ChapterEntity.builder().id("ch-1").name("Test Chapter").build();
        invokeProtected(ChapterListDialog.class, "fireEntitySelectedEvent", ChapterEntity.class, dialog, chapter);

        assertNotNull(capturedEvent.get(), "ChapterSelectedFromDialogEvent should have been fired");
        assertEquals("ch-1", capturedEvent.get().getSelectedChapter().getId());
        assertEquals("block-chapter-1", capturedEvent.get().getBlockId());
    }

    @Test
    void modelListDialog_fireEntitySelectedEvent_shouldFireCorrectEventOnUi() throws Exception {
        ModelListDialog dialog = OBJENESIS.newInstance(ModelListDialog.class);
        setField(dialog, "blockId", "block-model-1");

        AtomicReference<ModelSelectedFromDialogEvent> capturedEvent = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ModelSelectedFromDialogEvent.class, capturedEvent::set);

        QuickModelEntity model = TestFixtures.model("m-1", "model-1", "Test Model", null, java.util.List.of());
        invokeProtected(ModelListDialog.class, "fireEntitySelectedEvent", QuickModelEntity.class, dialog, model);

        assertNotNull(capturedEvent.get(), "ModelSelectedFromDialogEvent should have been fired");
        assertEquals("m-1", capturedEvent.get().getSelectedModel().getId());
        assertEquals("block-model-1", capturedEvent.get().getBlockId());
    }

    @Test
    void abstractListDialog_blockIdGetterAndSetter_shouldWorkViaReflection() throws Exception {
        ModelListDialog dialog = OBJENESIS.newInstance(ModelListDialog.class);

        assertNull(getField(dialog, "blockId"));

        setField(dialog, "blockId", "some-block");
        assertEquals("some-block", getField(dialog, "blockId"));

        setField(dialog, "blockId", null);
        assertNull(getField(dialog, "blockId"));
    }

    @Test
    void chapterListDialog_fireEntitySelectedEvent_withNullBlockId_shouldStillFireEvent() throws Exception {
        ChapterListDialog dialog = OBJENESIS.newInstance(ChapterListDialog.class);

        AtomicReference<ChapterSelectedFromDialogEvent> capturedEvent = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ChapterSelectedFromDialogEvent.class, capturedEvent::set);

        ChapterEntity chapter = ChapterEntity.builder().id("ch-2").build();
        invokeProtected(ChapterListDialog.class, "fireEntitySelectedEvent", ChapterEntity.class, dialog, chapter);

        assertNotNull(capturedEvent.get());
        assertNull(capturedEvent.get().getBlockId());
        assertEquals("ch-2", capturedEvent.get().getSelectedChapter().getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void abstractListDialog_openAndOnEntitySelected_shouldCallListViewAndFireEvent() throws Exception {
        AbstractListingView<QuickModelEntity, ?, ?, ?> listView = mock(AbstractListingView.class);

        AtomicReference<Consumer<QuickModelEntity>> capturedListener = new AtomicReference<>();
        doAnswer(inv -> {
            capturedListener.set(inv.getArgument(0));
            return null;
        }).when(listView).setEntitySelectedListener(any());

        AtomicReference<ModelSelectedFromDialogEvent> capturedEvent = new AtomicReference<>();
        ComponentUtil.addListener(UI.getCurrent(), ModelSelectedFromDialogEvent.class, capturedEvent::set);

        SafeTestAbstractListDialog dialog = OBJENESIS.newInstance(SafeTestAbstractListDialog.class);
        Field lvField = AbstractListDialog.class.getDeclaredField("listView");
        lvField.setAccessible(true);
        lvField.set(dialog, listView);
        Field bidField = AbstractListDialog.class.getDeclaredField("blockId");
        bidField.setAccessible(true);
        bidField.set(dialog, "open-block");

        dialog.open();

        verify(listView).setEntitySelectedListener(any());
        verify(listView).listEntities();

        QuickModelEntity model = TestFixtures.model("m-open", "model-open", "Open Model", null, java.util.List.of());
        capturedListener.get().accept(model);

        assertFalse(dialog.opened);
        assertNotNull(capturedEvent.get());
        assertEquals("m-open", capturedEvent.get().getSelectedModel().getId());
        assertEquals("open-block", capturedEvent.get().getBlockId());
    }

    @Test
    void abstractListDialog_setBlockId_shouldUpdateBlockId() throws Exception {
        ChapterListDialog dialog = OBJENESIS.newInstance(ChapterListDialog.class);
        dialog.setBlockId("via-setter");
        Field field = AbstractListDialog.class.getDeclaredField("blockId");
        field.setAccessible(true);
        assertEquals("via-setter", field.get(dialog));
    }

    private static final class SafeTestAbstractListDialog extends AbstractListDialog<QuickModelEntity> {
        boolean opened = false;

        @SuppressWarnings("unused")
        SafeTestAbstractListDialog(AbstractListingView<QuickModelEntity, ?, ?, ?> listView) {            super(listView);
        }

        @Override public void setOpened(boolean open) { this.opened = open; }
        @Override public boolean isOpened() { return opened; }
        @Override public void close() { setOpened(false); }

        @Override
        protected void fireEntitySelectedEvent(QuickModelEntity entity) {
            ComponentUtil.fireEvent(
                    UI.getCurrent(),
                    new ModelSelectedFromDialogEvent(UI.getCurrent(), false, entity, getBlockId())
            );
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AbstractListDialog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = AbstractListDialog.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static <T> void invokeProtected(Class<T> clazz, String methodName,
                                            Class<?> paramType, Object target, Object arg) throws Exception {
        Method method = clazz.getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        method.invoke(target, arg);
    }
}

