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
  Integer attachmentCount;
  Integer artifactCount;
  String chatNoteCompleteness;
  Integer workaroundsCount;
  String totalFileSize;
  String title;
  LocalDate conversationDate;
  List<String> tags;
  ConversationSummary summary;
  List<ArtifactMetadata> artifacts; // Metadata only, no content
  List<AttachmentMetadata> attachments; // Metadata only, no content
  List<Workaround> workarounds;
  String userId;
  Boolean isPublic;
  Long viewCount;
  Instant createdAt;
  Instant updatedAt;

}
