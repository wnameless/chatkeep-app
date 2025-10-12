package me.moonote.app.chatkeep.dto.response;

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
public class AttachmentMetadata {

  String filename;
  Boolean isSummarized;
  String originalSize;
  String summarizationLevel;
  String contentPreserved;
  String processingLimitation;
  // Note: content field is intentionally omitted for bandwidth optimization

}
