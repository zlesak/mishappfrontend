package cz.uhk.zlesak.threejslearningapp.common.logging;

import cz.uhk.zlesak.threejslearningapp.backend.service.CurrentUserProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Stamps every request with a correlation id and the acting user, so log lines can be grouped by
 * request and by user in Loki or Elasticsearch.
 *
 * <p>An incoming {@code X-Request-Id} is honoured, which is what lets a gateway or a load balancer
 * tie its own access log to the application's lines. The id is echoed back on the response so a user
 * reporting a problem can quote something that finds their request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class LoggingContextFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final CurrentUserProvider currentUserProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationIdOf(request);
        MDC.put(LogContext.CORRELATION_ID, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);

        String userId = currentUserProvider.currentUserId();
        if (userId != null) {
            MDC.put(LogContext.USER_ID, userId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(LogContext.CORRELATION_ID);
            MDC.remove(LogContext.USER_ID);
        }
    }

    /**
     * Accepts a caller-supplied id only when it looks like one. An unvalidated header would end up
     * in every log line for the request, which is a log-injection foothold.
     */
    private String correlationIdOf(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        if (supplied != null && supplied.length() <= 64 && supplied.matches("[A-Za-z0-9._-]+")) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }
}
