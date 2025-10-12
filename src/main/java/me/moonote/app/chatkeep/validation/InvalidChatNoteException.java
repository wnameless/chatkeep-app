package me.moonote.app.chatkeep.validation;

import java.util.List;

public class InvalidChatNoteException extends RuntimeException {

  public InvalidChatNoteException(String message) {
    super(message);
  }

  public InvalidChatNoteException(List<String> errors) {
    super("Chat note validation failed: " + String.join(", ", errors));
  }

}
