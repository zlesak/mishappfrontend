package cz.uhk.zlesak.threejslearningapp.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void everyMessageShownWhenSavingAQuizFailsIsTranslated() {
        // An undefined key falls back to the key itself, so the author was shown the literal
        // "quiz.create.error: ..." on every failed save. These three carry that whole path.
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());
        Locale czech = Locale.forLanguageTag("cs");

        for (String key : java.util.List.of("quiz.create.error", "quiz.update.error",
                "quiz.validation.question.invalid")) {
            assertNotEquals(key, provider.getTranslation(key, czech),
                    "The key '" + key + "' has no Czech text, so the user is shown the key");
        }
    }

    @Test
    void getTranslation_shouldFallbackToKeyForUnknownOrUnsupportedLocale() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals("missing.key", provider.getTranslation("missing.key", Locale.forLanguageTag("cs")));
        assertEquals("page.title.mainPageView", provider.getTranslation("page.title.mainPageView", Locale.ENGLISH));
    }

    @Test
    void getTranslation_withChapterCreatorKey_shouldReturnCzechTranslation() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals("Autor", provider.getTranslation("chapter.creator", Locale.forLanguageTag("cs")));
    }

    @Test
    void getTranslation_withModelViewPageTitle_shouldReturnCzechTranslation() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals("MISH - Model", provider.getTranslation("page.title.modelView", Locale.forLanguageTag("cs")));
    }

    @Test
    void getProvidedLocales_shouldReturnImmutableViewOfSupportedLocales() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        var locales = provider.getProvidedLocales();

        assertEquals(1, locales.size());
        assertTrue(locales.contains(Locale.forLanguageTag("cs")));
    }

    @Test
    void getProvidedLocales_shouldExposeCzechLocale() {
        CustomI18NProvider provider = new CustomI18NProvider(new ObjectMapper());

        assertEquals(1, provider.getProvidedLocales().size());
        assertTrue(provider.getProvidedLocales().contains(Locale.forLanguageTag("cs")));
    }
}

