package me.moonote.app.chatkeep.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.ArchiveDto;
import me.moonote.app.chatkeep.dto.response.ArchiveDetailResponse;
import me.moonote.app.chatkeep.dto.response.ArchiveResponse;
import me.moonote.app.chatkeep.mapper.ArchiveMapper;
import me.moonote.app.chatkeep.model.ConversationArchive;
import me.moonote.app.chatkeep.repository.ConversationArchiveRepository;
import me.moonote.app.chatkeep.validation.ArchiveNotFoundException;
import me.moonote.app.chatkeep.validation.ArchiveValidationResult;
import me.moonote.app.chatkeep.validation.InvalidArchiveException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

  private final MarkdownArchivePreprocessor preprocessor;
  private final ConversationArchiveRepository repository;
  private final ArchiveMapper mapper;

  /**
   * Upload and process a markdown archive
   */
  public ArchiveDetailResponse uploadArchive(String markdownContent, String userId) {
    log.info("Processing archive upload for user: {}", userId);

    // Parse and validate markdown
    ArchiveValidationResult validationResult = preprocessor.preprocess(markdownContent);

    if (!validationResult.isValid()) {
      log.warn("Archive validation failed: {}", validationResult.getErrors());
      throw new InvalidArchiveException("Archive validation failed: "
          + String.join(", ", validationResult.getErrors()));
    }

    // Convert to entity and save
    ArchiveDto archiveDto = validationResult.getArchiveDto();
    ConversationArchive entity = mapper.toEntity(archiveDto, userId);
    ConversationArchive saved = repository.save(entity);

    log.info("Archive saved successfully with id: {}", saved.getId());

    return toDetailResponse(saved);
  }

  /**
   * Get archive by ID
   */
  public ArchiveDetailResponse getArchiveById(String id) {
    ConversationArchive archive =
        repository.findById(id).orElseThrow(() -> new ArchiveNotFoundException(id));

    // Increment view count
    archive.setViewCount(archive.getViewCount() + 1);
    repository.save(archive);

    return toDetailResponse(archive);
  }

  /**
   * Get all archives (paginated)
   */
  public Page<ArchiveResponse> getAllArchives(Pageable pageable) {
    return repository.findAll(pageable).map(this::toResponse);
  }

  /**
   * Get archives by user ID
   */
  public List<ArchiveResponse> getArchivesByUserId(String userId) {
    return repository.findByUserId(userId).stream().map(this::toResponse).toList();
  }

  /**
   * Search archives by title
   */
  public List<ArchiveResponse> searchByTitle(String title) {
    return repository.findByTitleContainingIgnoreCase(title).stream().map(this::toResponse)
        .toList();
  }

  /**
   * Search archives by tags
   */
  public List<ArchiveResponse> searchByTag(String tag) {
    return repository.findByTagsContaining(tag).stream().map(this::toResponse).toList();
  }

  /**
   * Update archive visibility
   */
  public ArchiveDetailResponse updateVisibility(String id, Boolean isPublic) {
    ConversationArchive archive =
        repository.findById(id).orElseThrow(() -> new ArchiveNotFoundException(id));

    archive.setIsPublic(isPublic);
    ConversationArchive updated = repository.save(archive);

    log.info("Archive {} visibility updated to: {}", id, isPublic);

    return toDetailResponse(updated);
  }

  /**
   * Delete archive
   */
  public void deleteArchive(String id) {
    if (!repository.existsById(id)) {
      throw new ArchiveNotFoundException(id);
    }

    repository.deleteById(id);
    log.info("Archive deleted: {}", id);
  }

  /**
   * Get public archives
   */
  public Page<ArchiveResponse> getPublicArchives(Pageable pageable) {
    return repository.findByIsPublic(true, pageable).map(this::toResponse);
  }

  private ArchiveResponse toResponse(ConversationArchive archive) {
    return ArchiveResponse.builder().id(archive.getId()).title(archive.getTitle())
        .conversationDate(archive.getConversationDate()).tags(archive.getTags())
        .originalPlatform(archive.getOriginalPlatform())
        .archiveCompleteness(archive.getArchiveCompleteness().name())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .viewCount(archive.getViewCount()).isPublic(archive.getIsPublic())
        .createdAt(archive.getCreatedAt()).updatedAt(archive.getUpdatedAt()).build();
  }

  private ArchiveDetailResponse toDetailResponse(ConversationArchive archive) {
    return ArchiveDetailResponse.builder().id(archive.getId())
        .archiveVersion(archive.getArchiveVersion()).archiveType(archive.getArchiveType())
        .createdDate(archive.getCreatedDate()).originalPlatform(archive.getOriginalPlatform())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .archiveCompleteness(archive.getArchiveCompleteness().name())
        .workaroundsCount(archive.getWorkaroundsCount()).totalFileSize(archive.getTotalFileSize())
        .title(archive.getTitle()).conversationDate(archive.getConversationDate())
        .tags(archive.getTags()).summary(archive.getSummary()).artifacts(archive.getArtifacts())
        .attachments(archive.getAttachments()).workarounds(archive.getWorkarounds())
        .userId(archive.getUserId()).isPublic(archive.getIsPublic())
        .viewCount(archive.getViewCount()).createdAt(archive.getCreatedAt())
        .updatedAt(archive.getUpdatedAt()).build();
  }

}
