package me.moonote.app.chatkeep.security;

import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.service.UserMigrationService;

/**
 * Handles successful OAuth2 authentication and anonymous user migration.
 *
 * This handler: 1. Checks for anonymous session cookie 2. If exists, migrates anonymous user's
 * ChatNotes to authenticated user 3. Deletes the anonymous cookie 4. Redirects to home page
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserMigrationService userMigrationService;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // Check for anonymous session cookie
    String anonymousUuid = AnonymousCookieFilter.getAnonymousUuidFromCookie(request);

    if (anonymousUuid != null && !anonymousUuid.isBlank()) {
      try {
        // Extract OAuth2 user info
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String username = extractUsername(oauth2User);

        // Extract provider name from authentication
        String provider = extractProviderName(authentication);

        // Migrate anonymous user to authenticated user
        log.info("Migrating anonymous user {} to authenticated user", anonymousUuid);
        userMigrationService.migrateAnonymousUser(anonymousUuid, provider, providerId, email,
            username);

        // Delete anonymous cookie (user is now authenticated)
        AnonymousCookieFilter.deleteAnonymousCookie(response);

        log.info("Successfully migrated anonymous user {} and deleted cookie", anonymousUuid);
      } catch (Exception e) {
        // Delete anonymous cookie (user is now authenticated)
        AnonymousCookieFilter.deleteAnonymousCookie(response);
        log.error("Failed to migrate anonymous user", e);
        // Continue with authentication even if migration fails
      }
    }

    // Set default redirect URL
    setDefaultTargetUrl("/");

    super.onAuthenticationSuccess(request, response, authentication);
  }

  /**
   * Extract OAuth2 provider name from authentication object.
   */
  private String extractProviderName(Authentication authentication) {
    // Try to extract from authorities
    String authString = authentication.getAuthorities().toString();
    if (authString.contains("OAUTH2_USER")) {
      // Provider name is typically in the authority string
      // This is a fallback - normally we'd get it from the registration ID
      return "oauth2";
    }
    return "unknown";
  }

  /**
   * Extract username from OAuth2 user attributes.
   */
  private String extractUsername(OAuth2User oauth2User) {
    String name = oauth2User.getAttribute("name");
    if (name != null) return name;

    String username = oauth2User.getAttribute("preferred_username");
    if (username != null) return username;

    String email = oauth2User.getAttribute("email");
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf("@"));
    }

    return "User";
  }

}
