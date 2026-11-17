package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkTextAuditTest {

    @Test
    void aLinkThatSaysOnlyZdeIsReported() {
        // A screen reader user often pulls up the list of links on a page with the surrounding sentences
        // stripped away. A page whose links all read "ZDE" gives them a list of identical entries.
        Set<String> found = LinkTextAudit.nonDescriptiveLinkTexts(
                "<p>Více informací najdete <a href=\"/atlas\">ZDE</a>.</p>");

        assertEquals(List.of("ZDE"), List.copyOf(found));
    }

    @Test
    void aLinkThatNamesItsDestinationIsLeftAlone() {
        Set<String> found = LinkTextAudit.nonDescriptiveLinkTexts(
                "<p>Přejít na <a href=\"/atlas\">Atlas kostí lebky</a>.</p>");

        assertTrue(found.isEmpty(), found.toString());
    }

    @Test
    void punctuationCasingAndNestedMarkupDoNotHideTheProblem() {
        Set<String> found = LinkTextAudit.nonDescriptiveLinkTexts(
                "<p><a href=\"/a\">Klikněte zde!</a> a <a href=\"/b\"><b>více</b></a></p>");

        assertEquals(2, found.size(), found.toString());
        assertTrue(found.contains("Klikněte zde!"), found.toString());
        assertTrue(found.contains("více"), found.toString());
    }

    @Test
    void theSameOffenderIsReportedOnce() {
        Set<String> found = LinkTextAudit.nonDescriptiveLinkTexts(
                "<a href=\"/a\">zde</a><a href=\"/b\">zde</a>");

        assertEquals(1, found.size(), found.toString());
    }

    @Test
    void contentWithoutLinksOrNoContentAtAllIsFine() {
        assertTrue(LinkTextAudit.nonDescriptiveLinkTexts(null).isEmpty());
        assertTrue(LinkTextAudit.nonDescriptiveLinkTexts("  ").isEmpty());
        assertTrue(LinkTextAudit.nonDescriptiveLinkTexts("<p>Bez odkazů.</p>").isEmpty());
    }

    @Test
    void anEmptyAnchorIsNotReportedAsUnhelpfulText() {
        // An anchor with no text is a different defect and not this one's to report.
        assertTrue(LinkTextAudit.nonDescriptiveLinkTexts("<a href=\"/a\"></a>").isEmpty());
    }
}
