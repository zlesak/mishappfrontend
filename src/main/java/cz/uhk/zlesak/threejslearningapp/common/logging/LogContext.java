package cz.uhk.zlesak.threejslearningapp.common.logging;

import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The fields every log line carries, so lines from one request can be pulled together again.
 *
 * <p>Names follow Elastic Common Schema, which is what both an ELK stack and Grafana/Loki
 * understand without a per-field mapping.
 */
public final class LogContext {

    /** Ties every line of one request together, including the ones written on worker threads. */
    public static final String CORRELATION_ID = "trace.id";

    /** Stable id of the acting user; never their name, so log storage holds no more than it needs. */
    public static final String USER_ID = "user.id";

    private LogContext() {
    }

    /**
     * Runs work with a copy of the caller's logging context.
     *
     * <p>MDC is thread-local, so anything handed to an executor would otherwise log without a
     * correlation id and become impossible to tie back to the request that started it.
     *
     * @param context context captured on the calling thread, may be {@code null}.
     * @param work    work to run.
     * @param <T>     result type.
     * @return whatever {@code work} returns.
     */
    public static <T> T with(Map<String, String> context, Supplier<T> work) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        apply(context);
        try {
            return work.get();
        } finally {
            apply(previous);
        }
    }

    /**
     * @return the current context, to be handed to {@link #with(Map, Supplier)} on another thread.
     */
    public static Map<String, String> capture() {
        return MDC.getCopyOfContextMap();
    }

    private static void apply(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
