package me.moonote.app.chatkeep.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.model.OAuthProvider;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserType;
import me.moonote.app.chatkeep.repository.UserRepository;

/**
 * Custom OIDC user service that handles OpenID Connect authentication (e.g., AWS Cognito).
 *
 * This service handles OIDC providers (which extend OAuth2 with ID tokens) and wraps the OIDC user
 * in our custom ChatKeepUserDetails for consistent user management.
 *
 * Similar to CustomOAuth2UserService but for OIDC-specific flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

  private final UserRepository userRepository;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    // Load the OIDC user from the provider
    OidcUser oidcUser = super.loadUser(userRequest);

    // Extract provider name (e.g., "cognito", "google")
    String providerName = userRequest.getClientRegistration().getRegistrationId().toLowerCase();

    // Extract provider-specific user ID (the "sub" claim from ID token)
    String providerId = oidcUser.getSubject();
    if (providerId == null) {
      throw new OAuth2AuthenticationException("Provider user ID (sub claim) not found");
    }

    // Extract email from OIDC user claims
    String email = oidcUser.getEmail();

    log.info("OIDC authentication: provider={}, sub={}, email={}", providerName, providerId, email);

    // Find or create user account
    User user = findOrCreateUser(providerName, providerId, email, oidcUser);

    // Return custom UserDetails wrapped around OidcUser
    return new OidcUserDetailsAdapter(new ChatKeepUserDetails(user), oidcUser);
  }

  /**
   * Find existing user by OAuth2 provider or create a new authenticated user.
   */
  private User findOrCreateUser(String providerName, String providerId, String email,
      OidcUser oidcUser) {
    // Try to find user by OAuth2 provider
    Optional<User> existingUser = userRepository.findByOAuthProvider(providerName, providerId);

    if (existingUser.isPresent()) {
      // User already exists, update last used timestamp
      User user = existingUser.get();
      updateProviderLastUsed(user, providerName, providerId);
      log.info("Found existing user: id={}, type={}", user.getId(), user.getUserType());
      return userRepository.save(user);
    }

    // Check if user exists by email (for account linking)
    if (email != null) {
      Optional<User> userByEmail = userRepository.findByEmail(email);
      if (userByEmail.isPresent()) {
        // User exists with this email, link the new OAuth2 provider
        User user = userByEmail.get();
        linkNewProvider(user, providerName, providerId, email);
        log.info("Linked provider {} to existing user: id={}", providerName, user.getId());
        return userRepository.save(user);
      }
    }

    // Create new authenticated user
    User newUser = createNewAuthenticatedUser(providerName, providerId, email, oidcUser);
    log.info("Created new authenticated user: id={}, email={}", newUser.getId(),
        newUser.getEmail());
    return newUser;
  }

  /**
   * Update the lastUsedAt timestamp for an existing OAuth2 provider.
   */
  private void updateProviderLastUsed(User user, String providerName, String providerId) {
    user.getOauthProviders().stream()
        .filter(p -> p.getProvider().equals(providerName) && p.getProviderId().equals(providerId))
        .findFirst().ifPresent(provider -> provider.setLastUsedAt(Instant.now()));
  }

  /**
   * Link a new OAuth2 provider to an existing user account.
   */
  private void linkNewProvider(User user, String providerName, String providerId, String email) {
    OAuthProvider newProvider =
        OAuthProvider.builder().provider(providerName).providerId(providerId).providerEmail(email)
            .linkedAt(Instant.now()).lastUsedAt(Instant.now()).build();

    user.getOauthProviders().add(newProvider);

    // If user was anonymous, upgrade to authenticated
    if (user.getUserType() == UserType.ANONYMOUS) {
      user.setUserType(UserType.AUTHENTICATED);
      user.setRegisteredAt(Instant.now());
      if (user.getEmail() == null) {
        user.setEmail(email);
      }
    }
  }

  /**
   * Create a new authenticated user with the OAuth2 provider.
   */
  private User createNewAuthenticatedUser(String providerName, String providerId, String email,
      OidcUser oidcUser) {
    OAuthProvider oauthProvider =
        OAuthProvider.builder().provider(providerName).providerId(providerId).providerEmail(email)
            .linkedAt(Instant.now()).lastUsedAt(Instant.now()).build();

    // Extract username/name from OIDC claims
    String username = extractUsername(oidcUser);

    User newUser = User.builder().email(email).username(username).userType(UserType.AUTHENTICATED)
        .oauthProviders(new ArrayList<>()).registeredAt(Instant.now()).build();

    newUser.getOauthProviders().add(oauthProvider);

    return userRepository.save(newUser);
  }

  /**
   * Extract username from OIDC user claims.
   */
  private String extractUsername(OidcUser oidcUser) {
    // Try standard OIDC claims
    String name = oidcUser.getFullName();
    if (name != null) return name;

    String preferredUsername = oidcUser.getPreferredUsername();
    if (preferredUsername != null) return preferredUsername;

    String givenName = oidcUser.getGivenName();
    if (givenName != null) return givenName;

    // Try Cognito-specific claims
    String cognitoUsername = oidcUser.getClaim("cognito:username");
    if (cognitoUsername != null) return cognitoUsername;

    // Fallback to email prefix
    String email = oidcUser.getEmail();
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf("@"));
    }

    return "User";
  }

}
