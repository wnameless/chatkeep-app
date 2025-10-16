package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateTitleRequest {

  @NotBlank(message = "Title cannot be empty")
  @Size(max = 500, message = "Title must be less than 500 characters")
  String title;

}
