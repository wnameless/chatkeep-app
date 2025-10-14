package me.moonote.app.chatkeep.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Adapter that allows ChatKeepUserDetails to be used as an OidcUser.
 *
 * This is necessary because Spring Security OAuth2/OIDC expects OidcUser interface, but we want to
 * use our custom ChatKeepUserDetails for consistent user handling.
 *
 * Similar to OAuth2UserDetailsAdapter but implements the OidcUser interface for OpenID Connect
 * providers.
 */
public class OidcUserDetailsAdapter implements OidcUser {

  private final ChatKeepUserDetails userDetails;
  private final OidcUser oidcUser;

  public OidcUserDetailsAdapter(ChatKeepUserDetails userDetails, OidcUser oidcUser) {
    this.userDetails = userDetails;
    this.oidcUser = oidcUser;
  }

  /**
   * Get the underlying ChatKeepUserDetails
   */
  public ChatKeepUserDetails getUserDetails() {
    return userDetails;
  }

  @Override
  public Map<String, Object> getClaims() {
    return oidcUser.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return oidcUser.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return oidcUser.getIdToken();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oidcUser.getAttributes();
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
