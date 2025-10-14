package me.moonote.app.chatkeep.dto.response;

import static lombok.AccessLevel.PRIVATE;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import me.moonote.app.chatkeep.model.ConversationSummary;
import me.moonote.app.chatkeep.model.Workaround;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ChatNoteDetailLightResponse {

  String id;
  String archiveVersion;
  String archiveType;
  LocalDate createdDate;
  String originalPlatform;
  String platform; // Simplified platform name for display
  Integer attachmentCount;
  Integer artifactCount;
  String chatNoteCompleteness;
  Integer workaroundsCount;
  String totalFileSize;
  String title;
  LocalDate conversationDate;
  List<String> tags;
  Integer messageCount; // Total number of messages in conversation
  Integer wordCount; // Total word count
  ConversationSummary summary;
  List<ArtifactMetadata> artifacts; // Metadata only, no content
  List<AttachmentMetadata> attachments; // Metadata only, no content
  List<Workaround> workarounds;
  String conversationContent; // Full conversation content (without artifacts/attachments)
  String fullMarkdown; // Complete markdown including YAML frontmatter
  String userId;
  Boolean isPublic;
  Boolean isArchived;
  Boolean isTrashed;
  Boolean isFavorite;
  Instant trashedAt;
  Long viewCount;
  Instant createdAt;
  Instant updatedAt;

}
