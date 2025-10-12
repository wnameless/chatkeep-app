package me.moonote.app.chatkeep.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.ChatNoteCompleteness;
import me.moonote.app.chatkeep.model.ChatNote;

@Repository
public interface ChatNoteRepository
    extends MongoRepository<ChatNote, String> {

  // Find by user
  List<ChatNote> findByUserId(String userId);

  // Find public archives
  List<ChatNote> findByIsPublicTrue();

  Page<ChatNote> findByIsPublic(Boolean isPublic, Pageable pageable);

  // Find by tags - Single tag
  List<ChatNote> findByTagsContaining(String tag);

  // Find by tags - Multiple tags (AND operation - contains ALL tags)
  @Query("{ 'tags': { $all: ?0 } }")
  List<ChatNote> findByTagsContainingAll(List<String> tags);

  @Query("{ 'tags': { $all: ?0 } }")
  Page<ChatNote> findByTagsContainingAll(List<String> tags, Pageable pageable);

  @Query("{ 'tags': { $all: ?0 }, 'userId': ?1 }")
  Page<ChatNote> findByTagsContainingAllAndUserId(List<String> tags, String userId,
      Pageable pageable);

  // Find by tags - Multiple tags (OR operation - contains ANY tag)
  @Query("{ 'tags': { $in: ?0 } }")
  List<ChatNote> findByTagsIn(List<String> tags);

  @Query("{ 'tags': { $in: ?0 } }")
  Page<ChatNote> findByTagsIn(List<String> tags, Pageable pageable);

  @Query("{ 'tags': { $in: ?0 }, 'userId': ?1 }")
  Page<ChatNote> findByTagsInAndUserId(List<String> tags, String userId, Pageable pageable);

  // Find by tags with lifecycle filters - Active notes only (AND operation)
  @Query("{ 'tags': { $all: ?0 }, 'isArchived': false, 'isTrashed': false }")
  Page<ChatNote> findActiveByTagsContainingAll(List<String> tags, Pageable pageable);

  @Query("{ 'tags': { $all: ?0 }, 'userId': ?1, 'isArchived': false, 'isTrashed': false }")
  Page<ChatNote> findActiveByTagsContainingAllAndUserId(List<String> tags, String userId,
      Pageable pageable);

  // Find by tags with lifecycle filters - Active notes only (OR operation)
  @Query("{ 'tags': { $in: ?0 }, 'isArchived': false, 'isTrashed': false }")
  Page<ChatNote> findActiveByTagsIn(List<String> tags, Pageable pageable);

  @Query("{ 'tags': { $in: ?0 }, 'userId': ?1, 'isArchived': false, 'isTrashed': false }")
  Page<ChatNote> findActiveByTagsInAndUserId(List<String> tags, String userId, Pageable pageable);

  // Search by title
  List<ChatNote> findByTitleContainingIgnoreCase(String keyword);

  // Find by date range
  List<ChatNote> findByConversationDateBetween(LocalDate start, LocalDate end);

  // Find by platform
  List<ChatNote> findByOriginalPlatform(String platform);

  // Lifecycle queries - Active notes (not archived, not trashed)
  List<ChatNote> findByIsArchivedFalseAndIsTrashedFalse();

  Page<ChatNote> findByUserIdAndIsArchivedFalseAndIsTrashedFalse(String userId, Pageable pageable);

  // Archived notes
  List<ChatNote> findByUserIdAndIsArchivedTrueAndIsTrashedFalse(String userId);

  Page<ChatNote> findByIsArchivedTrueAndIsTrashedFalse(Pageable pageable);

  // Trashed notes
  List<ChatNote> findByUserIdAndIsTrashedTrue(String userId);

  Page<ChatNote> findByIsTrashedTrue(Pageable pageable);

  // Auto-purge candidates (trashed > 30 days ago)
  List<ChatNote> findByIsTrashedTrueAndTrashedAtBefore(Instant cutoffDate);

  // Custom queries
  @Query("{ 'user_id': ?0, 'is_public': true }")
  List<ChatNote> findPublicArchivesByUser(String userId);

  @Query("{ 'archive_completeness': ?0 }")
  List<ChatNote> findByCompleteness(ChatNoteCompleteness completeness);

}
