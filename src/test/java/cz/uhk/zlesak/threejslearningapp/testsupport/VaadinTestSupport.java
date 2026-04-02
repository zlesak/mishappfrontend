package cz.uhk.zlesak.threejslearningapp.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.i18n.CustomI18NProvider;
import cz.uhk.zlesak.threejslearningapp.services.ModelService;
import org.springframework.context.support.GenericApplicationContext;

import java.util.*;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
public final class VaadinTestSupport {
    private VaadinTestSupport() {
    }

    public static UI setCurrentUi() {
        return setCurrentUiInternal(Collections.emptyMap());
    }

    public static void setCurrentUiWithBeans(Map<Class<?>, Object> beans) {
        setCurrentUiInternal(beans);
    }

    @SafeVarargs
    private static UI setCurrentUiInternal(Map<Class<?>, Object> beans, Class<? extends Component>... routeTargets) {
        ensureI18nContext(beans);
        Map<Class<? extends Component>, String> routes = resolveRoutes(routeTargets);
        var routeRegistry = mock(RouteRegistry.class);
        when(routeRegistry.getTargetUrl(any())).thenAnswer(invocation ->
                Optional.ofNullable(routes.get(asComponentType(invocation.getArgument(0)))));
        when(routeRegistry.getTargetUrl(any(), any(RouteParameters.class))).thenAnswer(invocation ->
                Optional.ofNullable(routes.get(asComponentType(invocation.getArgument(0)))));

        var service = mock(VaadinService.class);
        VaadinService.setCurrent(service);

        Router router = new Router(routeRegistry);
        when(service.getRouter()).thenReturn(router);

        var session = mock(VaadinSession.class);
        doNothing().when(session).checkHasLock();
        when(session.getService()).thenReturn(service);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);

        UI ui = new UI();
        ui.setLocale(Locale.forLanguageTag("cs"));
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        return ui;
    }

    public static void clearCurrentUi() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        VaadinService.setCurrent(null);
    }

    public static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> results = new ArrayList<>();
        collect(root, type, results);
        return results;
    }

    public static <T extends Component> T findFirst(Component root, Class<T> type) {
        return findAll(root, type).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + type.getName()));
    }

    public static Button findButtonByText(Component root, String text) {
        Optional<Button> button = findAll(root, Button.class).stream()
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst();
        return button.orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> results) {
        if (type.isInstance(component)) {
            results.add(type.cast(component));
        }
        component.getChildren().forEach(child -> collect(child, type, results));
    }

    private static void ensureI18nContext(Map<Class<?>, Object> beans) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(CustomI18NProvider.class, () -> new CustomI18NProvider(new ObjectMapper()));

        Map<Class<?>, Object> resolvedBeans = new HashMap<>();
        resolvedBeans.put(ModelService.class, mock(ModelService.class));
        resolvedBeans.put(Executor.class, (Executor) Runnable::run);
        
        resolvedBeans.putAll(beans);

        
        resolvedBeans.forEach((type, bean) -> registerBean(context, type, bean));
        context.refresh();

        SpringContextUtils.setContext(context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerBean(GenericApplicationContext context, Class<?> type, Object bean) {
        context.registerBean((Class) type, () -> bean);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Component> asComponentType(Object routeTarget) {
        return (Class<? extends Component>) routeTarget;
    }

    @SafeVarargs
    private static Map<Class<? extends Component>, String> resolveRoutes(Class<? extends Component>... routeTargets) {
        Map<Class<? extends Component>, String> routes = new HashMap<>();
        for (Class<? extends Component> routeTarget : routeTargets) {
            Route route = routeTarget.getAnnotation(Route.class);
            if (route != null) {
                routes.put(routeTarget, route.value());
            }
        }
        return routes;
    }
}
