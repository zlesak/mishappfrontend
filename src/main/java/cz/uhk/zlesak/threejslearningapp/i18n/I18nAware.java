package cz.uhk.zlesak.threejslearningapp.i18n;

import com.vaadin.flow.component.UI;
import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;

import java.util.Locale;

/**
 * Interface providing internationalization (i18n) support.
 * Classes implementing this interface can easily access translation functionalities.
 */
public interface I18nAware {
    /**
     * Provides the I18NProvider instance.
     */
    default CustomI18NProvider i18nProvider() {
        return SpringContextUtils.getBean(CustomI18NProvider.class);
    }

    /**
     * Provides a method to translate a given key using the current UI locale.
     * @param key    the translation key
     * @param params optional parameters for the translation
     * @return the string in the current locale corresponding to the key
     */
    default String text(String key, Object... params) {
        UI currentUi = UI.getCurrent();
        Locale locale = currentUi != null ? currentUi.getLocale() : Locale.forLanguageTag("cs");
        return i18nProvider().getTranslation(key, locale, params);
    }
}
