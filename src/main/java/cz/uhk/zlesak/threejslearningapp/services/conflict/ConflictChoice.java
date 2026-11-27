package cz.uhk.zlesak.threejslearningapp.services.conflict;

/** Which version of one field to keep when resolving a save conflict. */
public enum ConflictChoice {
    /** The value the user was trying to save. */
    MINE,
    /** The value someone else saved in the meantime. */
    THEIRS
}
