package cl.casero.migration.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final PinAuthenticationProvider pinAuthenticationProvider;

    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PinAuthenticationFilter pinAuthenticationFilter(
        AuthenticationManager authenticationManager,
        HttpSessionSecurityContextRepository contextRepository,
        LoginSuccessHandler loginSuccessHandler,
        LoginFailureHandler loginFailureHandler
    ) {
        PinAuthenticationFilter filter = new PinAuthenticationFilter();

        filter.setSecurityContextRepository(contextRepository);
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(loginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailureHandler);

        return filter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        HttpSessionSecurityContextRepository contextRepository,
        PinAuthenticationFilter pinAuthenticationFilter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .authenticationProvider(pinAuthenticationProvider)
            .addFilterAt(pinAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/login", "/css/**", "/js/**", "/icons/**", "/favicon.ico").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/login?error")))
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        boolean timeout = request.getParameter("timeout") != null;
                        String contextPath = request.getContextPath();
                        String target = timeout ? "/login?timeout" : "/login?logout";
                        response.sendRedirect(contextPath + target);
                    })
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true))
            .formLogin(AbstractHttpConfigurer::disable);
            
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    LoginSuccessHandler loginSuccessHandler(cl.casero.migration.service.AuditEventService auditEventService) {
        return new LoginSuccessHandler(auditEventService);
    }

    @Bean
    LoginFailureHandler loginFailureHandler(cl.casero.migration.service.AuditEventService auditEventService) {
        return new LoginFailureHandler(auditEventService);
    }
}
