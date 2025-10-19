package me.moonote.app.chatkeep.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.FollowUpSection;
import me.moonote.app.chatkeep.model.InsightsSection;
import me.moonote.app.chatkeep.model.QuerySection;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.model.Workaround;
import me.moonote.app.chatkeep.repository.ArtifactRepository;
import me.moonote.app.chatkeep.repository.AttachmentRepository;

/**
 * Service for generating archive markdown from ChatNote entities.
 *
 * This service dynamically creates markdown content from the current state of a ChatNote entity,
 * ensuring users get the latest data including any edits made through the app, rather than the
 * original markdown content.
 */
@Service
@RequiredArgsConstructor
public class ChatNoteMarkdownGenerator {

  private final ArtifactRepository artifactRepository;
  private final AttachmentRepository attachmentRepository;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String INSTRUCTIONS_FOR_AI = """
      ## Purpose
        This is an archived conversation that has been summarized and preserved for future reference.
        The conversation has been condensed to capture only the meaningful phases and outcomes.

        ## File Structure
        1. This metadata header (YAML front matter)
        2. Conversation summary sections (Initial Query, Key Insights, Follow-up Explorations, References)
        3. Conversation Artifacts section (outputs created during the conversation)
        4. Attachments section (inputs provided to the conversation)
        5. Workarounds Used section (if applicable)
        6. Archive Metadata section

        ## Artifact vs Attachment
        - **Artifacts**: Outputs CREATED during the conversation (code, poems, documents, analyses, etc.)
        - **Attachments**: Inputs PROVIDED to the conversation (uploaded files, documents, images, etc.)
        Both are preserved but serve different purposes.

        ## Artifact Format
        Artifacts use this wrapper structure:

        <!-- ARTIFACT_START: type="code" language="python" title="Script Name" version="final" -->
        [artifact content]
        <!-- ARTIFACT_END -->

        Artifact attributes:
        - type: Category of artifact (code, poem, document, design, analysis, etc.)
        - title: Descriptive name
        - language: (optional) For code artifacts
        - version: (optional) Version identifier
        - Only final or significant milestone versions are included

        ## Attachment Format
        Attachments are located near the bottom in wrapped format:

        <!-- MARKDOWN_START: filename="example.md" -->
        [content here]
        <!-- MARKDOWN_END: filename="example.md" -->

        Important notes about attachments:
        - ALL attachments have been converted to markdown format, regardless of original type
        - Images are embedded as base64-encoded data URIs in markdown image syntax: ![alt](data:image/png;base64,...)
        - PDFs, Word docs, spreadsheets, etc. are converted to markdown tables or text
        - The filename in the wrapper preserves the original filename for reference
        - Some attachments may be summarized if they were too large - check for ⚠️ WARNING markers
        - Check the "Workarounds Used" section to see if any attachments were modified during archiving

        ## Archive Completeness
        Check the ARCHIVE_COMPLETENESS field:
        - COMPLETE: All attachments are fully converted and intact
        - PARTIAL: Some attachments were summarized or simplified
        - SUMMARIZED: Most/all attachments required summarization

        ## How to Process This Archive
        1. Read this entire file to understand the full context
        2. The summarized sections contain the core knowledge - treat them as primary context
        3. Artifacts show what was created/produced during the conversation
        4. Attachments show what was provided as input to the conversation
        5. When a section references an artifact or attachment, locate it by title/filename
        6. All content is already in markdown and directly readable - no extraction needed
        7. If attachments have ⚠️ WARNING markers, they were modified during archiving - see notes

        ## When User Uploads This File
        - Confirm you've loaded the archive and understood the topic
        - Be ready to continue the conversation from where it left off
        - You can reference the summary, artifacts, and attachments
        - Treat the archived information as established context, not as a question
        - Artifacts represent finalized work that can be built upon or referenced""";

  /**
   * Generates complete archive markdown from a ChatNote entity.
   *
   * This includes all sections: YAML frontmatter, summary sections, artifacts, attachments, and
   * metadata. Use this for downloading the full archive.
   *
   * @param chatNote The ChatNote entity to convert to markdown
   * @return Complete markdown archive content
   */
  public String generateMarkdown(ChatNote chatNote) {
    if (chatNote == null) {
      throw new IllegalArgumentException("ChatNote cannot be null");
    }

    StringBuilder markdown = new StringBuilder();

    // 1. YAML Frontmatter
    markdown.append(generateYamlFrontmatter(chatNote));

    // 2. Title and Metadata
    markdown.append(generateTitleAndMetadata(chatNote));

    // 3. Initial Query
    markdown.append(generateInitialQuery(chatNote.getSummary().getInitialQuery()));

    // 4. Key Insights
    markdown.append(generateKeyInsights(chatNote.getSummary().getKeyInsights()));

    // 5. Follow-up Explorations
    markdown.append(generateFollowUpExplorations(chatNote.getSummary().getFollowUpExplorations()));

    // 6. References
    markdown.append(generateReferences(chatNote.getSummary().getReferences()));

    // 7. Artifacts (fetch from separate collection)
    List<Artifact> artifacts = artifactRepository.findByChatNoteIdOrderByCreatedAtDesc(chatNote.getId());
    markdown.append(generateArtifacts(artifacts));

    // 8. Attachments (fetch from separate collection)
    List<Attachment> attachments = attachmentRepository.findByChatNoteIdOrderByCreatedAtDesc(chatNote.getId());
    markdown.append(generateAttachments(attachments));

    // 9. Workarounds
    markdown.append(generateWorkarounds(chatNote.getWorkarounds()));

    // 10. Archive Metadata
    markdown.append(generateArchiveMetadata(chatNote));

    // 11. End marker
    markdown.append("\n---\n\n");
    markdown.append("_End of archived conversation_\n");

    return markdown.toString();
  }

  /**
   * Generates conversation summary markdown without artifacts and attachments.
   *
   * This includes only the conversation summary sections (title, metadata, initial query, key
   * insights, follow-up explorations, and references). Excludes YAML frontmatter, artifacts,
   * attachments, workarounds, and archive metadata. Use this for displaying content in the
   * frontend.
   *
   * @param chatNote The ChatNote entity to convert to markdown
   * @return Conversation summary markdown content
   */
  public String generateConversationContent(ChatNote chatNote) {
    if (chatNote == null) {
      throw new IllegalArgumentException("ChatNote cannot be null");
    }

    StringBuilder markdown = new StringBuilder();

    // 1. Title and Metadata
    markdown.append(generateTitleAndMetadata(chatNote));

    // 2. Initial Query
    markdown.append(generateInitialQuery(chatNote.getSummary().getInitialQuery()));

    // 3. Key Insights
    markdown.append(generateKeyInsights(chatNote.getSummary().getKeyInsights()));

    // 4. Follow-up Explorations
    markdown.append(generateFollowUpExplorations(chatNote.getSummary().getFollowUpExplorations()));

    // 5. References
    markdown.append(generateReferences(chatNote.getSummary().getReferences()));

    return markdown.toString();
  }

  /**
   * Generates YAML frontmatter section.
   */
  private String generateYamlFrontmatter(ChatNote chatNote) {
    StringBuilder yaml = new StringBuilder();

    yaml.append("---\n");
    yaml.append("ARCHIVE_FORMAT_VERSION: ").append(chatNote.getArchiveVersion()).append("\n");
    yaml.append("ARCHIVE_TYPE: ").append(chatNote.getArchiveType()).append("\n");
    yaml.append("CREATED_DATE: ")
        .append(chatNote.getCreatedDate().format(DATE_FORMATTER)).append("\n");
    yaml.append("ORIGINAL_PLATFORM: ").append(chatNote.getOriginalPlatform()).append("\n");
    yaml.append("\n");

    // Instructions for AI
    yaml.append("INSTRUCTIONS_FOR_AI: |\n");
    for (String line : INSTRUCTIONS_FOR_AI.split("\n")) {
      yaml.append("  ").append(line).append("\n");
    }
    yaml.append("\n");

    // Counts and metadata
    yaml.append("ATTACHMENT_COUNT: ").append(chatNote.getAttachmentCount()).append("\n");
    yaml.append("ARTIFACT_COUNT: ").append(chatNote.getArtifactCount()).append("\n");
    yaml.append("ARCHIVE_COMPLETENESS: ").append(chatNote.getChatNoteCompleteness()).append("\n");
    yaml.append("WORKAROUNDS_COUNT: ").append(chatNote.getWorkaroundsCount()).append("\n");
    yaml.append("TOTAL_FILE_SIZE: ").append(chatNote.getTotalFileSize()).append("\n");
    yaml.append("---\n\n");

    return yaml.toString();
  }

  /**
   * Generates title and metadata section.
   */
  private String generateTitleAndMetadata(ChatNote chatNote) {
    StringBuilder section = new StringBuilder();

    section.append("# ").append(chatNote.getTitle()).append("\n\n");
    section.append("**Date:** ")
        .append(chatNote.getConversationDate().format(DATE_FORMATTER)).append("  \n");

    // Tags
    if (chatNote.getTags() != null && !chatNote.getTags().isEmpty()) {
      section.append("**Tags:** [");
      section.append(String.join(", ", chatNote.getTags()));
      section.append("]\n");
    }

    section.append("\n---\n\n");

    return section.toString();
  }

  /**
   * Generates Initial Query section.
   */
  private String generateInitialQuery(QuerySection initialQuery) {
    if (initialQuery == null) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## Initial Query\n\n");

    if (initialQuery.getDescription() != null && !initialQuery.getDescription().isEmpty()) {
      section.append(initialQuery.getDescription()).append("\n\n");
    }

    // Attachments referenced
    if (initialQuery.getAttachmentsReferenced() != null
        && !initialQuery.getAttachmentsReferenced().isEmpty()) {
      section.append("**Attachments referenced:** ");
      section.append(String.join(", ", initialQuery.getAttachmentsReferenced()));
      section.append("\n");
    }

    // Artifacts created
    if (initialQuery.getArtifactsCreated() != null
        && !initialQuery.getArtifactsCreated().isEmpty()) {
      section.append("**Artifacts created:** ");
      section.append(String.join(", ", initialQuery.getArtifactsCreated()));
      section.append("\n");
    }

    section.append("\n---\n\n");

    return section.toString();
  }

  /**
   * Generates Key Insights section.
   */
  private String generateKeyInsights(InsightsSection keyInsights) {
    if (keyInsights == null) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## Key Insights\n\n");

    if (keyInsights.getDescription() != null && !keyInsights.getDescription().isEmpty()) {
      section.append(keyInsights.getDescription()).append("\n\n");
    }

    // Key points
    if (keyInsights.getKeyPoints() != null && !keyInsights.getKeyPoints().isEmpty()) {
      section.append("**Key points:**\n");
      for (String point : keyInsights.getKeyPoints()) {
        section.append("- ").append(point).append("\n");
      }
      section.append("\n");
    }

    // Attachments referenced
    if (keyInsights.getAttachmentsReferenced() != null
        && !keyInsights.getAttachmentsReferenced().isEmpty()) {
      section.append("**Attachments referenced:** ");
      section.append(String.join(", ", keyInsights.getAttachmentsReferenced()));
      section.append("\n");
    }

    // Artifacts created
    if (keyInsights.getArtifactsCreated() != null && !keyInsights.getArtifactsCreated().isEmpty()) {
      section.append("**Artifacts created:** ");
      section.append(String.join(", ", keyInsights.getArtifactsCreated()));
      section.append("\n");
    }

    section.append("\n---\n\n");

    return section.toString();
  }

  /**
   * Generates Follow-up Explorations section.
   */
  private String generateFollowUpExplorations(FollowUpSection followUp) {
    if (followUp == null) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## Follow-up Explorations\n\n");

    if (followUp.getDescription() != null && !followUp.getDescription().isEmpty()) {
      section.append(followUp.getDescription()).append("\n\n");
    }

    // Attachments referenced
    if (followUp.getAttachmentsReferenced() != null
        && !followUp.getAttachmentsReferenced().isEmpty()) {
      section.append("**Attachments referenced:** ");
      section.append(String.join(", ", followUp.getAttachmentsReferenced()));
      section.append("\n");
    }

    // Artifacts created
    if (followUp.getArtifactsCreated() != null && !followUp.getArtifactsCreated().isEmpty()) {
      section.append("**Artifacts created:** ");
      section.append(String.join(", ", followUp.getArtifactsCreated()));
      section.append("\n");
    }

    section.append("\n---\n\n");

    return section.toString();
  }

  /**
   * Generates References/Links section.
   */
  private String generateReferences(List<Reference> references) {
    if (references == null || references.isEmpty()) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## References/Links\n\n");

    for (Reference ref : references) {
      if (ref.getUrl() != null && !ref.getUrl().isEmpty()) {
        if (ref.getDescription() != null && !ref.getDescription().isEmpty()) {
          section.append("- [").append(ref.getDescription()).append("](").append(ref.getUrl())
              .append(")\n");
        } else {
          section.append("- ").append(ref.getUrl()).append("\n");
        }
      }
    }

    section.append("\n---\n\n");

    return section.toString();
  }

  /**
   * Generates Conversation Artifacts section.
   */
  private String generateArtifacts(List<Artifact> artifacts) {
    if (artifacts == null || artifacts.isEmpty()) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## Conversation Artifacts\n\n");
    section.append(
        "_This section preserves the valuable outputs created during the conversation._\n\n");

    for (Artifact artifact : artifacts) {
      // Build artifact start marker with attributes
      section.append("<!-- ARTIFACT_START:");

      // Type (required)
      if (artifact.getType() != null && !artifact.getType().isEmpty()) {
        section.append(" type=\"").append(artifact.getType()).append("\"");
      }

      // Language (optional)
      if (artifact.getLanguage() != null && !artifact.getLanguage().isEmpty()) {
        section.append(" language=\"").append(artifact.getLanguage()).append("\"");
      }

      // Title (required)
      if (artifact.getTitle() != null && !artifact.getTitle().isEmpty()) {
        section.append(" title=\"").append(artifact.getTitle()).append("\"");
      }

      // Version (optional)
      if (artifact.getVersion() != null && !artifact.getVersion().isEmpty()) {
        section.append(" version=\"").append(artifact.getVersion()).append("\"");
      }

      section.append(" -->\n");

      // Artifact content (preserve as-is, including evolution notes if present)
      if (artifact.getContent() != null && !artifact.getContent().isEmpty()) {
        section.append(artifact.getContent());
        // Add newline before END marker if content doesn't end with one
        if (!artifact.getContent().endsWith("\n")) {
          section.append("\n");
        }
      }

      section.append("<!-- ARTIFACT_END -->\n\n");
    }

    section.append("---\n\n");

    return section.toString();
  }

  /**
   * Generates Attachments section.
   */
  private String generateAttachments(List<Attachment> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return "";
    }

    StringBuilder section = new StringBuilder();
    section.append("## Attachments\n\n");

    for (Attachment attachment : attachments) {
      section.append("<!-- MARKDOWN_START: filename=\"").append(attachment.getFilename())
          .append("\" -->\n\n");

      // Add warning marker if summarized
      if (Boolean.TRUE.equals(attachment.getIsSummarized())) {
        section.append(
            "**⚠️ NOTE: This attachment was summarized due to size limitations.**\n");

        if (attachment.getOriginalSize() != null && !attachment.getOriginalSize().isEmpty()) {
          section.append("- Original size: ").append(attachment.getOriginalSize()).append("\n");
        }

        if (attachment.getSummarizationLevel() != null
            && !attachment.getSummarizationLevel().isEmpty()) {
          section.append("- Summarization level: ").append(attachment.getSummarizationLevel())
              .append("\n");
        }

        if (attachment.getContentPreserved() != null
            && !attachment.getContentPreserved().isEmpty()) {
          section.append("- Content preserved: ").append(attachment.getContentPreserved())
              .append("\n");
        }

        section.append("\n");
      }

      // Attachment content
      if (attachment.getContent() != null && !attachment.getContent().isEmpty()) {
        section.append(attachment.getContent()).append("\n\n");
      }

      section.append("<!-- MARKDOWN_END: filename=\"").append(attachment.getFilename())
          .append("\" -->\n\n");
    }

    section.append("---\n\n");

    return section.toString();
  }

  /**
   * Generates Workarounds Used section.
   */
  private String generateWorkarounds(List<Workaround> workarounds) {
    StringBuilder section = new StringBuilder();
    section.append("## Workarounds Used\n\n");
    section.append("_This section documents any limitations encountered during archiving._\n\n");

    if (workarounds == null || workarounds.isEmpty()) {
      section.append(
          "None - All attachments were successfully converted to full markdown format.\n\n");
    } else {
      for (Workaround workaround : workarounds) {
        section.append("- **").append(workaround.getFilename()).append("**: ");

        if (workaround.getWorkaround() != null && !workaround.getWorkaround().isEmpty()) {
          section.append(workaround.getWorkaround());
        }

        if (workaround.getReason() != null && !workaround.getReason().isEmpty()) {
          section.append(" (").append(workaround.getReason()).append(")");
        }

        section.append(".");

        if (workaround.getPreserved() != null && !workaround.getPreserved().isEmpty()) {
          section.append(" Preserved: ").append(workaround.getPreserved()).append(".");
        }

        if (workaround.getLost() != null && !workaround.getLost().isEmpty()) {
          section.append(" Omitted: ").append(workaround.getLost()).append(".");
        }

        section.append("\n");
      }
      section.append("\n");
    }

    section.append("---\n\n");

    return section.toString();
  }

  /**
   * Generates Archive Metadata section.
   */
  private String generateArchiveMetadata(ChatNote chatNote) {
    StringBuilder section = new StringBuilder();
    section.append("## Archive Metadata\n\n");

    section.append("**Original conversation date:** ")
        .append(chatNote.getConversationDate().format(DATE_FORMATTER)).append("  \n");
    section.append("**Archive created:** ")
        .append(chatNote.getCreatedDate().format(DATE_FORMATTER)).append("  \n");
    section.append("**Archive version:** ").append(chatNote.getArchiveVersion()).append("  \n");
    section.append("**Archive completeness:** ").append(chatNote.getChatNoteCompleteness())
        .append("  \n");
    section.append("**Total attachments:** ").append(chatNote.getAttachmentCount()).append("  \n");
    section.append("**Total artifacts:** ").append(chatNote.getArtifactCount()).append("  \n");
    section.append("**Attachments with workarounds:** ").append(chatNote.getWorkaroundsCount())
        .append("  \n");
    section.append("**Total file size:** ").append(chatNote.getTotalFileSize()).append("  \n");

    section.append("\n");

    return section.toString();
  }
}
