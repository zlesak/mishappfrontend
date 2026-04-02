package cz.uhk.zlesak.threejslearningapp.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * SpringContextUtils Class - Utility class to access Spring ApplicationContext and retrieve beans.
 * This class implements ApplicationContextAware to allow Spring to inject the application context.
 * Used mainly for i18n beans and other components that need to be accessed statically.
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
    private static ApplicationContext context;


    /**
     * Sets the ApplicationContext. This method is called by Spring during application startup.
     *
     * @param applicationContext the ApplicationContext to be set
     */
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * Manually sets the ApplicationContext. Intended for test environments.
     *
     * @param applicationContext the ApplicationContext to set.
     */
    public static void setContext(@NotNull ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * Retrieves a bean from the Spring ApplicationContext by its class type.
     *
     * @param beanClass the class type of the bean to be retrieved
     * @param <T>       the type of the bean
     * @return the bean instance of the specified class type
     */
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}

