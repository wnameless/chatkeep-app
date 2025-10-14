package me.moonote.app.chatkeep.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.OAuthProvider;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserType;
import me.moonote.app.chatkeep.repository.ChatNoteRepository;
import me.moonote.app.chatkeep.repository.UserRepository;

/**
 * Service for migrating anonymous users to authenticated users.
 *
 * When an anonymous user (identified by UUID) registers via OAuth2, this service: 1. Finds the
 * anonymous user by UUID 2. Transfers ownership of all ChatNotes to the authenticated user 3.
 * Updates the user account to authenticated status 4. Preserves the original anonymousUuid for
 * audit purposes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserMigrationService {

  private final UserRepository userRepository;
  private final ChatNoteRepository chatNoteRepository;

  /**
   * Migrate an anonymous user to an authenticated user.
   *
   * This method is called when an anonymous user logs in via OAuth2 for the first time.
   *
   * @param anonymousUuid The browser-generated UUID of the anonymous user
   * @param provider OAuth2 provider name (e.g., "cognito", "google", "facebook")
   * @param providerId Provider-specific user identifier (the "sub" claim)
   * @param email Email address from OAuth2 provider
   * @param username Display name from OAuth2 provider
   * @return The authenticated user (existing or newly created)
   */
  @Transactional
  public User migrateAnonymousUser(String anonymousUuid, String provider, String providerId,
      String email, String username) {
    log.info("Starting migration for anonymous user: {}", anonymousUuid);

    // Find the anonymous user
    User anonymousUser = userRepository.findByAnonymousUuid(anonymousUuid).orElse(null);

    // Find or create authenticated user
    User authenticatedUser = findOrCreateAuthenticatedUser(provider, providerId, email, username);

    // If anonymous user exists, transfer their ChatNotes
    if (anonymousUser != null && !anonymousUser.getId().equals(authenticatedUser.getId())) {
      transferChatNotes(anonymousUser.getId(), authenticatedUser.getId());

      // Preserve the anonymousUuid in the authenticated user for audit trail
      if (authenticatedUser.getAnonymousUuid() == null) {
        authenticatedUser.setAnonymousUuid(anonymousUuid);
        userRepository.save(authenticatedUser);
      }

      // Delete the anonymous user record
      userRepository.delete(anonymousUser);
      log.info("Migrated {} ChatNotes from anonymous user {} to authenticated user {}",
          chatNoteRepository.findByUserId(authenticatedUser.getId()).size(), anonymousUuid,
          authenticatedUser.getId());
    }

    return authenticatedUser;
  }

  /**
   * Find existing authenticated user by OAuth2 provider or create a new one.
   */
  private User findOrCreateAuthenticatedUser(String provider, String providerId, String email,
      String username) {
    // Try to find user by OAuth2 provider
    return userRepository.findByOAuthProvider(provider, providerId).orElseGet(() -> {
      // Check if user exists by email (for account linking)
      if (email != null) {
        User userByEmail = userRepository.findByEmail(email).orElse(null);
        if (userByEmail != null) {
          // Link the new OAuth2 provider to existing user
          linkOAuthProvider(userByEmail, provider, providerId, email);
          return userRepository.save(userByEmail);
        }
      }

      // Create new authenticated user
      return createNewAuthenticatedUser(provider, providerId, email, username);
    });
  }

  /**
   * Link a new OAuth2 provider to an existing user.
   */
  private void linkOAuthProvider(User user, String provider, String providerId, String email) {
    OAuthProvider oauthProvider = OAuthProvider.builder().provider(provider).providerId(providerId)
        .providerEmail(email).linkedAt(Instant.now()).lastUsedAt(Instant.now()).build();

    user.getOauthProviders().add(oauthProvider);

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
   * Create a new authenticated user.
   */
  private User createNewAuthenticatedUser(String provider, String providerId, String email,
      String username) {
    OAuthProvider oauthProvider = OAuthProvider.builder().provider(provider).providerId(providerId)
        .providerEmail(email).linkedAt(Instant.now()).lastUsedAt(Instant.now()).build();

    User newUser = User.builder().email(email).username(username).userType(UserType.AUTHENTICATED)
        .registeredAt(Instant.now()).build();

    newUser.getOauthProviders().add(oauthProvider);

    return userRepository.save(newUser);
  }

  /**
   * Transfer all ChatNotes from one user to another.
   */
  private void transferChatNotes(String fromUserId, String toUserId) {
    List<ChatNote> chatNotes = chatNoteRepository.findByUserId(fromUserId);

    for (ChatNote chatNote : chatNotes) {
      chatNote.setUserId(toUserId);
    }

    chatNoteRepository.saveAll(chatNotes);
    log.info("Transferred {} ChatNotes from user {} to user {}", chatNotes.size(), fromUserId,
        toUserId);
  }

}
