package me.moonote.app.chatkeep.security;

import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.service.UserMigrationService;

/**
 * Handles successful OAuth2 authentication and anonymous user migration.
 *
 * This handler: 1. Checks if there's an anonymous UUID in the session 2. If yes, migrates the
 * anonymous user's ChatNotes to the authenticated user 3. Clears the anonymous UUID from the
 * session 4. Redirects to the home page
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserMigrationService userMigrationService;

  private static final String ANONYMOUS_UUID_SESSION_KEY = "anonymousUuid";

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    HttpSession session = request.getSession(false);

    // Check if there's an anonymous UUID in the session
    if (session != null) {
      String anonymousUuid = (String) session.getAttribute(ANONYMOUS_UUID_SESSION_KEY);

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

          // Clear anonymous UUID from session
          session.removeAttribute(ANONYMOUS_UUID_SESSION_KEY);
          log.info("Successfully migrated anonymous user {} to authenticated user", anonymousUuid);
        } catch (Exception e) {
          log.error("Failed to migrate anonymous user", e);
          // Continue with authentication even if migration fails
        }
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
    if (name != null)
      return name;

    String username = oauth2User.getAttribute("preferred_username");
    if (username != null)
      return username;

    String email = oauth2User.getAttribute("email");
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf("@"));
    }

    return "User";
  }

}
