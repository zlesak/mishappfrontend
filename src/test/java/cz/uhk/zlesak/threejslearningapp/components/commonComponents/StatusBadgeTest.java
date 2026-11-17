package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusBadgeTest {

    @Test
    void aBadgeAlwaysCarriesTheLumoBadgeTheme() {
        StatusBadge badge = new StatusBadge("Nezodpovězeno");

        assertEquals("Nezodpovězeno", badge.getText());
        assertTrue(badge.getElement().getThemeList().contains("badge"));
    }

    @Test
    void theToneOnlyDecoratesTheWord() {
        StatusBadge badge = new StatusBadge("Nezodpovězeno", StatusBadge.Tone.WARNING);

        assertTrue(badge.getElement().getThemeList().contains("badge"));
        assertTrue(badge.getElement().getThemeList().contains("contrast"));
        assertEquals("Nezodpovězeno", badge.getText());
    }

    @Test
    void aBadgeWithoutAWordIsRejected() {
        // Colour alone is not information. A badge that says nothing is a coloured pill that a reader
        // who is listening, or who cannot tell the hues apart, gets nothing from at all.
        assertThrows(IllegalArgumentException.class, () -> new StatusBadge("  "));
        assertThrows(IllegalArgumentException.class, () -> new StatusBadge(null));
    }
}
