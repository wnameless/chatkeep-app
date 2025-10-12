package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Embedded document representing an OAuth2 provider linked to a user account.
 *
 * Supports multiple OAuth2 providers (AWS Cognito, Google, Facebook, GitHub, etc.) per user,
 * allowing users to link multiple authentication methods to a single account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class OAuthProvider {

  /**
   * OAuth2 provider identifier (e.g., "cognito", "google", "facebook", "github")
   */
  String provider;

  /**
   * Provider-specific user identifier (the "sub" claim from OAuth2/JWT)
   */
  String providerId;

  /**
   * Email address associated with this OAuth2 provider
   */
  String providerEmail;

  /**
   * When this OAuth2 provider was first linked to the user account
   */
  Instant linkedAt;

  /**
   * Last time the user logged in using this OAuth2 provider
   */
  Instant lastUsedAt;

}
