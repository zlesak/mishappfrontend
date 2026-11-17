package cz.uhk.zlesak.threejslearningapp.views.abstractViews;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.common.logging.LogContext;
import cz.uhk.zlesak.threejslearningapp.components.notifications.ErrorNotification;
import cz.uhk.zlesak.threejslearningapp.components.notifications.SuccessNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * AbstractView Class - A base class for all views in the application.
 * It extends Composite with a VerticalLayout and implements the IView interface.
 * This class manages event registrations and ensures they are cleaned up when the view is detached.
 * @param <S> the type of service associated with the view
 */
public abstract class AbstractView<S> extends Composite<VerticalLayout> implements IView {
    private static final String SESSION_IO_BULKHEAD_KEY = AbstractView.class.getName() + ".sessionIoBulkhead";
    private static final int MAX_PARALLEL_SESSION_TASKS = Integer.parseInt(System.getenv().getOrDefault("FE_SESSION_IO_MAX_PARALLEL", "3"));
    private static final long MIN_LOADING_VISIBLE_MS = Long.parseLong(System.getenv().getOrDefault("FE_LOADING_MIN_VISIBLE_MS", "250"));

    protected final List<Registration> registrations = new ArrayList<>();
    protected final S service;
    protected final Executor ioExecutor = SpringContextUtils.getBean(Executor.class);
    private final String pageTitleKey;
    private final AtomicInteger runningAsyncTasks = new AtomicInteger(0);
    private final Div asyncLoadingOverlay = new Div();
    private volatile long loadingShownAtMs = 0L;

    /**
     * Constructor for AbstractView.
     * @param pageTitleKey the key for the page title
     * @param service the service associated with the view
     */
    public AbstractView(String pageTitleKey, S service) {
        this.service = service;
        getContent().setSizeFull();
        getContent().addClassName(LumoUtility.Gap.XSMALL);
        getContent().setSpacing(false);
        getContent().getStyle().set("position", "relative");
        this.pageTitleKey = pageTitleKey;

        asyncLoadingOverlay.setVisible(false);
        asyncLoadingOverlay.getElement().setProperty("aria-label", "Načítání");
        asyncLoadingOverlay.getStyle()
                .set("position", "fixed")
                .set("inset", "0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background", "rgba(255,255,255,0.55)")
                .set("backdrop-filter", "blur(1px)")
                .set("z-index", "100000")
                .set("pointer-events", "all");

        Div spinner = new Div();
        spinner.getStyle()
                .set("width", "52px")
                .set("height", "52px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("border-radius", "999px")
                .set("background", "rgba(255,255,255,0.85)")
                .set("box-shadow", "0 4px 18px rgba(0,0,0,0.16)");
        spinner.getElement().setProperty("innerHTML", """
                <svg viewBox='0 0 50 50' width='32' height='32' role='img' aria-hidden='true'>
                  <circle cx='25' cy='25' r='20' stroke='currentColor' stroke-width='5' fill='none' stroke-linecap='round' stroke-dasharray='31.4 157'>
                    <animateTransform attributeName='transform' attributeType='XML' type='rotate' from='0 25 25' to='360 25 25' dur='0.9s' repeatCount='indefinite'/>
                  </circle>
                </svg>
                """);

        Div loadingText = new Div("Načítám...");
        loadingText.getStyle()
                .set("margin-top", "10px")
                .set("font-size", "0.95rem")
                .set("font-weight", "600")
                .set("color", "var(--lumo-body-text-color)");

        asyncLoadingOverlay.add(spinner, loadingText);
        getContent().add(asyncLoadingOverlay);
    }

    /**
     * Shows a success notification.
     */
    protected void showSuccessNotification() {
        new SuccessNotification(text("notification.uploadSuccess"));
    }

    /**
     * Shows an error notification with the given message.
     *
     * @param errorMessage the error message to display
     */
    protected void showErrorNotification(String source, String errorMessage) {
        String safeSource = source == null ? "" : source.trim();
        String safeMessage = errorMessage == null ? "" : errorMessage.trim();

        if (safeSource.isEmpty()) {
            new ErrorNotification(safeMessage.isEmpty() ? text("notification.apiError.default") : safeMessage);
            return;
        }

        if (safeMessage.isEmpty()) {
            new ErrorNotification(safeSource);
            return;
        }

        if (safeSource.endsWith(":")) {
            new ErrorNotification(safeSource + " " + safeMessage);
            return;
        }

        if (safeSource.endsWith(": ")) {
            new ErrorNotification(safeSource + safeMessage);
            return;
        }

        new ErrorNotification(safeSource + ": " + safeMessage);
    }

    /**
     * Shows an error notification describing a failure.
     * The backend raises failures with messages already written for the user, so the deepest
     * message in the chain is the most specific one worth showing.
     */
    protected void showErrorNotification(String source, Throwable throwable) {
        showErrorNotification(source, resolveUserFriendlyErrorMessage(throwable));
    }

    /**
     * @param throwable failure to describe, may be {@code null}.
     * @return the message to show the user.
     */
    protected String resolveUserFriendlyErrorMessage(Throwable throwable) {
        String message = null;
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage().trim();
            }
            current = current.getCause();
        }

        if (message == null) {
            return text("notification.apiError.default");
        }
        return message.length() > 220 ? message.substring(0, 220) + "..." : message;
    }

    /**
     * Runs blocking work on background executor and synchronizes callbacks back to Vaadin UI thread.
     */
    protected <T> void runAsync(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            onError.accept(new IllegalStateException("UI is not available"));
            return;
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            onError.accept(new IllegalStateException("Vaadin session is not available"));
            return;
        }

        Semaphore bulkhead = getOrCreateSessionBulkhead(session);
        if (!bulkhead.tryAcquire()) {
            onError.accept(new IllegalStateException("Příliš mnoho paralelních operací. Zkus to prosím za chvíli."));
            return;
        }

        // Přihlášeného uživatele je nutné přenést explicitně: worker vlákno nemá vazbu na Vaadin
        // session, ze které se přihlášení jinak odvozuje, a backend z něj čte autora změn.
        SecurityContext capturedSecurityContext = SecurityContextHolder.getContext();
        // Stejný důvod pro logovací kontext: bez něj by řádky z workeru neměly korelační ID
        // a nešlo by je spojit s požadavkem, který je vyvolal.
        Map<String, String> capturedLogContext = LogContext.capture();

        onAsyncWorkStarted(ui);

        try {
            CompletableFuture
                    .supplyAsync(() -> {
                        SecurityContext previous = SecurityContextHolder.getContext();
                        SecurityContextHolder.setContext(capturedSecurityContext);
                        try {
                            return LogContext.with(capturedLogContext, supplier);
                        } catch (Throwable t) {
                            throw new CompletionException(t);
                        } finally {
                            // Restore rather than clear: the worker may be a pooled thread whose
                            // holder is shared with the caller, and clearing it would sign the
                            // user out of everything that runs afterwards.
                            SecurityContextHolder.setContext(previous);
                        }
                    }, ioExecutor)
                    .whenComplete((result, error) -> {
                        bulkhead.release();
                        onAsyncWorkFinished(ui);
                        if (ui.isClosing()) {
                            return;
                        }
                        ui.access(() -> {
                            try {
                                if (error != null) {
                                    onError.accept(unwrapAsyncError(error));
                                    return;
                                }
                                onSuccess.accept(result);
                            } catch (Throwable callbackError) {
                                try {
                                    onError.accept(unwrapAsyncError(callbackError));
                                } catch (Throwable ignored) {
                                    // Ignored
                                }
                            }
                        });
                    });
        } catch (Throwable t) {
            bulkhead.release();
            onAsyncWorkFinished(ui);
            onError.accept(t);
        }
    }

    /**
     * Public wrapper so non-view components can still execute backend calls with the same overlay semantics.
     */
    public <T> void executeAsyncWithOverlay(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        runAsync(supplier, onSuccess, onError);
    }

    /**
     * Convenience wrapper for asynchronous work without a return value.
     */
    protected void runAsyncVoid(Runnable runnable, Runnable onSuccess, Consumer<Throwable> onError) {
        runAsync(() -> {
            runnable.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    /**
     * Tracks external async work (e.g. client callback/future) with the same loading overlay semantics.
     */
    protected <T> void runFutureWithOverlay(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            onError.accept(new IllegalStateException("UI is not available"));
            return;
        }

        onAsyncWorkStarted(ui);
        future.whenComplete((result, error) -> {
            onAsyncWorkFinished(ui);
            if (ui.isClosing()) {
                return;
            }
            ui.access(() -> {
                if (error != null) {
                    onError.accept(unwrapAsyncError(error));
                    return;
                }
                onSuccess.accept(result);
            });
        });
    }

    /**
     * Returns a future that mirrors the source one and keeps loading overlay visible while it is running.
     */
    protected <T> CompletableFuture<T> futureWithLoadingOverlay(CompletableFuture<T> future) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return future;
        }

        onAsyncWorkStarted(ui);
        CompletableFuture<T> wrapped = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            onAsyncWorkFinished(ui);
            if (error != null) {
                wrapped.completeExceptionally(unwrapAsyncError(error));
                return;
            }
            wrapped.complete(result);
        });
        return wrapped;
    }

    public static AbstractView<?> findCurrentAbstractView(UI ui) {
        if (ui == null) {
            return null;
        }

        for (HasElement target : ui.getInternals().getActiveRouterTargetsChain()) {
            if (target instanceof AbstractView<?> abstractView) {
                return abstractView;
            }
        }
        return null;
    }

    protected void beginLoadingOverlay(UI ui) {
        if (ui == null || ui.isClosing()) {
            return;
        }
        onAsyncWorkStarted(ui);
    }

    protected void endLoadingOverlay(UI ui) {
        if (ui == null || ui.isClosing()) {
            return;
        }
        onAsyncWorkFinished(ui);
    }

    private void onAsyncWorkStarted(UI ui) {
        if (runningAsyncTasks.incrementAndGet() == 1 && !ui.isClosing()) {
            Command showOverlay = () -> {
                asyncLoadingOverlay.setVisible(true);
                getContent().getElement().setAttribute("aria-busy", "true");
                loadingShownAtMs = System.currentTimeMillis();
            };

            if (UI.getCurrent() == ui) {
                showOverlay.execute();
            } else {
                ui.access(showOverlay);
            }
        }
    }

    private void onAsyncWorkFinished(UI ui) {
        int current = runningAsyncTasks.updateAndGet(value -> Math.max(0, value - 1));
        if (current == 0 && !ui.isClosing()) {
            long elapsed = System.currentTimeMillis() - loadingShownAtMs;
            long remaining = Math.max(0L, MIN_LOADING_VISIBLE_MS - elapsed);

            Command hideOverlay = () -> {
                if (runningAsyncTasks.get() != 0) {
                    return;
                }
                asyncLoadingOverlay.setVisible(false);
                getContent().getElement().removeAttribute("aria-busy");
            };

            if (remaining == 0L) {
                ui.access(hideOverlay);
            } else {
                CompletableFuture
                        .runAsync(() -> {
                        }, CompletableFuture.delayedExecutor(remaining, TimeUnit.MILLISECONDS))
                        .whenComplete((ignored, error) -> {
                            if (!ui.isClosing()) {
                                ui.access(hideOverlay);
                            }
                        });
            }
        }
    }

    private Semaphore getOrCreateSessionBulkhead(VaadinSession session) {
        Object existing = session.getAttribute(SESSION_IO_BULKHEAD_KEY);
        if (existing instanceof Semaphore semaphore) {
            return semaphore;
        }
        Semaphore semaphore = new Semaphore(MAX_PARALLEL_SESSION_TASKS, true);
        session.setAttribute(SESSION_IO_BULKHEAD_KEY, semaphore);
        return semaphore;
    }

    protected Throwable unwrapAsyncError(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Gets the title of the page.
     *
     * @return the page title
     */
    @Override
    public String getPageTitle() {
        return text(pageTitleKey);
    }

    /**
     * On detach function to clean up event registrations when the view is detached.
     * @param detachEvent the detach event
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
