package me.moonote.app.chatkeep.dto;

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
public class WorkaroundDto {

  String filename;
  String workaround;
  String reason;
  String preserved;
  String lost;

}
