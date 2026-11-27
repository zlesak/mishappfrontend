package cz.uhk.zlesak.threejslearningapp.services.conflict;

/**
 * Looks up a UI text. Views pass their own {@code text(...)}; tests can pass the key back, so the
 * comparison logic does not need a running application to be checked.
 */
@FunctionalInterface
public interface Translator {
    String text(String key, Object... params);
}
