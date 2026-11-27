package cz.uhk.zlesak.threejslearningapp.services.conflict;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;

import java.util.List;
import java.util.Map;

/**
 * What one kind of entity needs for the conflict dialog: which of its fields differ between two
 * versions, and how to put a version together from a choice per field.
 *
 * <p>Fields that cannot be chosen independently without producing something broken are offered as
 * one field: a chapter's headings carry the models they show, so the content and those models go
 * together; a quiz's answers are keyed by question, so questions and answers go together.
 *
 * @param <E> entity type.
 */
public interface ConflictResolution<E extends AbstractEntity> {

    /**
     * @param mine   the version the user tried to save.
     * @param theirs the version stored now.
     * @return the fields whose values differ, in the order the form shows them.
     */
    List<ConflictField> differences(E mine, E theirs);

    /**
     * @param mine    the version the user tried to save.
     * @param theirs  the version stored now.
     * @param choices choice per {@link ConflictField#key()}; a field without a choice keeps mine.
     * @return the version to save. It is based on {@code theirs}, so saving it is checked against
     * the version the user was shown and fails again if a third save got in first.
     */
    E merge(E mine, E theirs, Map<String, ConflictChoice> choices);

    /**
     * @param choices choices as returned by the dialog.
     * @param key     a field key.
     * @return whether the stored value was chosen for that field.
     */
    static boolean takesTheirs(Map<String, ConflictChoice> choices, String key) {
        return choices != null && choices.get(key) == ConflictChoice.THEIRS;
    }
}
