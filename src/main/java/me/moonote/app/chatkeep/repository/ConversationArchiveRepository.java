package me.moonote.app.chatkeep.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.ArchiveCompleteness;
import me.moonote.app.chatkeep.model.ConversationArchive;

@Repository
public interface ConversationArchiveRepository
    extends MongoRepository<ConversationArchive, String> {

  // Find by user
  List<ConversationArchive> findByUserId(String userId);

  // Find public archives
  List<ConversationArchive> findByIsPublicTrue();

  Page<ConversationArchive> findByIsPublic(Boolean isPublic, Pageable pageable);

  // Find by tags
  List<ConversationArchive> findByTagsContaining(String tag);

  // Search by title
  List<ConversationArchive> findByTitleContainingIgnoreCase(String keyword);

  // Find by date range
  List<ConversationArchive> findByConversationDateBetween(LocalDate start, LocalDate end);

  // Find by platform
  List<ConversationArchive> findByOriginalPlatform(String platform);

  // Custom queries
  @Query("{ 'user_id': ?0, 'is_public': true }")
  List<ConversationArchive> findPublicArchivesByUser(String userId);

  @Query("{ 'archive_completeness': ?0 }")
  List<ConversationArchive> findByCompleteness(ArchiveCompleteness completeness);

}
