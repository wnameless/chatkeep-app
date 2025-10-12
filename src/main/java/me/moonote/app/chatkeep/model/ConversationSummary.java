package me.moonote.app.chatkeep.model;

import java.util.List;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummary {

  @Field("initial_query")
  private QuerySection initialQuery;

  @Field("key_insights")
  private InsightsSection keyInsights;

  @Field("follow_up_explorations")
  private FollowUpSection followUpExplorations;

  private List<Reference> references;

}
