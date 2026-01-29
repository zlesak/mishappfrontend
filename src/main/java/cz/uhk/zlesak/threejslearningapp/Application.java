package cz.uhk.zlesak.threejslearningapp;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Main application class for the Three.js Learning App.
 * It sets up the Spring Boot application and provides RestClient and I18NProvider beans.
 * The application uses a custom theme named "threejslearningapp".
 */
@SpringBootApplication(scanBasePackages = "cz.uhk.zlesak.threejslearningapp")
@Theme(value = "threejslearningapp")
public class Application implements AppShellConfigurator {

    /**
     * Main method to run the Spring Boot application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Provides a RestClient bean for making RESTful web service calls.
     *
     * @return a new instance of RestClient
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    /**
     * Configures the application shell settings.
     * This method sets the favicon for the application as of now.
     *
     * @param settings initial application shell settings
     */
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "/icons/MISH_icon.ico", "256x256");
        settings.addLink("shortcut icon", "/icons/MISH_icon.ico");
        settings.addMetaTag("description", "MISH APP - moderní systém učení");
        settings.setPageTitle("MISH APP");
    }
}