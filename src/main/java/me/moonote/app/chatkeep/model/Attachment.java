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
public class Attachment {

  String filename;
  String content;
  Boolean isSummarized;
  String originalSize;
  String summarizationLevel;
  String contentPreserved;
  String processingLimitation;

}
