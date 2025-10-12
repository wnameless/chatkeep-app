package me.moonote.app.chatkeep.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveDto {

  private ArchiveMetadataDto metadata;
  private ConversationSummaryDto summary;
  private List<ArtifactDto> artifacts;
  private List<AttachmentDto> attachments;
  private List<WorkaroundDto> workarounds;

}
