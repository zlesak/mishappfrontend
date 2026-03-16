package cz.uhk.zlesak.threejslearningapp.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ApplicationBrowserE2ETest {
    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void startPlaywright() {
        assumeTrue(LivePlaywrightSupport.isReachable(LivePlaywrightSupport.BASE_URL),
                () -> "E2E base URL is not reachable: " + LivePlaywrightSupport.BASE_URL);

        playwright = Playwright.create();
        browser = LivePlaywrightSupport.startBrowser(playwright);
    }

    @AfterAll
    static void stopPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContext() {
        context = LivePlaywrightSupport.createContext(browser);
        page = LivePlaywrightSupport.createPage(context);
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void anonymousAndStudentUsersShouldBeBlockedFromTeacherRoutes() {
        page.navigate(LivePlaywrightSupport.BASE_URL + "/administration");
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Sign in to your account")).waitFor();

        LivePlaywrightSupport.loginAsStudent(page);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("bart (b) bart")).click();
        assertEquals(0, page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Administrační centrum")).count());

        page.navigate(LivePlaywrightSupport.BASE_URL + "/createChapter");
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Přístup odepřen")).waitFor();
        assertTrue(page.getByText("Nemáte oprávnění pro přístup k této stránce.").isVisible());
    }

    @Test
    void cookiesConsentAndThemeShouldPersistInBrowser() {
        LivePlaywrightSupport.navigateHome(page);

        LivePlaywrightSupport.acceptCookiesIfVisible(page);
        LivePlaywrightSupport.assertCookieValue(page, "cookieConsent", "accepted");

        page.locator(".theme-mode-toggle").click();
        LivePlaywrightSupport.assertCookieValue(page, "themeMode", "dark");
        LivePlaywrightSupport.assertBodyTheme(page, "dark");

        page.reload();
        page.locator(".theme-mode-toggle").waitFor();
        LivePlaywrightSupport.assertCookieValue(page, "themeMode", "dark");
        LivePlaywrightSupport.assertBodyTheme(page, "dark");
    }

    @Test
    void teacherShouldCreateListAndDeleteChapter() {
        String chapterName = LivePlaywrightSupport.uniqueName("E2E Kapitola");

        LivePlaywrightSupport.loginAsTeacher(page);
        page.navigate(LivePlaywrightSupport.BASE_URL + "/createChapter");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Nahrát Moodle ZIP")).waitFor();

        LivePlaywrightSupport.uploadViaChooser(page, "Nahrát Moodle ZIP", LivePlaywrightSupport.CHAPTER_ZIP);
        page.getByText("Kapitola byla úspěšně importována").waitFor();
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Vývoj koncového mozku")).waitFor();

        page.getByPlaceholder("Název").fill(chapterName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Vytvořit kapitolu")).click();

        page.waitForURL("**/chapter/**");
        page.getByText(chapterName, new Page.GetByTextOptions().setExact(true)).waitFor();

        LivePlaywrightSupport.openAdministration(page);
        LivePlaywrightSupport.searchCurrentListing(page, chapterName);
        LivePlaywrightSupport.deleteListedEntity(page, chapterName, "Smazat kapitolu");
        page.getByText("Kapitola byla úspěšně smazána").waitFor();

        page.getByPlaceholder("Hledat...").fill(chapterName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Hledat")).click();
        page.getByText("Nebyly nalezeny žádné položky.").waitFor();
    }

    @Test
    void teacherShouldCreateListAndDeleteModel() {
        String modelName = LivePlaywrightSupport.uniqueName("000 E2E Model");

        LivePlaywrightSupport.loginAsTeacher(page);
        page.navigate(LivePlaywrightSupport.BASE_URL + "/createModel");
        page.getByPlaceholder("Zadejte název modelu").fill(modelName);
        LivePlaywrightSupport.uploadViaChooser(page, "Nahrát soubor (.glb, .obj)", LivePlaywrightSupport.SIMPLE_GLB);
        page.getByText("simple_model.glb").waitFor();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Vytvořit model")).click();
        page.waitForURL("**/model/**");
        page.getByText(modelName, new Page.GetByTextOptions().setExact(true)).waitFor();

        LivePlaywrightSupport.openAdministration(page);
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Modely")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Vytvořit model")).waitFor();
        page.getByText(modelName, new Page.GetByTextOptions().setExact(true)).waitFor();
        LivePlaywrightSupport.deleteListedEntity(page, modelName, "Smazat model");
        page.getByText("Model byl úspěšně smazán").waitFor();
    }

    @Test
    void teacherShouldCreateListAndDeleteQuiz() {
        String quizName = LivePlaywrightSupport.uniqueName("E2E Kvíz");

        LivePlaywrightSupport.loginAsTeacher(page);
        page.navigate(LivePlaywrightSupport.BASE_URL + "/createQuiz");
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Název kvízu")).fill(quizName);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Popis")).fill("Browser E2E quiz");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Jedna správná odpověď")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Otevřená odpověď")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Přidat otázku")).click();
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Text otázky")).fill("Jak se jmenuje test?");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Přidat možnost")).click();
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Možnost 1")).fill("správná odpověď");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Vytvořit kvíz")).click();

        page.waitForURL("**/quizes");
        LivePlaywrightSupport.openAdministration(page);
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Kvízy")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Vytvořit kvíz")).waitFor();
        LivePlaywrightSupport.searchCurrentListing(page, quizName);
        LivePlaywrightSupport.deleteListedEntity(page, quizName, "Smazat kvíz");
        page.getByText("Kvíz byl úspěšně smazán").waitFor();
    }
}
