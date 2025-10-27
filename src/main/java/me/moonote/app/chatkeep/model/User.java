package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * User entity supporting both anonymous (UUID-based) and authenticated (OAuth2) users.
 *
 * Anonymous users are identified by a browser-generated UUID stored in LocalStorage. Authenticated
 * users can link multiple OAuth2 providers (AWS Cognito, Google, Facebook, GitHub, etc.) to a
 * single account.
 *
 * When an anonymous user registers, their ChatNotes are migrated to the authenticated user account
 * while preserving the original anonymousUuid for audit purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@FieldDefaults(level = PRIVATE)
public class User {

  @Id
  String id;

  /**
   * Browser-generated UUID for anonymous users (stored in LocalStorage).
   *
   * For authenticated users who were previously anonymous, this field preserves the original UUID
   * for audit trail purposes.
   */
  @Indexed(unique = true, sparse = true)
  String anonymousUuid;

  /**
   * Primary email address (from OAuth2 provider or manual registration)
   */
  @Indexed(unique = true, sparse = true)
  String email;

  /**
   * Display name or username
   */
  String username;

  /**
   * User type: ANONYMOUS (unregistered) or AUTHENTICATED (registered via OAuth2)
   */
  @Builder.Default
  UserType userType = UserType.ANONYMOUS;

  /**
   * OAuth2 providers linked to this user account.
   *
   * Supports multiple providers per user (e.g., user can link both Google and GitHub).
   */
  @Builder.Default
  List<OAuthProvider> oauthProviders = new ArrayList<>();

  /**
   * User preferences for UI customization (theme, language, etc.)
   */
  @Builder.Default
  UserPreferences preferences = UserPreferences.builder().build();

  /**
   * When the user upgraded from anonymous to authenticated (via OAuth2 registration)
   */
  Instant registeredAt;

  /**
   * Account creation timestamp (when user first interacted with the system)
   */
  @CreatedDate
  Instant createdAt;

  /**
   * Last account update timestamp
   */
  @LastModifiedDate
  Instant updatedAt;

}
