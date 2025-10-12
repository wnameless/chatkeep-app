package me.moonote.app.chatkeep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workaround {

  private String filename;
  private String workaround;
  private String reason;
  private String preserved;
  private String lost;

}
