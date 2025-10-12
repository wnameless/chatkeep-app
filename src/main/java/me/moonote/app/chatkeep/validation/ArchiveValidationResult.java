package me.moonote.app.chatkeep.validation;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import me.moonote.app.chatkeep.dto.ArchiveDto;

@Data
@Builder
public class ArchiveValidationResult {
  private boolean valid;
  private ArchiveDto archiveDto;
  private List<String> errors;

  public static ArchiveValidationResult success(ArchiveDto dto) {
    return ArchiveValidationResult.builder().valid(true).archiveDto(dto)
        .errors(Collections.emptyList()).build();
  }

  public static ArchiveValidationResult failure(List<String> errors) {
    return ArchiveValidationResult.builder().valid(false).errors(errors).build();
  }
}
