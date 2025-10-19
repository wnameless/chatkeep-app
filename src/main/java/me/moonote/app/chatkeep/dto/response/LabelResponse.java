package me.moonote.app.chatkeep.dto.response;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for label information. Used by /api/v1/labels endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class LabelResponse {

  String id; // MongoDB ObjectId
  String userId; // Owner of this label
  String name; // Display name (original casing)
  String color; // Hex color code
  Integer usageCount; // Number of ChatNotes using this label
  Instant createdAt;
  Instant updatedAt;

}
