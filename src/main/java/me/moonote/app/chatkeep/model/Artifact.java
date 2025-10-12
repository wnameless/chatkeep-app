package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
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
public class Artifact {

  String type;
  String title;
  String language;
  String version;
  String iterations;
  String evolutionNotes;
  String content;

}
