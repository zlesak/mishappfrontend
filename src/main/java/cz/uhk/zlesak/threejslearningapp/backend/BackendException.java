package cz.uhk.zlesak.threejslearningapp.backend;

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
}
