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
        // ===========================================
        // ENGINEERING DEPARTMENT
        // ===========================================
        // Level 1: Engineers
        var engUser1 = User.builder()
            .username("eng.john")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "ENGINEERING")
            .build();

        var engUser2 = User.builder()
            .username("eng.sarah")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "ENGINEERING")
            .build();

        // Level 2: Tech Lead
        var engSupervisor = User.builder()
            .username("eng.mike")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "ENGINEERING")
            .build();

        // Level 3: Engineering Manager
        var engManager = User.builder()
            .username("eng.lisa")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "ENGINEERING")
            .build();

        // ===========================================
        // FINANCE DEPARTMENT
        // ===========================================
        // Level 1: Accountants
        var finUser1 = User.builder()
            .username("fin.bob")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "FINANCE")
            .build();

        var finUser2 = User.builder()
            .username("fin.alice")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "FINANCE")
            .build();

        // Level 2: Senior Accountant
        var finSupervisor = User.builder()
            .username("fin.carol")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "FINANCE")
            .build();

        // Level 3: Finance Manager
        var finManager = User.builder()
            .username("fin.david")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "FINANCE")
            .build();

        // Level 4: Finance Director / CFO
        var finDirector = User.builder()
            .username("fin.cfo")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER", "FINANCE")
            .build();

        // ===========================================
        // HR DEPARTMENT
        // ===========================================
        // Level 1: HR Specialists
        var hrUser1 = User.builder()
            .username("hr.emma")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "HR")
            .build();

        var hrUser2 = User.builder()
            .username("hr.james")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "HR")
            .build();

        // Level 2: HR Business Partner
        var hrSupervisor = User.builder()
            .username("hr.nina")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "HR")
            .build();

        // Level 3: HR Manager
        var hrManager = User.builder()
            .username("hr.tom")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "HR")
            .build();

        // Level 4: HR Director / CHRO
        var hrDirector = User.builder()
            .username("hr.chro")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER", "HR")
            .build();

        // ===========================================
        // SALES DEPARTMENT
        // ===========================================
        // Level 1: Sales Representatives
        var salesUser1 = User.builder()
            .username("sales.kevin")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "SALES")
            .build();

        var salesUser2 = User.builder()
            .username("sales.maria")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "SALES")
            .build();

        // Level 2: Sales Team Lead
        var salesSupervisor = User.builder()
            .username("sales.peter")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "SALES")
            .build();

        // Level 3: Sales Manager
        var salesManager = User.builder()
            .username("sales.rachel")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "SALES")
            .build();

        // ===========================================
        // OPERATIONS DEPARTMENT
        // ===========================================
        // Level 1: Operations Analysts
        var opsUser1 = User.builder()
            .username("ops.steve")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "OPERATIONS")
            .build();

        var opsUser2 = User.builder()
            .username("ops.linda")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "OPERATIONS")
            .build();

        // Level 2: Operations Supervisor
        var opsSupervisor = User.builder()
            .username("ops.frank")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "OPERATIONS")
            .build();

        // Level 3: Operations Manager
        var opsManager = User.builder()
            .username("ops.grace")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "OPERATIONS")
            .build();

        // Level 4: Operations Director / COO
        var opsDirector = User.builder()
            .username("ops.coo")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER", "OPERATIONS")
            .build();

        // ===========================================
        // IT DEPARTMENT
        // ===========================================
        // Level 1: IT Support Specialists
        var itUser1 = User.builder()
            .username("it.alex")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "IT")
            .build();

        var itUser2 = User.builder()
            .username("it.diana")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "IT")
            .build();

        // Level 2: IT Team Lead
        var itSupervisor = User.builder()
            .username("it.henry")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "IT")
            .build();

        // Level 3: IT Manager
        var itManager = User.builder()
            .username("it.olivia")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "IT")
            .build();

        // Level 4: IT Director / CTO
        var itDirector = User.builder()
            .username("it.cto")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER", "IT")
            .build();

        // ===========================================
        // LEGAL / COMPLIANCE DEPARTMENT
        // ===========================================
        // Level 1: Legal Analysts
        var legalUser1 = User.builder()
            .username("legal.amy")
            .password(passwordEncoder().encode("password"))
            .roles("USER", "LEGAL")
            .build();

        // Level 2: Senior Legal Counsel
        var legalSupervisor = User.builder()
            .username("legal.ben")
            .password(passwordEncoder().encode("password"))
            .roles("SUPERVISOR", "USER", "LEGAL")
            .build();

        // Level 3: Legal Manager
        var legalManager = User.builder()
            .username("legal.claire")
            .password(passwordEncoder().encode("password"))
            .roles("MANAGER", "SUPERVISOR", "USER", "LEGAL")
            .build();

        // ===========================================
        // EXECUTIVE LEADERSHIP
        // ===========================================
        // Level 5: CEO
        var ceo = User.builder()
            .username("exec.ceo")
            .password(passwordEncoder().encode("password"))
            .roles("EXECUTIVE", "DIRECTOR", "MANAGER", "SUPERVISOR", "USER")
            .build();

        // ===========================================
        // LEGACY USERS (for backward compatibility)
        // ===========================================
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

        var director1 = User.builder()
            .username("director1")
            .password(passwordEncoder().encode("password"))
            .roles("DIRECTOR", "MANAGER", "SUPERVISOR", "USER")
            .build();

        var executive1 = User.builder()
            .username("executive1")
            .password(passwordEncoder().encode("password"))
            .roles("EXECUTIVE", "DIRECTOR", "MANAGER", "SUPERVISOR", "USER")
            .build();

        return new InMemoryUserDetailsManager(
            // Engineering
            engUser1, engUser2, engSupervisor, engManager,
            // Finance
            finUser1, finUser2, finSupervisor, finManager, finDirector,
            // HR
            hrUser1, hrUser2, hrSupervisor, hrManager, hrDirector,
            // Sales
            salesUser1, salesUser2, salesSupervisor, salesManager,
            // Operations
            opsUser1, opsUser2, opsSupervisor, opsManager, opsDirector,
            // IT
            itUser1, itUser2, itSupervisor, itManager, itDirector,
            // Legal
            legalUser1, legalSupervisor, legalManager,
            // Executive
            ceo,
            // Legacy users
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
