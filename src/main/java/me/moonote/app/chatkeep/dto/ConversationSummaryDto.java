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
public class ConversationSummaryDto {

  private QuerySectionDto initialQuery;
  private InsightsSectionDto keyInsights;
  private FollowUpSectionDto followUpExplorations;
  private List<ReferenceDto> references;

}
