package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
public class AssignLabelsRequest {

  // @NotEmpty(message = "At least one label ID is required")
  JsonNode labelIds;

  public List<String> getLabelIds() {
    if (labelIds.isTextual()) return List.of(labelIds.asText());

    var ids = new ArrayList<String>();
    if (labelIds.isArray()) {
      ArrayNode arrayNode = ((ArrayNode) labelIds);
      var iter = arrayNode.elements();
      while (iter.hasNext()) {
        ids.add(iter.next().asText());
      }
    }
    return ids;
  }

}
