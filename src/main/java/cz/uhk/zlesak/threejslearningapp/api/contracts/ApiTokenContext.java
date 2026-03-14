package cz.uhk.zlesak.threejslearningapp.api.contracts;

/**
 * Přenáší JWT token mezi UI vláknem a asynchronními worker vlákny.
 */
public final class ApiTokenContext {
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private ApiTokenContext() {
    }

    public static void set(String token) {
        TOKEN.set(token);
    }

    public static String get() {
        return TOKEN.get();
    }

    public static void clear() {
        TOKEN.remove();
    }
}
