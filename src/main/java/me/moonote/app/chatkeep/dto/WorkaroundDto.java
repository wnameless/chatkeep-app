package me.moonote.app.chatkeep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkaroundDto {

  private String filename;
  private String workaround;
  private String reason;
  private String preserved;
  private String lost;

}
