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
public class QuerySection {
  private String description;

  @Field("attachments_referenced")
  private List<String> attachmentsReferenced;

  @Field("artifacts_created")
  private List<String> artifactsCreated;

}
