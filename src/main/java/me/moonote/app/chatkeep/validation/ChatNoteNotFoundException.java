package me.moonote.app.chatkeep.validation;

public class ArchiveNotFoundException extends RuntimeException {

  public ArchiveNotFoundException(String id) {
    super("Archive not found with id: " + id);
  }

}
