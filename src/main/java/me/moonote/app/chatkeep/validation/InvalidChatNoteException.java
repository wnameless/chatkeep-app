package me.moonote.app.chatkeep.validation;

import java.util.List;

public class InvalidArchiveException extends RuntimeException {

  public InvalidArchiveException(String message) {
    super(message);
  }

  public InvalidArchiveException(List<String> errors) {
    super("Archive validation failed: " + String.join(", ", errors));
  }

}
