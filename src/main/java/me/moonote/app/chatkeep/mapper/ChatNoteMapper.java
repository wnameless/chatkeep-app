package me.moonote.app.chatkeep.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.ArtifactDto;
import me.moonote.app.chatkeep.dto.AttachmentDto;
import me.moonote.app.chatkeep.dto.ConversationSummaryDto;
import me.moonote.app.chatkeep.dto.FollowUpSectionDto;
import me.moonote.app.chatkeep.dto.InsightsSectionDto;
import me.moonote.app.chatkeep.dto.QuerySectionDto;
import me.moonote.app.chatkeep.dto.ReferenceDto;
import me.moonote.app.chatkeep.dto.WorkaroundDto;
import me.moonote.app.chatkeep.model.ChatNoteCompleteness;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.ConversationSummary;
import me.moonote.app.chatkeep.model.FollowUpSection;
import me.moonote.app.chatkeep.model.InsightsSection;
import me.moonote.app.chatkeep.model.QuerySection;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.model.Workaround;

@Component
public class ChatNoteMapper {

  public ChatNote toEntity(ChatNoteDto dto, String userId, String markdownContent) {
    return ChatNote.builder().archiveVersion(dto.getMetadata().getArchiveVersion())
        .archiveType(dto.getMetadata().getArchiveType())
        .createdDate(dto.getMetadata().getCreatedDate())
        .originalPlatform(dto.getMetadata().getOriginalPlatform())
        .attachmentCount(dto.getMetadata().getAttachmentCount())
        .artifactCount(dto.getMetadata().getArtifactCount())
        .chatNoteCompleteness(
            ChatNoteCompleteness.valueOf(dto.getMetadata().getChatNoteCompleteness()))
        .workaroundsCount(dto.getMetadata().getWorkaroundsCount())
        .totalFileSize(dto.getMetadata().getTotalFileSize()).title(dto.getMetadata().getTitle())
        .conversationDate(dto.getMetadata().getConversationDate()).tags(dto.getMetadata().getTags())
        .summary(toSummary(dto.getSummary())).artifacts(toArtifacts(dto.getArtifacts()))
        .attachments(toAttachments(dto.getAttachments()))
        .workarounds(toWorkarounds(dto.getWorkarounds()))
        .markdownContent(markdownContent) // Store original markdown
        .userId(userId)
        .isPublic(false) // Default to private
        .isArchived(false) // Default to active (not archived)
        .isTrashed(false) // Default to not trashed
        .isFavorite(false) // Default to not favorited
        .trashedAt(null) // No trash timestamp initially
        .viewCount(0L).build();
  }

  private ConversationSummary toSummary(ConversationSummaryDto dto) {
    if (dto == null) return null;

    return ConversationSummary.builder().initialQuery(toQuerySection(dto.getInitialQuery()))
        .keyInsights(toInsightsSection(dto.getKeyInsights()))
        .followUpExplorations(toFollowUpSection(dto.getFollowUpExplorations()))
        .references(toReferences(dto.getReferences())).build();
  }

  private QuerySection toQuerySection(QuerySectionDto dto) {
    if (dto == null) return null;

    return QuerySection.builder().description(dto.getDescription())
        .attachmentsReferenced(dto.getAttachmentsReferenced())
        .artifactsCreated(dto.getArtifactsCreated()).build();
  }

  private InsightsSection toInsightsSection(InsightsSectionDto dto) {
    if (dto == null) return null;

    return InsightsSection.builder().description(dto.getDescription()).keyPoints(dto.getKeyPoints())
        .attachmentsReferenced(dto.getAttachmentsReferenced())
        .artifactsCreated(dto.getArtifactsCreated()).build();
  }

  private FollowUpSection toFollowUpSection(FollowUpSectionDto dto) {
    if (dto == null) return null;

    return FollowUpSection.builder().description(dto.getDescription())
        .attachmentsReferenced(dto.getAttachmentsReferenced())
        .artifactsCreated(dto.getArtifactsCreated()).build();
  }

  private List<Reference> toReferences(List<ReferenceDto> dtos) {
    if (dtos == null) return Collections.emptyList();

    return dtos.stream()
        .map(dto -> Reference.builder().url(dto.getUrl()).description(dto.getDescription()).build())
        .collect(Collectors.toList());
  }

  private List<Artifact> toArtifacts(List<ArtifactDto> dtos) {
    if (dtos == null) return Collections.emptyList();

    return dtos.stream()
        .map(dto -> Artifact.builder().type(dto.getType()).title(dto.getTitle())
            .language(dto.getLanguage()).version(dto.getVersion()).iterations(dto.getIterations())
            .evolutionNotes(dto.getEvolutionNotes()).content(dto.getContent()).build())
        .collect(Collectors.toList());
  }

  private List<Attachment> toAttachments(List<AttachmentDto> dtos) {
    if (dtos == null) return Collections.emptyList();

    return dtos.stream()
        .map(dto -> Attachment.builder().filename(dto.getFilename()).content(dto.getContent())
            .isSummarized(dto.getIsSummarized()).originalSize(dto.getOriginalSize())
            .summarizationLevel(dto.getSummarizationLevel())
            .contentPreserved(dto.getContentPreserved())
            .processingLimitation(dto.getProcessingLimitation()).build())
        .collect(Collectors.toList());
  }

  private List<Workaround> toWorkarounds(List<WorkaroundDto> dtos) {
    if (dtos == null) return Collections.emptyList();

    return dtos.stream()
        .map(dto -> Workaround.builder().filename(dto.getFilename()).workaround(dto.getWorkaround())
            .reason(dto.getReason()).preserved(dto.getPreserved()).lost(dto.getLost()).build())
        .collect(Collectors.toList());
  }

  /**
   * Generate a content preview from the conversation summary
   * Takes first ~200 characters for card display
   */
  public String generateContentPreview(ChatNote chatNote) {
    if (chatNote.getSummary() == null) return "";

    StringBuilder preview = new StringBuilder();

    // Try initial query description first
    if (chatNote.getSummary().getInitialQuery() != null
        && chatNote.getSummary().getInitialQuery().getDescription() != null) {
      preview.append(chatNote.getSummary().getInitialQuery().getDescription());
    }

    // If still short, add key insights description
    if (preview.length() < 100 && chatNote.getSummary().getKeyInsights() != null
        && chatNote.getSummary().getKeyInsights().getDescription() != null) {
      if (preview.length() > 0) preview.append(" ");
      preview.append(chatNote.getSummary().getKeyInsights().getDescription());
    }

    String content = preview.toString();
    return content.length() > 200 ? content.substring(0, 200) + "..." : content;
  }

  /**
   * Count total messages in conversation (rough estimate based on summary structure)
   */
  public int estimateMessageCount(ChatNote chatNote) {
    // This is a simple estimation - can be improved with actual message parsing
    int count = 0;
    if (chatNote.getSummary() != null) {
      if (chatNote.getSummary().getInitialQuery() != null) count += 2; // User + AI
      if (chatNote.getSummary().getKeyInsights() != null) count += 4; // Multiple exchanges
      if (chatNote.getSummary().getFollowUpExplorations() != null) count += 4;
    }
    return count;
  }

  /**
   * Count total words in conversation summary
   */
  public int estimateWordCount(ChatNote chatNote) {
    int wordCount = 0;
    if (chatNote.getSummary() != null) {
      wordCount += countWords(chatNote.getSummary().getInitialQuery());
      wordCount += countWords(chatNote.getSummary().getKeyInsights());
      wordCount += countWords(chatNote.getSummary().getFollowUpExplorations());
    }
    return wordCount;
  }

  private int countWords(Object section) {
    if (section == null) return 0;
    String text = section.toString();
    if (text.trim().isEmpty()) return 0;
    return text.split("\\s+").length;
  }

}
