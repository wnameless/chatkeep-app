package me.moonote.app.chatkeep.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversation_archives")
public class ConversationArchive {

  @Id
  private String id;

  // Metadata fields
  @Field("archive_version")
  private String archiveVersion;

  @Field("archive_type")
  private String archiveType;

  @Field("created_date")
  private LocalDate createdDate;

  @Field("original_platform")
  private String originalPlatform;

  @Field("attachment_count")
  private Integer attachmentCount;

  @Field("artifact_count")
  private Integer artifactCount;

  @Field("archive_completeness")
  private ArchiveCompleteness archiveCompleteness; // ENUM

  @Field("workarounds_count")
  private Integer workaroundsCount;

  @Field("total_file_size")
  private String totalFileSize;

  // Summary fields
  private String title;

  @Field("conversation_date")
  private LocalDate conversationDate;

  private List<String> tags;

  // Embedded documents
  private ConversationSummary summary;
  private List<Artifact> artifacts;
  private List<Attachment> attachments;
  private List<Workaround> workarounds;

  // Metadata for web app
  @Field("created_at")
  @CreatedDate
  private LocalDateTime createdAt;

  @Field("updated_at")
  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Field("user_id")
  private String userId; // For multi-user support

  @Field("is_public")
  private Boolean isPublic; // For sharing feature

  @Field("view_count")
  private Long viewCount;

}
