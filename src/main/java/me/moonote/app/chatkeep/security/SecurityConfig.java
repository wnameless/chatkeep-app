package me.moonote.app.chatkeep.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration supporting both OAuth2 and anonymous authentication.
 *
 * Security features: - OAuth2 login with multiple providers (AWS Cognito, Google, Facebook, etc.)
 * - Anonymous user authentication via UUID from browser - Public endpoints for static resources and
 * health checks - Protected endpoints requiring authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
  private final AnonymousUserFilter anonymousUserFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF configuration
        .csrf(csrf -> csrf.disable()) // TODO: Enable CSRF in production

        // Authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/", "/login", "/error", "/webjars/**", "/css/**", "/js/**",
                "/images/**", "/favicon.ico")
            .permitAll()
            // API endpoints - allow both authenticated and anonymous users
            .requestMatchers("/api/**").permitAll()
            // All other requests require authentication
            .anyRequest().authenticated())

        // OAuth2 login configuration
        .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(
            userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oauth2AuthenticationSuccessHandler).loginPage("/login").permitAll())

        // Logout configuration
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())

        // Add anonymous user filter before standard authentication
        .addFilterBefore(anonymousUserFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

}
