package com.demo.bpm.config;

import com.demo.bpm.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/logout", "/api/auth/clear-session", "/h2-console/**", "/actuator/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" +
                        authException.getMessage().replace("\"", "'") + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" +
                        accessDeniedException.getMessage().replace("\"", "'") + "\"}");
                })
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Level 1: Regular users
        var user1 = User.builder()
            .username("user1")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();

        var user2 = User.builder()
            .username("user2")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();

        // Level 2: Supervisors (can approve up to $1000)
        var supervisor1 = User.builder()
            .username("supervisor1")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER")
            .build();

        var supervisor2 = User.builder()
            .username("supervisor2")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER")
            .build();

        // Level 3: Managers (can approve up to $5000, manager of supervisors)
        var manager1 = User.builder()
            .username("manager1")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER")
            .build();

        var manager2 = User.builder()
            .username("manager2")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER")
            .build();

        // Level 4: Directors (can approve up to $20000, manager of managers)
        var director1 = User.builder()
            .username("director1")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER")
            .build();

        // Level 5: Executives (final approval authority)
        var executive1 = User.builder()
            .username("executive1")
            .password(passwordEncoder().encode("password"))
            .roles("EXECUTIVE", "DIRECTOR", "MANAGER", "SUPERVISOR", "USER")
            .build();

        return new InMemoryUserDetailsManager(
            user1, user2, supervisor1, supervisor2,
            manager1, manager2, director1, executive1
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
