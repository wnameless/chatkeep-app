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
public class ConversationSummaryDto {

  QuerySectionDto initialQuery;
  InsightsSectionDto keyInsights;
  FollowUpSectionDto followUpExplorations;
  List<ReferenceDto> references;

}
