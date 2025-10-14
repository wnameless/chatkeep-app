package me.moonote.app.chatkeep.security;

import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserType;
import me.moonote.app.chatkeep.repository.UserRepository;

/**
 * Security filter that handles anonymous user authentication via httpOnly cookies.
 *
 * This filter: 1. Checks for ANONYMOUS_SESSION httpOnly cookie 2. If missing, generates UUID and
 * sets httpOnly cookie (1 year expiry) 3. If exists, finds or creates anonymous user in database 4.
 * Sets Spring Security context with anonymous user details
 *
 * The UUID is server-generated and stored in httpOnly cookie for security. JavaScript cannot access
 * the cookie (XSS protection).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnonymousCookieFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  private static final String ANONYMOUS_COOKIE_NAME = "ANONYMOUS_SESSION";
  private static final int COOKIE_MAX_AGE = 31536000; // 1 year in seconds

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    // Skip anonymous authentication if user is already authenticated via OAuth2
    // Check if authentication exists, is authenticated, and is NOT Spring's default
    // AnonymousAuthenticationToken
    boolean isAlreadyAuthenticated = authentication != null && authentication.isAuthenticated()
        && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);

    if (isAlreadyAuthenticated) {
      log.debug("Skipping anonymous authentication - user already authenticated: {}",
          authentication.getName());
      filterChain.doFilter(request, response);
      return;
    }

    // Check for anonymous session cookie
    String anonymousUuid = getAnonymousCookie(request);

    if (anonymousUuid == null) {
      // No cookie found - generate new UUID and set cookie
      anonymousUuid = UUID.randomUUID().toString();
      setAnonymousCookie(response, anonymousUuid);
      log.debug("Created new anonymous session: {}", anonymousUuid);
    }

    // Authenticate anonymous user
    try {
      // Find or create anonymous user
      User user = findOrCreateAnonymousUser(anonymousUuid);

      // Create authentication token
      ChatKeepUserDetails userDetails = new ChatKeepUserDetails(user);
      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      // Set authentication in security context
      SecurityContextHolder.getContext().setAuthentication(authToken);

      log.debug("Anonymous user authenticated: {}", anonymousUuid);
    } catch (Exception e) {
      log.error("Failed to authenticate anonymous user: {}", anonymousUuid, e);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Get anonymous UUID from httpOnly cookie.
   */
  private String getAnonymousCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (ANONYMOUS_COOKIE_NAME.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Set anonymous UUID in httpOnly cookie.
   */
  private void setAnonymousCookie(HttpServletResponse response, String uuid) {
    Cookie cookie = new Cookie(ANONYMOUS_COOKIE_NAME, uuid);
    cookie.setHttpOnly(true); // Cannot be accessed via JavaScript (XSS protection)
    cookie.setSecure(false); // TODO: Set to true in production (HTTPS only)
    cookie.setPath("/"); // Available site-wide
    cookie.setMaxAge(COOKIE_MAX_AGE); // 1 year
    // cookie.setAttribute("SameSite", "Lax"); // CSRF protection (Spring Boot 3+ auto-sets this)

    response.addCookie(cookie);
  }

  /**
   * Find existing anonymous user or create a new one.
   */
  private User findOrCreateAnonymousUser(String anonymousUuid) {
    return userRepository.findByAnonymousUuid(anonymousUuid).orElseGet(() -> {
      // Create new anonymous user
      User newUser = User.builder().anonymousUuid(anonymousUuid).userType(UserType.ANONYMOUS)
          .username("Anonymous User").build();

      return userRepository.save(newUser);
    });
  }

  /**
   * Delete anonymous cookie (called after OAuth2 login).
   */
  public static void deleteAnonymousCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(ANONYMOUS_COOKIE_NAME, null);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(0); // Delete immediately
    response.addCookie(cookie);
  }

  /**
   * Get anonymous UUID from cookie (for migration purposes).
   */
  public static String getAnonymousUuidFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (ANONYMOUS_COOKIE_NAME.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

}
