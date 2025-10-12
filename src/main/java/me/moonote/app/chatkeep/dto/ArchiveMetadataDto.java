package me.moonote.app.chatkeep.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveMetadataDto {

  private String archiveVersion;
  private String archiveType;
  private LocalDate createdDate;
  private String originalPlatform;
  private Integer attachmentCount;
  private Integer artifactCount;
  private String archiveCompleteness;
  private Integer workaroundsCount;
  private String totalFileSize;
  private String title;
  private LocalDate conversationDate;
  private List<String> tags;

}
