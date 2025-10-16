package me.moonote.app.chatkeep.dto;

import static lombok.AccessLevel.PRIVATE;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ReferenceDto {

  String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  String url;

  @Builder.Default
  ReferenceType type = ReferenceType.EXTERNAL_LINK;

}
