package com.embabel.urbot.security;

import com.embabel.agent.rag.service.NamedEntityDataRepository;
import com.embabel.urbot.user.DummyUrbotUserService;
import com.embabel.urbot.user.UrbotUser;
import com.embabel.urbot.user.UrbotUserService;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for Urbot.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration extends VaadinWebSecurity {

    private final UrbotUserService userService;

    SecurityConfiguration(@Lazy UrbotUserService userService) {
        this.userService = userService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
        );
        super.configure(http);
        setLoginView(http, LoginView.class);

        http.userDetailsService(userService);

        // Permit WebSocket upgrade requests used by Vaadin Push (Atmosphere)
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers(request ->
                        "websocket".equalsIgnoreCase(request.getHeader("Upgrade"))
                )
        );

        // Configure logout
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .permitAll()
        );
    }

    @Bean
    UrbotUserService userService(NamedEntityDataRepository entityRepository) {
        return new DummyUrbotUserService(entityRepository,
                new UrbotUser("1", "Alice Agu", "alice"),
                new UrbotUser("2", "Ben Blossom", "ben")
        );
    }
}
