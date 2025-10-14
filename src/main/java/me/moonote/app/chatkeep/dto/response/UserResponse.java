package me.moonote.app.chatkeep.dto.response;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for user information Used by /api/v1/user/me endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UserResponse {

  String id; // MongoDB ObjectId
  String anonymousUuid; // Browser UUID (null if authenticated without previous anonymous session)
  String email; // Email (null for anonymous users)
  String username; // Display name (null for anonymous users)
  String userType; // "ANONYMOUS" or "AUTHENTICATED"
  Instant registeredAt; // When user upgraded from anonymous to authenticated
  Instant createdAt;
  Instant updatedAt;

}
