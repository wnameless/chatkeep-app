package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
public class UpdateConversationDateRequest {

  @NotNull(message = "Conversation date cannot be null")
  @PastOrPresent(message = "Conversation date cannot be in the future")
  LocalDate conversationDate;

}
