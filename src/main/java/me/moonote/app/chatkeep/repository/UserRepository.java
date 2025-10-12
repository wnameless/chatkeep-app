package me.moonote.app.chatkeep.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

  /**
   * Find user by anonymous UUID (browser-generated UUID stored in LocalStorage)
   */
  Optional<User> findByAnonymousUuid(String anonymousUuid);

  /**
   * Check if an anonymous UUID already exists
   */
  boolean existsByAnonymousUuid(String anonymousUuid);

  /**
   * Find user by primary email address
   */
  Optional<User> findByEmail(String email);

  /**
   * Find user by OAuth2 provider and provider-specific user ID.
   *
   * This query searches within the embedded oauthProviders array for a matching provider and
   * providerId combination.
   *
   * @param provider OAuth2 provider name (e.g., "cognito", "google", "facebook")
   * @param providerId Provider-specific user identifier (the "sub" claim)
   * @return Optional User if found
   */
  @Query("{ 'oauthProviders': { $elemMatch: { 'provider': ?0, 'providerId': ?1 } } }")
  Optional<User> findByOAuthProvider(String provider, String providerId);

  /**
   * Check if an OAuth2 provider is already linked to any user account.
   *
   * Used to prevent duplicate OAuth2 provider linkages across different users.
   *
   * @param provider OAuth2 provider name
   * @param providerId Provider-specific user identifier
   * @return true if this provider is already linked to a user
   */
  @Query(value = "{ 'oauthProviders': { $elemMatch: { 'provider': ?0, 'providerId': ?1 } } }",
      exists = true)
  boolean existsByOAuthProvider(String provider, String providerId);

}
