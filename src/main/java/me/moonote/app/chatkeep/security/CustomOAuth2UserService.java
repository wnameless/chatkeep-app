package me.moonote.app.chatkeep.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import me.moonote.app.chatkeep.model.OAuthProvider;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserType;
import me.moonote.app.chatkeep.repository.UserRepository;

/**
 * Custom OAuth2 user service that handles multi-provider authentication.
 *
 * Supports multiple OAuth2 providers (AWS Cognito, Google, Facebook, GitHub, etc.) and allows
 * users to link multiple providers to a single account.
 *
 * This service: 1. Extracts provider name and user ID from OAuth2 authentication 2. Finds or
 * creates user account 3. Links the OAuth2 provider to the user account 4. Returns
 * ChatKeepUserDetails for Spring Security
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // Load the OAuth2 user from the provider
    OAuth2User oauth2User = super.loadUser(userRequest);

    // Extract provider name (e.g., "cognito", "google", "facebook")
    String providerName =
        userRequest.getClientRegistration().getRegistrationId().toLowerCase();

    // Extract provider-specific user ID (the "sub" claim from JWT)
    String providerId = oauth2User.getAttribute("sub");
    if (providerId == null) {
      throw new OAuth2AuthenticationException("Provider user ID (sub claim) not found");
    }

    // Extract email from OAuth2 user attributes
    String email = oauth2User.getAttribute("email");

    // Find or create user account
    User user = findOrCreateUser(providerName, providerId, email, oauth2User);

    // Return custom UserDetails wrapped around OAuth2User
    return new OAuth2UserDetailsAdapter(new ChatKeepUserDetails(user), oauth2User);
  }

  /**
   * Find existing user by OAuth2 provider or create a new authenticated user.
   */
  private User findOrCreateUser(String providerName, String providerId, String email,
      OAuth2User oauth2User) {
    // Try to find user by OAuth2 provider
    Optional<User> existingUser = userRepository.findByOAuthProvider(providerName, providerId);

    if (existingUser.isPresent()) {
      // User already exists, update last used timestamp
      User user = existingUser.get();
      updateProviderLastUsed(user, providerName, providerId);
      return userRepository.save(user);
    }

    // Check if user exists by email (for account linking)
    if (email != null) {
      Optional<User> userByEmail = userRepository.findByEmail(email);
      if (userByEmail.isPresent()) {
        // User exists with this email, link the new OAuth2 provider
        User user = userByEmail.get();
        linkNewProvider(user, providerName, providerId, email);
        return userRepository.save(user);
      }
    }

    // Create new authenticated user
    return createNewAuthenticatedUser(providerName, providerId, email, oauth2User);
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
    OAuthProvider newProvider = OAuthProvider.builder().provider(providerName)
        .providerId(providerId).providerEmail(email).linkedAt(Instant.now())
        .lastUsedAt(Instant.now()).build();

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
      OAuth2User oauth2User) {
    OAuthProvider oauthProvider = OAuthProvider.builder().provider(providerName)
        .providerId(providerId).providerEmail(email).linkedAt(Instant.now())
        .lastUsedAt(Instant.now()).build();

    // Extract username/name from OAuth2 attributes
    String username = extractUsername(oauth2User);

    User newUser = User.builder().email(email).username(username)
        .userType(UserType.AUTHENTICATED).oauthProviders(new ArrayList<>())
        .registeredAt(Instant.now()).build();

    newUser.getOauthProviders().add(oauthProvider);

    return userRepository.save(newUser);
  }

  /**
   * Extract username from OAuth2 user attributes.
   */
  private String extractUsername(OAuth2User oauth2User) {
    // Try common attribute names
    String name = oauth2User.getAttribute("name");
    if (name != null)
      return name;

    String username = oauth2User.getAttribute("preferred_username");
    if (username != null)
      return username;

    String givenName = oauth2User.getAttribute("given_name");
    if (givenName != null)
      return givenName;

    // Fallback to email prefix
    String email = oauth2User.getAttribute("email");
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf("@"));
    }

    return "User";
  }

}
