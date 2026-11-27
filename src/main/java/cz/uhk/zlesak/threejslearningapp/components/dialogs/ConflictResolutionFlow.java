package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictChoice;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictResolution;
import lombok.Getter;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What an editing view does when its save comes back as a {@link BackendException.Conflict}: it opens
 * the comparison, saves the combination the user picks, and opens the comparison again if that save
 * conflicts too. The same flow serves chapters, quizzes and models; only the
 * {@link ConflictResolution} differs.
 *
 * <p>A conflict never reaches {@code onSaved}: the only way to success is a save the backend
 * accepted.
 *
 * @param <E> entity type being saved.
 */
public final class ConflictResolutionFlow<E extends AbstractEntity> {

    /** Runs a backend call off the UI thread and reports back on it, as views do with {@code runAsync}. */
    @FunctionalInterface
    public interface AsyncRunner {
        void run(Supplier<String> work, Consumer<String> onSuccess, Consumer<Throwable> onError);
    }

    private final Class<E> type;
    private final String titleKey;
    private final ConflictResolution<E> resolution;
    private final Function<E, String> saveResolved;
    private final AsyncRunner runner;
    private final Consumer<String> onSaved;
    private final Consumer<Throwable> onFailure;
    private final Runnable onDiscard;

    /** The dialog opened last, so tests can operate it. */
    @Getter
    private ConflictResolutionDialog dialog;

    /**
     * @param type         entity type; a conflict about anything else is not handled.
     * @param titleKey     text key of the dialog heading.
     * @param resolution   how this entity is compared and combined.
     * @param saveResolved saves a combined version, returning its id; may throw another conflict.
     * @param runner       runs the save asynchronously.
     * @param onSaved      what the view does after a successful save, with the saved id.
     * @param onFailure    what the view does with any other failure of the save.
     * @param onDiscard    throws the user's changes away and loads the stored version.
     */
    public ConflictResolutionFlow(Class<E> type,
                                  String titleKey,
                                  ConflictResolution<E> resolution,
                                  Function<E, String> saveResolved,
                                  AsyncRunner runner,
                                  Consumer<String> onSaved,
                                  Consumer<Throwable> onFailure,
                                  Runnable onDiscard) {
        this.type = type;
        this.titleKey = titleKey;
        this.resolution = resolution;
        this.saveResolved = saveResolved;
        this.runner = runner;
        this.onSaved = onSaved;
        this.onFailure = onFailure;
        this.onDiscard = onDiscard;
    }

    /**
     * @param error failure of a save, as the view received it.
     * @return {@code true} when it was a conflict and the comparison is now open; {@code false} when
     * the view has to report it as an ordinary error.
     */
    public boolean handle(Throwable error) {
        BackendException.Conflict conflict = BackendException.Conflict.findIn(error);
        if (conflict == null || !type.isInstance(conflict.getAttempted()) || !type.isInstance(conflict.getCurrent())) {
            return false;
        }
        E mine = type.cast(conflict.getAttempted());
        E theirs = type.cast(conflict.getCurrent());

        dialog = new ConflictResolutionDialog(titleKey, theirs.getUpdated(),
                resolution.differences(mine, theirs),
                choices -> save(mine, theirs, choices),
                onDiscard);
        dialog.open();
        return true;
    }

    private void save(E mine, E theirs, Map<String, ConflictChoice> choices) {
        E combined = resolution.merge(mine, theirs, choices);
        runner.run(() -> saveResolved.apply(combined), onSaved, error -> {
            if (!handle(error)) {
                onFailure.accept(error);
            }
        });
    }
}
