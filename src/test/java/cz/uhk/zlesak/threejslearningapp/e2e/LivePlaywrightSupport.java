package cz.uhk.zlesak.threejslearningapp.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LivePlaywrightSupport {
    public static final String BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8081");
    public static final String TEACHER_USERNAME = System.getenv().getOrDefault("E2E_TEACHER_USERNAME", "alice");
    public static final String TEACHER_PASSWORD = System.getenv().getOrDefault("E2E_TEACHER_PASSWORD", "password");
    public static final String STUDENT_USERNAME = System.getenv().getOrDefault("E2E_STUDENT_USERNAME", "bart");
    public static final String STUDENT_PASSWORD = System.getenv().getOrDefault("E2E_STUDENT_PASSWORD", "password");
    public static final Path SIMPLE_GLB = Paths.get("src", "test", "resources", "simple_model.glb").toAbsolutePath();
    public static final Path CHAPTER_ZIP = Paths.get("src", "test", "resources", "Koncový mozek - telencephalon.zip").toAbsolutePath();

    private LivePlaywrightSupport() {
    }

    public static Browser startBrowser(Playwright playwright) {
        return launchChromium(playwright);
    }

    public static BrowserContext createContext(Browser browser) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setBaseURL(BASE_URL));
        Page page = context.newPage();
        page.setDefaultTimeout(Duration.ofSeconds(20).toMillis());
        return context;
    }

    public static Page createPage(BrowserContext context) {
        return context.pages().getFirst();
    }

    public static void navigateHome(Page page) {
        page.navigate(BASE_URL);
        waitForHomePage(page);
    }

    public static void loginAsTeacher(Page page) {
        login(page, TEACHER_USERNAME, TEACHER_PASSWORD);
    }

    public static void loginAsStudent(Page page) {
        login(page, STUDENT_USERNAME, STUDENT_PASSWORD);
    }

    public static void acceptCookiesIfVisible(Page page) {
        Locator acceptButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Rozumím"));
        if (acceptButton.count() > 0 && acceptButton.first().isVisible()) {
            acceptButton.first().click();
        }
    }

    public static void openAdministration(Page page) {
        page.navigate(BASE_URL + "/administration");
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Kapitoly")).waitFor();
    }

    public static void searchCurrentListing(Page page, String text) {
        Locator searchField = page.getByPlaceholder("Hledat...");
        searchField.waitFor();
        searchField.fill(text);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Hledat")).click();
        page.getByText(text, new Page.GetByTextOptions().setExact(true)).waitFor();
    }

    public static void deleteListedEntity(Page page, String entityName, String confirmButtonLabel) {
        Locator entityCard = page.getByText(entityName, new Page.GetByTextOptions().setExact(true))
                .locator("xpath=ancestor::*[.//button[normalize-space()='Smazat']][1]");
        entityCard.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Smazat")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(confirmButtonLabel)).click();
    }

    public static void uploadViaChooser(Page page, String buttonLabel, Path file) {
        FileChooser fileChooser = page.waitForFileChooser(
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonLabel)).click()
        );
        fileChooser.setFiles(file);
    }

    public static void assertCookieValue(Page page, String cookieName, String expectedValue) {
        String value = (String) page.evaluate(
                "(cookieName) => {" +
                        " const prefix = cookieName + '=';" +
                        " const match = document.cookie.split('; ').find(item => item.startsWith(prefix));" +
                        " return match ? match.substring(prefix.length) : null;" +
                        "}",
                cookieName
        );
        assertEquals(expectedValue, value, "Unexpected cookie value for " + cookieName);
    }

    public static void assertBodyTheme(Page page, String expectedTheme) {
        String actualTheme = (String) page.evaluate("() => document.body.getAttribute('theme')");
        assertEquals(expectedTheme, actualTheme);
    }

    public static String uniqueName(String prefix) {
        return prefix + " " + System.currentTimeMillis();
    }

    public static boolean isReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void login(Page page, String username, String password) {
        navigateHome(page);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Přihlásit se")).click();
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username or email")).fill(username);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
        page.waitForURL(BASE_URL + "/**");
        page.getByRole(AriaRole.MENUITEM,
                new Page.GetByRoleOptions().setName(username + " (" + username.charAt(0) + ") " + username)).waitFor();
        acceptCookiesIfVisible(page);
    }

    private static void waitForHomePage(Page page) {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Kapitoly")).waitFor();
        assertTrue(page.url().startsWith(BASE_URL));
    }

    private static Browser launchChromium(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(System.getenv().getOrDefault("E2E_HEADLESS", "true")))
                .setTimeout(Duration.ofSeconds(30).toMillis());
        try {
            return playwright.chromium().launch(options);
        } catch (PlaywrightException primaryFailure) {
            return playwright.chromium().launch(options.setChannel("chrome"));
        }
    }
}

