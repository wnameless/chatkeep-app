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
public class Attachment {

  private String filename;
  private String content;

  @Field("is_summarized")
  private Boolean isSummarized;

  @Field("original_size")
  private String originalSize;

  @Field("summarization_level")
  private String summarizationLevel;

  @Field("content_preserved")
  private String contentPreserved;

  @Field("processing_limitation")
  private String processingLimitation;

}
