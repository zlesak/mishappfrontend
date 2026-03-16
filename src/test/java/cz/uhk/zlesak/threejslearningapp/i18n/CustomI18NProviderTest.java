package cz.uhk.zlesak.threejslearningapp.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomI18NProviderTest {

    @Test
    void getTranslation_shouldLoadCzechResourcesAndFormatParameters() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals("MISH - Úvod", provider.getTranslation("page.title.mainPageView", Locale.forLanguageTag("cs")));
        assertEquals(
                "Opravdu chcete smazat kvíz \"Atlas\"?",
                provider.getTranslation("dialog.delete.quiz.message", Locale.forLanguageTag("cs"), "Atlas")
        );
    }

    @Test
    void getTranslation_shouldFallbackToKeyForUnknownOrUnsupportedLocale() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals("missing.key", provider.getTranslation("missing.key", Locale.forLanguageTag("cs")));
        assertEquals("page.title.mainPageView", provider.getTranslation("page.title.mainPageView", Locale.ENGLISH));
    }

    @Test
    void getProvidedLocales_shouldExposeCzechLocale() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals(1, provider.getProvidedLocales().size());
        assertTrue(provider.getProvidedLocales().contains(Locale.forLanguageTag("cs")));
    }
}
