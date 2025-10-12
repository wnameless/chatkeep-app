package me.moonote.app.chatkeep.model;

import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {

  private String type;
  private String title;
  private String language;
  private String version;
  private String iterations;

  @Field("evolution_notes")
  private String evolutionNotes;

  private String content;

}
