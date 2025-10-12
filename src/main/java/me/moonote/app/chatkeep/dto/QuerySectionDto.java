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
public class QuerySectionDto {

  private String description;
  private List<String> attachmentsReferenced;
  private List<String> artifactsCreated;

}
