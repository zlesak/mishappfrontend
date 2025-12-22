package cz.uhk.zlesak.threejslearningapp.security;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import cz.uhk.zlesak.threejslearningapp.views.auth.LoginView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for the application.
 * Sets up HTTP security, user details management, and password encoding.
 * Currently uses an in-memory user details manager with predefined users. TODO After BE is ready, switch to persistent user store.
 */
@Slf4j
@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }

    /**
     * Configures the password encoder to use BCrypt hashing.
     * @return the PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures an in-memory user details manager with predefined users.
     * @return the UserDetailsManager instance
     */
    @Bean
    public UserDetailsManager userDetailsManager() {
        log.warn("NOT FOR PRODUCTION: Using in-memory user details manager!");
        var student = User.withUsername("student")
                .password(passwordEncoder().encode("student"))
                .roles("STUDENT")
                .build();
        var admin = User.withUsername("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("ADMIN")
                .build();
        var ucitel = User.withUsername("ucitel")
                .password(passwordEncoder().encode("ucitel"))
                .roles("TEACHER")
                .build();
        return new InMemoryUserDetailsManager(student, admin, ucitel);
    }
}
