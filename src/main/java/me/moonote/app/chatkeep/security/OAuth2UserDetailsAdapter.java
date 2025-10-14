package me.moonote.app.chatkeep.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Adapter that allows ChatKeepUserDetails to be used as an OAuth2User.
 *
 * This is necessary because Spring Security OAuth2 expects OAuth2User interface, but we want to use
 * our custom ChatKeepUserDetails for consistent user handling.
 */
public class OAuth2UserDetailsAdapter implements OAuth2User {

  private final ChatKeepUserDetails userDetails;
  private final OAuth2User oauth2User;

  public OAuth2UserDetailsAdapter(ChatKeepUserDetails userDetails, OAuth2User oauth2User) {
    this.userDetails = userDetails;
    this.oauth2User = oauth2User;
  }

  /**
   * Get the underlying ChatKeepUserDetails
   */
  public ChatKeepUserDetails getUserDetails() {
    return userDetails;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oauth2User.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return userDetails.getAuthorities();
  }

  @Override
  public String getName() {
    return userDetails.getUsername();
  }

}
