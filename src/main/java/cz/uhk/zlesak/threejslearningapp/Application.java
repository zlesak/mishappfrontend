package cz.uhk.zlesak.threejslearningapp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.ui.Transport;
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
@Push(transport = Transport.WEBSOCKET_XHR)
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
     * Configures the ObjectMapper bean for JSON serialization and deserialization.
     * It registers the JavaTimeModule to handle Java 8 date/time types and
     * configures it to ignore unknown properties during deserialization.
     *
     * @return a configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
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