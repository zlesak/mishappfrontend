package cz.uhk.zlesak.threejslearningapp.services.conflict;

/**
 * One field that differs between the user's version and the stored one, rendered for reading.
 *
 * @param key         identifies the field when the choices come back.
 * @param label       what the user sees as the field's name.
 * @param mine        the user's value as readable text.
 * @param theirs      the stored value as readable text.
 * @param mineImage   optional picture of the user's value (a data or stream URL), e.g. a model's
 *                    thumbnail; {@code null} when the field has none.
 * @param theirsImage optional picture of the stored value.
 */
public record ConflictField(String key, String label, String mine, String theirs, String mineImage, String theirsImage) {

    public ConflictField(String key, String label, String mine, String theirs) {
        this(key, label, mine, theirs, null, null);
    }
}
