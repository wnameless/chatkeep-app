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
  Instant createdAt;
  Instant updatedAt;

}
