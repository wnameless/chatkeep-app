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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ChatNoteResponse {

  String id;
  String title;
  LocalDate conversationDate;
  List<String> tags;
  String originalPlatform;
  String chatNoteCompleteness;
  Integer attachmentCount;
  Integer artifactCount;
  Long viewCount;
  Boolean isPublic;
  Boolean isArchived;
  Boolean isTrashed;
  Boolean isFavorite;
  List<String> labelIds; // Label IDs assigned to this note
  List<LabelResponse> labels; // Full label objects for display (optional, populated by fragments)
  String contentPreview; // First ~200 characters of content for card display
  Instant createdAt;
  Instant updatedAt;

}
