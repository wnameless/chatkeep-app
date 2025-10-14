package me.moonote.app.chatkeep.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for Spring Security operations.
 *
 * Provides convenient methods for: - Extracting current user from SecurityContext - Getting user
 * ID for database queries - Checking authentication status
 */
public class SecurityUtils {

  private SecurityUtils() {
    // Utility class, prevent instantiation
  }

  /**
   * Get the currently authenticated user's ChatKeepUserDetails.
   *
   * @return ChatKeepUserDetails if authenticated, null otherwise
   */
  public static ChatKeepUserDetails getCurrentUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    // Handle OidcUserDetailsAdapter (from OIDC login - AWS Cognito, Google, etc.)
    if (principal instanceof OidcUserDetailsAdapter oidcAdapter) {
      return oidcAdapter.getUserDetails();
    }

    // Handle OAuth2UserDetailsAdapter (from OAuth2 login)
    if (principal instanceof OAuth2UserDetailsAdapter oauth2Adapter) {
      return oauth2Adapter.getUserDetails();
    }

    // Handle direct ChatKeepUserDetails (from anonymous filter)
    if (principal instanceof ChatKeepUserDetails userDetails) {
      return userDetails;
    }

    return null;
  }

  /**
   * Get the current user's MongoDB ID.
   *
   * This is the primary method for getting user ID for database queries.
   *
   * @return User ID (MongoDB ObjectId) if authenticated, null otherwise
   */
  public static String getCurrentUserId() {
    ChatKeepUserDetails userDetails = getCurrentUserDetails();
    return userDetails != null ? userDetails.getUserId() : null;
  }

  /**
   * Get the current user's username.
   *
   * For anonymous users, this returns the UUID. For authenticated users, this returns the email or
   * username.
   *
   * @return Username/email/UUID if authenticated, null otherwise
   */
  public static String getCurrentUsername() {
    ChatKeepUserDetails userDetails = getCurrentUserDetails();
    return userDetails != null ? userDetails.getUsername() : null;
  }

  /**
   * Check if the current user is authenticated (not anonymous).
   *
   * @return true if user is authenticated via OAuth2, false if anonymous or not authenticated
   */
  public static boolean isAuthenticated() {
    ChatKeepUserDetails userDetails = getCurrentUserDetails();
    return userDetails != null
        && userDetails.getUserType() == me.moonote.app.chatkeep.model.UserType.AUTHENTICATED;
  }

  /**
   * Check if the current user is anonymous.
   *
   * @return true if user is anonymous (UUID-based), false otherwise
   */
  public static boolean isAnonymous() {
    ChatKeepUserDetails userDetails = getCurrentUserDetails();
    return userDetails != null
        && userDetails.getUserType() == me.moonote.app.chatkeep.model.UserType.ANONYMOUS;
  }

  /**
   * Check if any user is logged in (either authenticated or anonymous).
   *
   * @return true if there's a user in the security context
   */
  public static boolean hasUser() {
    return getCurrentUserDetails() != null;
  }

}
