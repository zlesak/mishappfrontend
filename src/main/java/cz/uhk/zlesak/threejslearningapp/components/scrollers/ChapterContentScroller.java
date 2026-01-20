package cz.uhk.zlesak.threejslearningapp.components.scrollers;

import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import cz.uhk.zlesak.threejslearningapp.components.editors.EditorJs;

/**
 * ChapterContentScroller Class - A custom scroller component that contains EditorJs for chapter content editing.
 * This scroller is designed to hold chapter content editors in a vertical layout.
 */
public class ChapterContentScroller extends Scroller {

    public ChapterContentScroller(EditorJs editorjs) {
        super(new VerticalLayout(), ScrollDirection.VERTICAL);
        VerticalLayout scrollerVerticalLayout = (VerticalLayout) getContent();
        scrollerVerticalLayout.add(editorjs);
        scrollerVerticalLayout.setMargin(false);
        scrollerVerticalLayout.setPadding(false);
        scrollerVerticalLayout.setSpacing(false);
        scrollerVerticalLayout.setSizeFull();
        setSizeFull();
    }
}
