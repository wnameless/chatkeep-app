package me.moonote.app.chatkeep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactDto {

  private String type;
  private String title;
  private String language;
  private String version;
  private String iterations;
  private String evolutionNotes;
  private String content;

}
