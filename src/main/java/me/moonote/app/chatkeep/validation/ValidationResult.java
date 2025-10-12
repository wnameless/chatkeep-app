package me.moonote.app.chatkeep.validation;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResult {

  private boolean valid;
  private List<String> errors;

}
