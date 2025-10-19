package me.moonote.app.chatkeep.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.Artifact;

/**
 * Repository for Artifact collection Artifacts are stored separately from ChatNote for performance
 */
@Repository
public interface ArtifactRepository extends MongoRepository<Artifact, String> {

  /**
   * Find all artifacts belonging to a specific ChatNote.
   *
   * @param chatNoteId ChatNote ID
   * @return List of artifacts ordered by creation date (newest first)
   */
  List<Artifact> findByChatNoteIdOrderByCreatedAtDesc(String chatNoteId);

  /**
   * Delete all artifacts belonging to a specific ChatNote. Used for cascading deletion.
   *
   * @param chatNoteId ChatNote ID
   */
  void deleteByChatNoteId(String chatNoteId);

  /**
   * Count the number of artifacts belonging to a specific ChatNote.
   *
   * @param chatNoteId ChatNote ID
   * @return Count of artifacts
   */
  long countByChatNoteId(String chatNoteId);

}
