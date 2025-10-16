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
public class UpdateContentRequest {

  @NotNull(message = "Content cannot be null")
  @Size(max = 1000000, message = "Content must be less than 1MB (1000000 characters)")
  String content;

}
