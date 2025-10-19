package me.moonote.app.chatkeep.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.Attachment;

/**
 * Repository for Attachment collection Attachments are stored separately from ChatNote for
 * performance
 */
@Repository
public interface AttachmentRepository extends MongoRepository<Attachment, String> {

  /**
   * Find all attachments belonging to a specific ChatNote.
   *
   * @param chatNoteId ChatNote ID
   * @return List of attachments ordered by creation date (newest first)
   */
  List<Attachment> findByChatNoteIdOrderByCreatedAtDesc(String chatNoteId);

  /**
   * Delete all attachments belonging to a specific ChatNote. Used for cascading deletion.
   *
   * @param chatNoteId ChatNote ID
   */
  void deleteByChatNoteId(String chatNoteId);

  /**
   * Count the number of attachments belonging to a specific ChatNote.
   *
   * @param chatNoteId ChatNote ID
   * @return Count of attachments
   */
  long countByChatNoteId(String chatNoteId);

}
