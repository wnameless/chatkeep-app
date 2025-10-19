package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CreateLabelRequest {

  @NotBlank(message = "Label name is required")
  String name;

  @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
      message = "Color must be a valid hex code (e.g., #FF5733)")
  String color;

}
