package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
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
public class UpdateFollowUpRequest {

  @NotNull(message = "Description cannot be null")
  @Size(max = 10000, message = "Description must be less than 10000 characters")
  String description;

}
