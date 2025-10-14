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
 * - Anonymous user authentication via httpOnly cookies - Public endpoints for static resources and
 * health checks - Protected endpoints requiring authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final CustomOidcUserService customOidcUserService;
  private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
  private final AnonymousCookieFilter anonymousCookieFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF configuration - now enabled with cookie-based auth
        .csrf(csrf -> csrf.disable()) // TODO: Enable CSRF in production after thorough testing

        // Authorization rules
        .authorizeHttpRequests(auth -> auth
            // Allow all endpoints - anonymous users are handled by AnonymousCookieFilter
            .anyRequest().permitAll())

        // OAuth2 login configuration
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                // Handle regular OAuth2 providers
                .userService(customOAuth2UserService)
                // Handle OIDC providers (AWS Cognito, Google, etc.)
                .oidcUserService(customOidcUserService))
            .successHandler(oauth2AuthenticationSuccessHandler).loginPage("/login").permitAll())

        // Logout configuration
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())

        // Add anonymous cookie filter before standard authentication
        .addFilterBefore(anonymousCookieFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

}
