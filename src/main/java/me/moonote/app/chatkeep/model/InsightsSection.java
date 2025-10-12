package me.moonote.app.chatkeep.model;

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
public class InsightsSection {

  String description;
  List<String> keyPoints;
  List<String> attachmentsReferenced;
  List<String> artifactsCreated;

}
