package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import me.moonote.app.chatkeep.dto.ReferenceDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UpdateReferencesRequest {

  @NotNull(message = "References list cannot be null")
  @Valid
  List<ReferenceDto> references;

}
