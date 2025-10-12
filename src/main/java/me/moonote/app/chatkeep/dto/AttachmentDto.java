package me.moonote.app.chatkeep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {

  private String filename;
  private String content;
  private Boolean isSummarized;
  private String originalSize;
  private String summarizationLevel;
  private String contentPreserved;
  private String processingLimitation;

}
