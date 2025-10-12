package me.moonote.app.chatkeep.dto;

import static lombok.AccessLevel.PRIVATE;
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
public class ArchiveMetadataDto {

  String archiveVersion;
  String archiveType;
  LocalDate createdDate;
  String originalPlatform;
  Integer attachmentCount;
  Integer artifactCount;
  String archiveCompleteness;
  Integer workaroundsCount;
  String totalFileSize;
  String title;
  LocalDate conversationDate;
  List<String> tags;

}
