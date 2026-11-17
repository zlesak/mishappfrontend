package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.html.Span;

/**
 * A Lumo badge that always says a word.
 *
 * <p>Colour alone is not information: a reader who cannot distinguish the hues, or who is listening
 * rather than looking, gets nothing from a coloured pill. The text is therefore mandatory and the
 * tone only decorates it.
 *
 * <p>Use for states the domain actually has. A number of sub-chapters, a number of models and a date
 * are not states — those belong in the metadata line as ordinary text.
 */
public class StatusBadge extends Span {

    /**
     * The visual weight of a badge. Maps onto the Lumo badge theme variants.
     */
    public enum Tone {
        /** Neutral, for a plain fact that still needs to stand out. */
        NEUTRAL(""),
        /** Something succeeded or is complete. */
        SUCCESS(" success"),
        /** Something needs attention before the user can go on. */
        WARNING(" contrast"),
        /** Something failed or is missing. */
        ERROR(" error");

        private final String themeSuffix;

        Tone(String themeSuffix) {
            this.themeSuffix = themeSuffix;
        }
    }

    /**
     * Constructs a neutral badge.
     *
     * @param label the word shown, must not be blank
     */
    public StatusBadge(String label) {
        this(label, Tone.NEUTRAL);
    }

    /**
     * Constructs the badge.
     *
     * @param label the word shown, must not be blank
     * @param tone the visual weight
     */
    public StatusBadge(String label, Tone tone) {
        super(label);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("A badge without a word carries no information");
        }
        getElement().getThemeList().add("badge" + (tone == null ? "" : tone.themeSuffix));
    }
}
