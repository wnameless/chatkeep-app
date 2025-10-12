package me.moonote.app.chatkeep.dto;

import static lombok.AccessLevel.PRIVATE;
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
public class ArchiveDto {

  ArchiveMetadataDto metadata;
  ConversationSummaryDto summary;
  List<ArtifactDto> artifacts;
  List<AttachmentDto> attachments;
  List<WorkaroundDto> workarounds;

}
