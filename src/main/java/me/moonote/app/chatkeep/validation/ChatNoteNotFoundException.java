package me.moonote.app.chatkeep.validation;

public class ChatNoteNotFoundException extends RuntimeException {

  public ChatNoteNotFoundException(String id) {
    super("Chat note not found with id: " + id);
  }

}
