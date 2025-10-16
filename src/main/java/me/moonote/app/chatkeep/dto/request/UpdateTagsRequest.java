package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateTagsRequest {

  @NotNull(message = "Tags cannot be null")
  @Size(max = 20, message = "Maximum 20 tags allowed")
  List<String> tags;

}
