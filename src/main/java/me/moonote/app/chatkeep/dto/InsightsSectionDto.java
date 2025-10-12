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
public class InsightsSectionDto {

  private String description;
  private List<String> keyPoints;
  private List<String> attachmentsReferenced;
  private List<String> artifactsCreated;

}
