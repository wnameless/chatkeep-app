package me.moonote.app.chatkeep.security;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserType;

/**
 * Custom UserDetails implementation that wraps both anonymous and authenticated users.
 *
 * Provides a unified interface for Spring Security to handle both: - Anonymous users (identified
 * by UUID from browser LocalStorage) - Authenticated users (logged in via OAuth2 providers)
 */
public class ChatKeepUserDetails implements UserDetails {

  private final User user;

  public ChatKeepUserDetails(User user) {
    this.user = user;
  }

  /**
   * Get the underlying User entity
   */
  public User getUser() {
    return user;
  }

  /**
   * Get the user's MongoDB ID
   */
  public String getUserId() {
    return user.getId();
  }

  /**
   * Get the user type (ANONYMOUS or AUTHENTICATED)
   */
  public UserType getUserType() {
    return user.getUserType();
  }

  /**
   * Get the anonymous UUID (for anonymous users)
   */
  public String getAnonymousUuid() {
    return user.getAnonymousUuid();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // All users have ROLE_USER authority
    // Anonymous users get ROLE_ANONYMOUS, authenticated users get ROLE_AUTHENTICATED
    if (user.getUserType() == UserType.ANONYMOUS) {
      return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
    } else {
      return Collections.singletonList(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
    }
  }

  @Override
  public String getPassword() {
    // No password for OAuth2-based authentication
    return null;
  }

  @Override
  public String getUsername() {
    // For anonymous users, return the UUID
    // For authenticated users, return the email
    if (user.getUserType() == UserType.ANONYMOUS) {
      return user.getAnonymousUuid();
    } else {
      return user.getEmail() != null ? user.getEmail() : user.getUsername();
    }
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
