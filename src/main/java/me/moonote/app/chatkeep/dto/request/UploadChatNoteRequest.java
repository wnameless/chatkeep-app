package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
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
public class UploadChatNoteRequest {

  String markdownContent;

  // For copying an existing note to the user's workspace
  String sourceNoteId;
  Boolean copyFromPublic;

}
