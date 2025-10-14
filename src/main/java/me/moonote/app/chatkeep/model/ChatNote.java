package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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
@Document
@FieldDefaults(level = PRIVATE)
public class ChatNote {

  @Id
  String id;

  // Metadata fields
  String archiveVersion;
  String archiveType;
  LocalDate createdDate;
  String originalPlatform;
  Integer attachmentCount;
  Integer artifactCount;
  ChatNoteCompleteness chatNoteCompleteness; // ENUM
  Integer workaroundsCount;
  String totalFileSize;
  String title;
  LocalDate conversationDate;
  List<String> tags;

  // Embedded documents
  ConversationSummary summary;
  List<Artifact> artifacts;
  List<Attachment> attachments;
  List<Workaround> workarounds;

  // Original markdown content (for display and download)
  String markdownContent;

  // Metadata for web app
  @CreatedDate
  Instant createdAt;
  @LastModifiedDate
  Instant updatedAt;
  String userId; // For multi-user support
  Boolean isPublic; // For sharing feature
  Boolean isArchived; // Archived (hidden from main view)
  Boolean isTrashed; // Soft deleted (in trash)
  Boolean isFavorite; // Starred/favorited for quick access
  Instant trashedAt; // When moved to trash (for auto-purge)
  Long viewCount;

}
