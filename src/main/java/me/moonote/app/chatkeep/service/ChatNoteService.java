package me.moonote.app.chatkeep.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.ArchiveDto;
import me.moonote.app.chatkeep.dto.response.ArchiveDetailLightResponse;
import me.moonote.app.chatkeep.dto.response.ArchiveDetailResponse;
import me.moonote.app.chatkeep.dto.response.ArchiveResponse;
import me.moonote.app.chatkeep.dto.response.ArtifactMetadata;
import me.moonote.app.chatkeep.dto.response.AttachmentMetadata;
import me.moonote.app.chatkeep.mapper.ArchiveMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
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
   * Get archive by ID (lightweight - without artifact/attachment content)
   */
  public ArchiveDetailLightResponse getArchiveById(String id) {
    ConversationArchive archive =
        repository.findById(id).orElseThrow(() -> new ArchiveNotFoundException(id));

    // Increment view count
    archive.setViewCount(archive.getViewCount() + 1);
    repository.save(archive);

    return toDetailLightResponse(archive);
  }

  /**
   * Get artifact content by archive ID and artifact index
   */
  public Artifact getArtifactContent(String archiveId, int index) {
    ConversationArchive archive =
        repository.findById(archiveId).orElseThrow(() -> new ArchiveNotFoundException(archiveId));

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
    ConversationArchive archive =
        repository.findById(archiveId).orElseThrow(() -> new ArchiveNotFoundException(archiveId));

    if (archive.getAttachments() == null || index < 0
        || index >= archive.getAttachments().size()) {
      throw new IllegalArgumentException(
          "Invalid attachment index: " + index + " for archive: " + archiveId);
    }

    return archive.getAttachments().get(index);
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

  private ArchiveDetailLightResponse toDetailLightResponse(ConversationArchive archive) {
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

    return ArchiveDetailLightResponse.builder().id(archive.getId())
        .archiveVersion(archive.getArchiveVersion()).archiveType(archive.getArchiveType())
        .createdDate(archive.getCreatedDate()).originalPlatform(archive.getOriginalPlatform())
        .attachmentCount(archive.getAttachmentCount()).artifactCount(archive.getArtifactCount())
        .archiveCompleteness(archive.getArchiveCompleteness().name())
        .workaroundsCount(archive.getWorkaroundsCount()).totalFileSize(archive.getTotalFileSize())
        .title(archive.getTitle()).conversationDate(archive.getConversationDate())
        .tags(archive.getTags()).summary(archive.getSummary()).artifacts(artifactMetadata)
        .attachments(attachmentMetadata).workarounds(archive.getWorkarounds())
        .userId(archive.getUserId()).isPublic(archive.getIsPublic())
        .viewCount(archive.getViewCount()).createdAt(archive.getCreatedAt())
        .updatedAt(archive.getUpdatedAt()).build();
  }

}
