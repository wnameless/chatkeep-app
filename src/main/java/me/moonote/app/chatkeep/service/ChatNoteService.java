package me.moonote.app.chatkeep.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.response.ArtifactMetadata;
import me.moonote.app.chatkeep.dto.response.AttachmentMetadata;
import me.moonote.app.chatkeep.dto.response.ChatNoteDetailLightResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteDetailResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteResponse;
import me.moonote.app.chatkeep.mapper.ChatNoteMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.repository.ChatNoteRepository;
import me.moonote.app.chatkeep.validation.ChatNoteNotFoundException;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.InvalidChatNoteException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNoteService {

  private final MarkdownChatNotePreprocessor preprocessor;
  private final ChatNoteRepository repository;
  private final ChatNoteMapper mapper;
  private final ChatNoteMarkdownGenerator markdownGenerator;

  /**
   * Upload and process a markdown archive
   */
  public ChatNoteDetailResponse uploadChatNote(String markdownContent, String userId) {
    log.info("Processing archive upload for user: {}", userId);

    // Parse and validate markdown
    ChatNoteValidationResult validationResult = preprocessor.preprocess(markdownContent);

    if (!validationResult.isValid()) {
      log.warn("Chat note validation failed: {}", validationResult.getErrors());
      throw new InvalidChatNoteException(
          "Chat note validation failed: " + String.join(", ", validationResult.getErrors()));
    }

    // Convert to entity and save
    ChatNoteDto chatNoteDto = validationResult.getChatNoteDto();
    ChatNote entity = mapper.toEntity(chatNoteDto, userId, markdownContent);
    ChatNote saved = repository.save(entity);

    log.info("Chat note saved successfully with id: {}", saved.getId());

    return toDetailResponse(saved);
  }

  /**
   * Copy a public chat note to the user's workspace Used when anonymous/logged-in users want to
   * save a shared note to their own workspace
   */
  public ChatNoteDetailResponse copyChatNoteToWorkspace(String sourceNoteId, String userId) {
    log.info("Copying chat note {} to workspace for user: {}", sourceNoteId, userId);

    // Get the source note
    ChatNote sourceNote = repository.findById(sourceNoteId)
        .orElseThrow(() -> new ChatNoteNotFoundException(sourceNoteId));

    // Verify the source note is public
    if (!Boolean.TRUE.equals(sourceNote.getIsPublic())) {
      throw new IllegalArgumentException("Cannot copy a non-public chat note");
    }

    // Create a new note entity by copying all fields from source
    // Use builder to create a clean copy without the ID (so MongoDB generates a new one)
    ChatNote copiedNote = ChatNote.builder().archiveVersion(sourceNote.getArchiveVersion())
        .archiveType(sourceNote.getArchiveType()).createdDate(sourceNote.getCreatedDate())
        .originalPlatform(sourceNote.getOriginalPlatform())
        .attachmentCount(sourceNote.getAttachmentCount())
        .artifactCount(sourceNote.getArtifactCount())
        .chatNoteCompleteness(sourceNote.getChatNoteCompleteness())
        .workaroundsCount(sourceNote.getWorkaroundsCount())
        .totalFileSize(sourceNote.getTotalFileSize()).title(sourceNote.getTitle())
        .conversationDate(sourceNote.getConversationDate())
        .tags(sourceNote.getTags() != null ? List.copyOf(sourceNote.getTags()) : null)
        .summary(sourceNote.getSummary())
        .artifacts(
            sourceNote.getArtifacts() != null ? List.copyOf(sourceNote.getArtifacts()) : null)
        .attachments(
            sourceNote.getAttachments() != null ? List.copyOf(sourceNote.getAttachments()) : null)
        .workarounds(
            sourceNote.getWorkarounds() != null ? List.copyOf(sourceNote.getWorkarounds()) : null)
        .markdownContent(sourceNote.getMarkdownContent()).userId(userId) // Assign to the requesting
                                                                         // user
        .isPublic(false) // Set as private by default
        .isArchived(false).isTrashed(false).isFavorite(false).trashedAt(null).viewCount(0L).build();

    // Save the copied note
    ChatNote saved = repository.save(copiedNote);

    log.info("Chat note copied successfully with new id: {}", saved.getId());

    return toDetailResponse(saved);
  }

  /**
   * Get archive by ID (lightweight - without artifact/attachment content)
   */
  public ChatNoteDetailLightResponse getChatNoteById(String id) {
    ChatNote archive = repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    // Increment view count
    archive.setViewCount(archive.getViewCount() + 1);
    repository.save(archive);

    return toDetailLightResponse(archive);
  }

  /**
   * Get artifact content by archive ID and artifact index
   */
  public Artifact getArtifactContent(String archiveId, int index) {
    ChatNote archive =
        repository.findById(archiveId).orElseThrow(() -> new ChatNoteNotFoundException(archiveId));

    if (archive.getArtifacts() == null || index < 0 || index >= archive.getArtifacts().size()) {
      throw new IllegalArgumentException(
          "Invalid artifact index: " + index + " for archive: " + archiveId);
    }

    return archive.getArtifacts().get(index);
  }

  /**
   * Get attachment content by archive ID and attachment index
   */
  public Attachment getAttachmentContent(String archiveId, int index) {
    ChatNote archive =
        repository.findById(archiveId).orElseThrow(() -> new ChatNoteNotFoundException(archiveId));

    if (archive.getAttachments() == null || index < 0 || index >= archive.getAttachments().size()) {
      throw new IllegalArgumentException(
          "Invalid attachment index: " + index + " for archive: " + archiveId);
    }

    return archive.getAttachments().get(index);
  }

  /**
   * Get all archives (paginated)
   */
  public Page<ChatNoteResponse> getAllChatNotes(Pageable pageable) {
    return repository.findAll(pageable).map(this::toResponse);
  }

  /**
   * Get archives by user ID
   */
  public List<ChatNoteResponse> getChatNotesByUserId(String userId) {
    return repository.findByUserId(userId).stream().map(this::toResponse).toList();
  }

  /**
   * Search archives by title
   */
  public List<ChatNoteResponse> searchByTitle(String title) {
    return repository.findByTitleContainingIgnoreCase(title).stream().map(this::toResponse)
        .toList();
  }

  /**
   * Search archives by tags
   */
  public List<ChatNoteResponse> searchByTag(String tag) {
    return repository.findByTagsContaining(tag).stream().map(this::toResponse).toList();
  }

  /**
   * Comprehensive search across title, tags, and content Searches only user's active notes (not
   * archived, not trashed)
   */
  public List<ChatNoteResponse> searchUserChatNotes(String userId, String query) {
    if (query == null || query.trim().isEmpty()) {
      return List.of();
    }

    String searchQuery = query.trim();
    log.info("Searching user {} chat notes with query: {}", userId, searchQuery);

    return repository.searchActiveByUserId(userId, searchQuery).stream().map(this::toResponse)
        .toList();
  }

  /**
   * Update archive visibility
   */
  public ChatNoteDetailResponse updateVisibility(String id, Boolean isPublic) {
    ChatNote archive = repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    archive.setIsPublic(isPublic);
    ChatNote updated = repository.save(archive);

    log.info("Chat note {} visibility updated to: {}", id, isPublic);

    return toDetailResponse(updated);
  }

  /**
   * Delete archive
   */
  public void deleteChatNote(String id) {
    if (!repository.existsById(id)) {
      throw new ChatNoteNotFoundException(id);
    }

    repository.deleteById(id);
    log.info("Chat note deleted: {}", id);
  }

  /**
   * Get public archives
   */
  public Page<ChatNoteResponse> getPublicChatNotes(Pageable pageable) {
    return repository.findByIsPublic(true, pageable).map(this::toResponse);
  }

  // ==================== Tag-Based Filtering ====================

  /**
   * Filter chat notes by multiple tags with AND/OR operation
   */
  public Page<ChatNoteResponse> filterByTags(List<String> tags, String operator,
      Pageable pageable) {
    if (tags == null || tags.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<ChatNote> chatNotes;
    if ("OR".equalsIgnoreCase(operator)) {
      chatNotes = repository.findByTagsIn(tags, pageable);
    } else {
      // Default to AND operation
      chatNotes = repository.findByTagsContainingAll(tags, pageable);
    }

    return chatNotes.map(this::toResponse);
  }

  /**
   * Filter chat notes by tags for a specific user
   */
  public Page<ChatNoteResponse> filterByTagsForUser(String userId, List<String> tags,
      String operator, Pageable pageable) {
    if (tags == null || tags.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<ChatNote> chatNotes;
    if ("OR".equalsIgnoreCase(operator)) {
      chatNotes = repository.findByTagsInAndUserId(tags, userId, pageable);
    } else {
      // Default to AND operation
      chatNotes = repository.findByTagsContainingAllAndUserId(tags, userId, pageable);
    }

    return chatNotes.map(this::toResponse);
  }

  /**
   * Filter active (not archived, not trashed) chat notes by tags
   */
  public Page<ChatNoteResponse> filterActiveByTags(List<String> tags, String operator,
      Pageable pageable) {
    if (tags == null || tags.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<ChatNote> chatNotes;
    if ("OR".equalsIgnoreCase(operator)) {
      chatNotes = repository.findActiveByTagsIn(tags, pageable);
    } else {
      // Default to AND operation
      chatNotes = repository.findActiveByTagsContainingAll(tags, pageable);
    }

    return chatNotes.map(this::toResponse);
  }

  /**
   * Filter active chat notes by tags for a specific user
   */
  public Page<ChatNoteResponse> filterActiveByTagsForUser(String userId, List<String> tags,
      String operator, Pageable pageable) {
    if (tags == null || tags.isEmpty()) {
      return Page.empty(pageable);
    }

    Page<ChatNote> chatNotes;
    if ("OR".equalsIgnoreCase(operator)) {
      chatNotes = repository.findActiveByTagsInAndUserId(tags, userId, pageable);
    } else {
      // Default to AND operation
      chatNotes = repository.findActiveByTagsContainingAllAndUserId(tags, userId, pageable);
    }

    return chatNotes.map(this::toResponse);
  }

  // ==================== Favorites Management ====================

  /**
   * Toggle favorite status (star/unstar)
   */
  public ChatNoteDetailResponse toggleFavorite(String id, Boolean isFavorite) {
    ChatNote chatNote =
        repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    chatNote.setIsFavorite(isFavorite);
    ChatNote updated = repository.save(chatNote);

    log.info("Chat note {} favorite status updated to: {}", id, isFavorite);

    return toDetailResponse(updated);
  }

  /**
   * Get all favorite chat notes for a user (paginated)
   */
  public Page<ChatNoteResponse> getFavoriteChatNotes(String userId, Pageable pageable) {
    return repository.findByUserIdAndIsFavoriteTrue(userId, pageable).map(this::toResponse);
  }

  /**
   * Get favorite active chat notes for a user (not archived, not trashed)
   */
  public Page<ChatNoteResponse> getFavoriteActiveChatNotes(String userId, Pageable pageable) {
    return repository.findFavoriteActiveByUserId(userId, pageable).map(this::toResponse);
  }

  // ==================== Lifecycle Management ====================

  /**
   * Update archive status (archive/unarchive)
   */
  public ChatNoteDetailResponse updateArchiveStatus(String id, Boolean isArchived) {
    ChatNote chatNote =
        repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    chatNote.setIsArchived(isArchived);
    ChatNote updated = repository.save(chatNote);

    log.info("Chat note {} archive status updated to: {}", id, isArchived);

    return toDetailResponse(updated);
  }

  /**
   * Move chat note to trash (soft delete)
   */
  public ChatNoteDetailResponse moveToTrash(String id) {
    ChatNote chatNote =
        repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    chatNote.setIsTrashed(true);
    chatNote.setTrashedAt(java.time.Instant.now());
    ChatNote updated = repository.save(chatNote);

    log.info("Chat note {} moved to trash", id);

    return toDetailResponse(updated);
  }

  /**
   * Restore chat note from trash
   */
  public ChatNoteDetailResponse restoreFromTrash(String id) {
    ChatNote chatNote =
        repository.findById(id).orElseThrow(() -> new ChatNoteNotFoundException(id));

    chatNote.setIsTrashed(false);
    chatNote.setTrashedAt(null);
    ChatNote updated = repository.save(chatNote);

    log.info("Chat note {} restored from trash", id);

    return toDetailResponse(updated);
  }

  /**
   * Permanently delete chat note from database (hard delete)
   */
  public void permanentlyDeleteChatNote(String id) {
    if (!repository.existsById(id)) {
      throw new ChatNoteNotFoundException(id);
    }

    repository.deleteById(id);
    log.info("Chat note {} permanently deleted", id);
  }

  /**
   * Get active chat notes for a user (not archived, not trashed)
   */
  public Page<ChatNoteResponse> getActiveChatNotes(String userId, Pageable pageable) {
    return repository.findByUserIdAndIsArchivedFalseAndIsTrashedFalse(userId, pageable)
        .map(this::toResponse);
  }

  /**
   * Get archived chat notes for a user
   */
  public List<ChatNoteResponse> getArchivedChatNotes(String userId) {
    return repository.findByUserIdAndIsArchivedTrueAndIsTrashedFalse(userId).stream()
        .map(this::toResponse).toList();
  }

  /**
   * Get trashed chat notes for a user
   */
  public List<ChatNoteResponse> getTrashedChatNotes(String userId) {
    return repository.findByUserIdAndIsTrashedTrue(userId).stream().map(this::toResponse).toList();
  }

  /**
   * Get all archived chat notes (paginated) - for admin or global view
   */
  public Page<ChatNoteResponse> getAllArchivedChatNotes(Pageable pageable) {
    return repository.findByIsArchivedTrueAndIsTrashedFalse(pageable).map(this::toResponse);
  }

  /**
   * Get all trashed chat notes (paginated) - for admin or global view
   */
  public Page<ChatNoteResponse> getAllTrashedChatNotes(Pageable pageable) {
    return repository.findByIsTrashedTrue(pageable).map(this::toResponse);
  }

  /**
   * Purge old trashed chat notes (trashed > 30 days ago) - for scheduled job
   */
  public int purgeOldTrashedNotes() {
    java.time.Instant cutoffDate =
        java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
    List<ChatNote> oldTrashedNotes = repository.findByIsTrashedTrueAndTrashedAtBefore(cutoffDate);

    int count = oldTrashedNotes.size();
    repository.deleteAll(oldTrashedNotes);

    log.info("Purged {} chat notes trashed before {}", count, cutoffDate);
    return count;
  }

  // ==================== Private Helper Methods ====================

  private ChatNoteResponse toResponse(ChatNote archive) {
    return ChatNoteResponse.builder().id(archive.getId()).title(archive.getTitle())
        .conversationDate(archive.getConversationDate()).tags(archive.getTags())
        .originalPlatform(archive.getOriginalPlatform())
        .chatNoteCompleteness(archive.getChatNoteCompleteness().name())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .viewCount(archive.getViewCount()).isPublic(archive.getIsPublic())
        .isArchived(archive.getIsArchived()).isTrashed(archive.getIsTrashed())
        .isFavorite(archive.getIsFavorite()).createdAt(archive.getCreatedAt())
        .updatedAt(archive.getUpdatedAt()).build();
  }

  private ChatNoteDetailResponse toDetailResponse(ChatNote archive) {
    return ChatNoteDetailResponse.builder().id(archive.getId())
        .archiveVersion(archive.getArchiveVersion()).archiveType(archive.getArchiveType())
        .createdDate(archive.getCreatedDate()).originalPlatform(archive.getOriginalPlatform())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .chatNoteCompleteness(archive.getChatNoteCompleteness().name())
        .workaroundsCount(archive.getWorkaroundsCount()).totalFileSize(archive.getTotalFileSize())
        .title(archive.getTitle()).conversationDate(archive.getConversationDate())
        .tags(archive.getTags()).summary(archive.getSummary()).artifacts(archive.getArtifacts())
        .attachments(archive.getAttachments()).workarounds(archive.getWorkarounds())
        .userId(archive.getUserId()).isPublic(archive.getIsPublic())
        .isArchived(archive.getIsArchived()).isTrashed(archive.getIsTrashed())
        .isFavorite(archive.getIsFavorite()).trashedAt(archive.getTrashedAt())
        .viewCount(archive.getViewCount()).createdAt(archive.getCreatedAt())
        .updatedAt(archive.getUpdatedAt()).build();
  }

  private ChatNoteDetailLightResponse toDetailLightResponse(ChatNote archive) {
    // Convert artifacts to metadata only (no content)
    List<ArtifactMetadata> artifactMetadata = archive.getArtifacts() == null ? List.of()
        : archive.getArtifacts().stream()
            .map(a -> ArtifactMetadata.builder().type(a.getType()).title(a.getTitle())
                .language(a.getLanguage()).version(a.getVersion()).iterations(a.getIterations())
                .evolutionNotes(a.getEvolutionNotes()).build())
            .toList();

    // Convert attachments to metadata only (no content)
    List<AttachmentMetadata> attachmentMetadata = archive.getAttachments() == null ? List.of()
        : archive.getAttachments().stream()
            .map(att -> AttachmentMetadata.builder().filename(att.getFilename())
                .isSummarized(att.getIsSummarized()).originalSize(att.getOriginalSize())
                .summarizationLevel(att.getSummarizationLevel())
                .contentPreserved(att.getContentPreserved())
                .processingLimitation(att.getProcessingLimitation()).build())
            .toList();

    return ChatNoteDetailLightResponse.builder().id(archive.getId())
        .archiveVersion(archive.getArchiveVersion()).archiveType(archive.getArchiveType())
        .createdDate(archive.getCreatedDate()).originalPlatform(archive.getOriginalPlatform())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .chatNoteCompleteness(archive.getChatNoteCompleteness().name())
        .workaroundsCount(archive.getWorkaroundsCount()).totalFileSize(archive.getTotalFileSize())
        .title(archive.getTitle()).conversationDate(archive.getConversationDate())
        .tags(archive.getTags()).summary(archive.getSummary()).artifacts(artifactMetadata)
        .attachments(attachmentMetadata).workarounds(archive.getWorkarounds())
        .conversationContent(markdownGenerator.generateConversationContent(archive))
        .fullMarkdown(markdownGenerator.generateMarkdown(archive)).userId(archive.getUserId())
        .isPublic(archive.getIsPublic()).isArchived(archive.getIsArchived())
        .isTrashed(archive.getIsTrashed()).isFavorite(archive.getIsFavorite())
        .trashedAt(archive.getTrashedAt()).viewCount(archive.getViewCount())
        .createdAt(archive.getCreatedAt()).updatedAt(archive.getUpdatedAt()).build();
  }

}
