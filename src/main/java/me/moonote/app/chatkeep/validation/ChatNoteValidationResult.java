package me.moonote.app.chatkeep.validation;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import me.moonote.app.chatkeep.dto.ChatNoteDto;

@Data
@Builder
public class ChatNoteValidationResult {
  private boolean valid;
  private ChatNoteDto chatNoteDto;
  private List<String> errors;

  public static ChatNoteValidationResult success(ChatNoteDto dto) {
    return ChatNoteValidationResult.builder().valid(true).chatNoteDto(dto)
        .errors(Collections.emptyList()).build();
  }

  public static ChatNoteValidationResult failure(List<String> errors) {
    return ChatNoteValidationResult.builder().valid(false).errors(errors).build();
  }
}
