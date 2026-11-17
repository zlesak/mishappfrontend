package cz.uhk.zlesak.threejslearningapp.common.logging;

import cz.uhk.zlesak.threejslearningapp.backend.service.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Records who changed what, on a logger of its own so audit lines can be shipped and kept apart
 * from diagnostics.
 *
 * <p>Every entry carries the same fields — {@code event.action}, {@code event.outcome},
 * {@code event.dataset} and the target — which is what makes the trail queryable
 * ({@code event.dataset:"mish.audit" and event.action:"delete"}) rather than something to grep. The
 * correlation id and user id come from {@link LogContext} via MDC, so a single audit line can be
 * expanded into everything else that happened in the same request.
 *
 * <p>Values a user typed are not logged: names of entities are, their content is not.
 */
@Component
@RequiredArgsConstructor
public class AuditLog {

    /** Separate logger name so the audit trail can be routed and retained on its own terms. */
    private static final Logger AUDIT = LoggerFactory.getLogger("mish.audit");

    private static final String DATASET = "mish.audit";

    private final CurrentUserProvider currentUserProvider;

    /** What happened to the target. */
    public enum Action {
        CREATE, UPDATE, DELETE, SUBMIT, DOWNLOAD
    }

    /** How it ended. */
    public enum Outcome {
        SUCCESS, FAILURE, DENIED
    }

    /**
     * Records a completed action.
     *
     * @param action     what was done.
     * @param entityType kind of entity, e.g. {@code chapter}.
     * @param entityId   id of the entity, may be {@code null} when it was never assigned.
     * @param entityName name of the entity, for a reader of the trail.
     */
    public void success(Action action, String entityType, String entityId, String entityName) {
        write(action, Outcome.SUCCESS, entityType, entityId, entityName, null);
    }

    /**
     * Records an action that was refused.
     *
     * @param action     what was attempted.
     * @param entityType kind of entity.
     * @param entityId   id of the entity.
     * @param reason     why it was refused.
     */
    public void denied(Action action, String entityType, String entityId, String reason) {
        write(action, Outcome.DENIED, entityType, entityId, null, reason);
    }

    /**
     * Records an action that failed for a reason other than permissions.
     *
     * @param action     what was attempted.
     * @param entityType kind of entity.
     * @param entityId   id of the entity.
     * @param reason     what went wrong.
     */
    public void failure(Action action, String entityType, String entityId, String reason) {
        write(action, Outcome.FAILURE, entityType, entityId, null, reason);
    }

    private void write(Action action, Outcome outcome, String entityType, String entityId,
                       String entityName, String reason) {
        var event = AUDIT.atInfo()
                .setMessage("{} {} {}")
                .addArgument(outcome)
                .addArgument(action)
                .addArgument(entityType)
                .addKeyValue("event.dataset", DATASET)
                .addKeyValue("event.action", action.name().toLowerCase())
                .addKeyValue("event.outcome", outcome.name().toLowerCase())
                .addKeyValue("mish.entity.type", entityType)
                .addKeyValue("mish.entity.id", entityId)
                // Repeated from MDC on purpose: an audit line has to stand on its own even when it
                // is exported away from the rest of the stream.
                .addKeyValue("user.id", currentUserProvider.currentUserId());

        if (entityName != null) {
            event = event.addKeyValue("mish.entity.name", entityName);
        }
        if (reason != null) {
            event = event.addKeyValue("mish.reason", reason);
        }
        event.log();
    }
}
