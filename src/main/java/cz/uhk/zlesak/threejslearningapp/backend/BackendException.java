package cz.uhk.zlesak.threejslearningapp.backend;

import cz.uhk.zlesak.threejslearningapp.domain.common.AbstractEntity;

/**
 * Failure raised by the embedded backend. The message is written in Czech because it is surfaced
 * to the user by the UI's error notifications.
 */
public class BackendException extends RuntimeException {

    /**
     * @param message user-facing description of the failure.
     */
    public BackendException(String message) {
        super(message);
    }

    /**
     * @param message user-facing description of the failure.
     * @param cause   underlying failure.
     */
    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Input rejected before anything was persisted. */
    public static class Validation extends BackendException {
        public Validation(String message) {
            super(message);
        }
    }

    /** A referenced entity does not exist. */
    public static class NotFound extends BackendException {
        public NotFound(String message) {
            super(message);
        }
    }

    /** The current user may not perform the requested change. */
    public static class Forbidden extends BackendException {
        public Forbidden(String message) {
            super(message);
        }
    }

    /**
     * The save was based on a version that is no longer the stored one: someone else saved in the
     * meantime. Nothing was written. Carries the stored version so the UI can show what changed
     * instead of only saying that something did.
     */
    public static class Conflict extends BackendException {
        private final transient AbstractEntity current;
        private final transient AbstractEntity attempted;

        /**
         * @param message user-facing description of the conflict.
         * @param current the version stored now, which the rejected save would have overwritten.
         */
        public Conflict(String message, AbstractEntity current) {
            this(message, current, null);
        }

        private Conflict(String message, AbstractEntity current, AbstractEntity attempted) {
            super(message);
            this.current = current;
            this.attempted = attempted;
        }

        /**
         * @param attempted the version the user tried to save, in the form the caller prepared it.
         * @return the same conflict, also carrying the rejected version. The backend only sees what
         * reached it; the UI layer that built the entity attaches it so a comparison can show it.
         */
        public Conflict withAttempted(AbstractEntity attempted) {
            return new Conflict(getMessage(), current, attempted);
        }

        /** @return the version stored now. */
        public AbstractEntity getCurrent() {
            return current;
        }

        /** @return the rejected version, or {@code null} when no layer attached it. */
        public AbstractEntity getAttempted() {
            return attempted;
        }

        /**
         * @param throwable a failure as it reached a view, possibly wrapped.
         * @return the conflict somewhere in its cause chain, or {@code null}.
         */
        public static Conflict findIn(Throwable throwable) {
            for (Throwable current = throwable; current != null; current = current.getCause()) {
                if (current instanceof Conflict conflict) {
                    return conflict;
                }
                if (current.getCause() == current) {
                    break;
                }
            }
            return null;
        }
    }
}
