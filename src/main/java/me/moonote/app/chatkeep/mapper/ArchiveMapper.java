package me.moonote.app.chatkeep.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import me.moonote.app.chatkeep.dto.ArchiveDto;
import me.moonote.app.chatkeep.dto.ArtifactDto;
import me.moonote.app.chatkeep.dto.AttachmentDto;
import me.moonote.app.chatkeep.dto.ConversationSummaryDto;
import me.moonote.app.chatkeep.dto.FollowUpSectionDto;
import me.moonote.app.chatkeep.dto.InsightsSectionDto;
import me.moonote.app.chatkeep.dto.QuerySectionDto;
import me.moonote.app.chatkeep.dto.ReferenceDto;
import me.moonote.app.chatkeep.dto.WorkaroundDto;
import me.moonote.app.chatkeep.model.ArchiveCompleteness;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ConversationArchive;
import me.moonote.app.chatkeep.model.ConversationSummary;
import me.moonote.app.chatkeep.model.FollowUpSection;
import me.moonote.app.chatkeep.model.InsightsSection;
import me.moonote.app.chatkeep.model.QuerySection;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.model.Workaround;

@Component
public class ArchiveMapper {

  public ConversationArchive toEntity(ArchiveDto dto, String userId) {
    return ConversationArchive.builder().archiveVersion(dto.getMetadata().getArchiveVersion())
        .archiveType(dto.getMetadata().getArchiveType())
        .createdDate(dto.getMetadata().getCreatedDate())
        .originalPlatform(dto.getMetadata().getOriginalPlatform())
        .attachmentCount(dto.getMetadata().getAttachmentCount())
        .artifactCount(dto.getMetadata().getArtifactCount())
        .archiveCompleteness(
            ArchiveCompleteness.valueOf(dto.getMetadata().getArchiveCompleteness()))
        .workaroundsCount(dto.getMetadata().getWorkaroundsCount())
        .totalFileSize(dto.getMetadata().getTotalFileSize()).title(dto.getMetadata().getTitle())
        .conversationDate(dto.getMetadata().getConversationDate()).tags(dto.getMetadata().getTags())
        .summary(toSummary(dto.getSummary())).artifacts(toArtifacts(dto.getArtifacts()))
        .attachments(toAttachments(dto.getAttachments()))
        .workarounds(toWorkarounds(dto.getWorkarounds())).userId(userId).isPublic(false) // Default
                                                                                         // to
                                                                                         // private
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

}
