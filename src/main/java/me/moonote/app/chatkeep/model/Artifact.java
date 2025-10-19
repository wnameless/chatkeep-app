package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Artifact - Outputs CREATED during conversation Stored as separate collection for performance
 * (list views don't need to load full content)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Document
@CompoundIndex(def = "{'chatNoteId': 1, 'createdAt': -1}")
public class Artifact {

  @Id
  String id;

  @Indexed
  String chatNoteId; // Reference to ChatNote

  String type;
  String title;
  String language;
  String version;
  String iterations;
  String evolutionNotes;
  String content;

  @CreatedDate
  Instant createdAt;

}
