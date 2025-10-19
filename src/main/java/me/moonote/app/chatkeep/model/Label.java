package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "labels")
@CompoundIndex(name = "userId_normalizedName_unique", def = "{'userId': 1, 'normalizedName': 1}", unique = true)
@FieldDefaults(level = PRIVATE)
public class Label {

  @Id
  String id;

  // User ownership
  @Indexed
  String userId;

  // Display name as user entered (e.g., "Work Projects")
  String name;

  // Lowercase version for case-insensitive uniqueness (e.g., "work projects")
  String normalizedName;

  // Hex color code (e.g., "#FF5733")
  String color;

  // Audit fields
  @CreatedDate
  Instant createdAt;

  @LastModifiedDate
  Instant updatedAt;

}
