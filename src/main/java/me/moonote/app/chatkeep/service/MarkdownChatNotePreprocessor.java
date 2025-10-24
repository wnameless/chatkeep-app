package me.moonote.app.chatkeep.service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.ArtifactDto;
import me.moonote.app.chatkeep.dto.AttachmentDto;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.ChatNoteMetadataDto;
import me.moonote.app.chatkeep.dto.ConversationSummaryDto;
import me.moonote.app.chatkeep.dto.FollowUpSectionDto;
import me.moonote.app.chatkeep.dto.InsightsSectionDto;
import me.moonote.app.chatkeep.dto.QuerySectionDto;
import me.moonote.app.chatkeep.dto.ReferenceDto;
import me.moonote.app.chatkeep.dto.ReferenceType;
import me.moonote.app.chatkeep.dto.WorkaroundDto;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.InvalidChatNoteException;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;
import me.moonote.app.chatkeep.validation.ValidationResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownChatNotePreprocessor {

  private final ObjectMapper objectMapper;
  private final JsonSchemaValidator schemaValidator;

  public ChatNoteValidationResult preprocess(String markdownContent) {
    try {
      log.info("Starting preprocessing of markdown archive");

      // Step 0: Strip code block wrappers if present (users often copy from code blocks)
      markdownContent = stripCodeBlockWrappers(markdownContent);

      // Step 1: Validate basic structure
      List<String> structuralErrors = validateBasicStructure(markdownContent);
      if (!structuralErrors.isEmpty()) {
        log.warn("Archive structural validation failed: {}", structuralErrors);
        return ChatNoteValidationResult.failure(structuralErrors);
      }

      // Step 2-6: Parse all sections
      ChatNoteMetadataDto metadata = parseYamlFrontmatter(markdownContent);
      ConversationSummaryDto summary = parseSummary(markdownContent);
      List<ArtifactDto> artifacts = extractArtifacts(markdownContent);
      List<AttachmentDto> attachments = extractAttachments(markdownContent);
      List<WorkaroundDto> workarounds = extractWorkarounds(markdownContent);

      // Step 7: Calculate and set counts from actual content (backend is authoritative)
      calculateAndSetMetadataCounts(metadata, artifacts, attachments, workarounds, markdownContent);

      // Step 8: Create JSON structure
      ChatNoteDto chatNoteDto = ChatNoteDto.builder().metadata(metadata).summary(summary)
          .artifacts(artifacts).attachments(attachments).workarounds(workarounds).build();

      // Step 9: Convert to JSON
      String json = objectMapper.writeValueAsString(chatNoteDto);
      log.debug("Converted archive to JSON, size: {} bytes", json.length());

      // Step 10: Validate against schema
      ValidationResult validationResult = schemaValidator.validate(json);

      if (validationResult.isValid()) {
        log.info("Chat note validation successful");
        return ChatNoteValidationResult.success(chatNoteDto);
      } else {
        log.warn("Chat note validation failed: {}", validationResult.getErrors());
        return ChatNoteValidationResult.failure(validationResult.getErrors());
      }

    } catch (Exception e) {
      log.error("Error preprocessing archive", e);
      return ChatNoteValidationResult.failure(Collections.singletonList(e.getMessage()));
    }
  }

  /**
   * Strips code block wrappers (```) from markdown content if present. Users often copy archives
   * from code blocks, which adds ``` at the beginning and end.
   *
   * Handles: - Plain code blocks: ```content``` - Language-specific blocks: ```markdown content ```
   * - Multiple backticks: ````content````
   *
   * @param content the markdown content
   * @return content with code block wrappers removed if present, otherwise unchanged
   */
  private String stripCodeBlockWrappers(String content) {
    if (content == null || content.trim().isEmpty()) {
      return content;
    }

    String trimmed = content.trim();

    // Pattern to match opening code fence: ``` or ```language or ````
    // Allows optional language identifier after opening fence
    Pattern openingPattern = Pattern.compile("^(`{3,})\\s*[\\w]*\\s*\\n");
    Matcher openingMatcher = openingPattern.matcher(trimmed);

    if (!openingMatcher.find()) {
      // No opening code fence found
      return content;
    }

    String fence = openingMatcher.group(1); // e.g., "```" or "````"
    int fenceLength = fence.length();

    // Check if content ends with matching closing fence
    Pattern closingPattern = Pattern.compile("\\n`{" + fenceLength + ",}\\s*$");
    Matcher closingMatcher = closingPattern.matcher(trimmed);

    if (!closingMatcher.find()) {
      // No matching closing fence found
      log.debug("Opening code fence found but no matching closing fence, leaving content as-is");
      return content;
    }

    // Remove opening fence (including optional language and newline)
    int startIndex = openingMatcher.end();

    // Remove closing fence (including newline before it)
    int endIndex = closingMatcher.start();

    String unwrapped = trimmed.substring(startIndex, endIndex);
    log.debug("Stripped code block wrappers from markdown content");

    return unwrapped;
  }

  private ChatNoteMetadataDto parseYamlFrontmatter(String content) {
    Pattern yamlPattern = Pattern.compile("^---\\s*\\n(.*?)\\n---", Pattern.DOTALL);
    Matcher matcher = yamlPattern.matcher(content);

    if (!matcher.find()) {
      throw new InvalidChatNoteException("YAML frontmatter not found");
    }

    String yamlContent = matcher.group(1);
    Yaml yaml = new Yaml();
    Map<String, Object> yamlMap = yaml.load(yamlContent);

    // Extract title, date, tags from the main content
    String title = extractTitle(content);
    LocalDate conversationDate = extractConversationDate(content);
    List<String> tags = extractTags(content);

    // Parse CREATED_DATE with flexible format support
    LocalDate createdDate = parseFlexibleDate(String.valueOf(yamlMap.get("CREATED_DATE")));

    return ChatNoteMetadataDto.builder()
        .archiveVersion(String.valueOf(yamlMap.get("ARCHIVE_FORMAT_VERSION")))
        .archiveType(String.valueOf(yamlMap.get("ARCHIVE_TYPE"))).createdDate(createdDate)
        .originalPlatform(String.valueOf(yamlMap.get("ORIGINAL_PLATFORM")))
        .attachmentCount((Integer) yamlMap.get("ATTACHMENT_COUNT"))
        .artifactCount((Integer) yamlMap.get("ARTIFACT_COUNT"))
        .chatNoteCompleteness(String.valueOf(yamlMap.get("ARCHIVE_COMPLETENESS")))
        .workaroundsCount((Integer) yamlMap.get("WORKAROUNDS_COUNT"))
        .totalFileSize(String.valueOf(yamlMap.get("TOTAL_FILE_SIZE"))).title(title)
        .conversationDate(conversationDate).tags(tags).build();
  }

  private String extractTitle(String content) {
    Pattern titlePattern = Pattern.compile("^# (.+)$", Pattern.MULTILINE);
    Matcher matcher = titlePattern.matcher(content);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "Untitled Archive";
  }

  private LocalDate extractConversationDate(String content) {
    Pattern datePattern = Pattern.compile("\\*\\*Date:\\*\\* (\\d{4}-\\d{2}-\\d{2})");
    Matcher matcher = datePattern.matcher(content);
    if (matcher.find()) {
      return LocalDate.parse(matcher.group(1));
    }
    return LocalDate.now();
  }

  private List<String> extractTags(String content) {
    // Try with brackets first: **Tags:** [tag1, tag2, tag3]
    Pattern tagsPatternWithBrackets = Pattern.compile("\\*\\*Tags:\\*\\* \\[(.+?)\\]");
    Matcher matcher = tagsPatternWithBrackets.matcher(content);
    if (matcher.find()) {
      String tagsStr = matcher.group(1);
      return Arrays.stream(tagsStr.split(",")).map(String::trim).collect(Collectors.toList());
    }

    // Fallback: Try without brackets: **Tags:** tag1, tag2, tag3
    Pattern tagsPatternWithoutBrackets = Pattern.compile("\\*\\*Tags:\\*\\*\\s+(.+?)(?=\\n|$)");
    matcher = tagsPatternWithoutBrackets.matcher(content);
    if (matcher.find()) {
      String tagsStr = matcher.group(1).trim();
      return Arrays.stream(tagsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty())
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  /**
   * Parse date with flexible format support.
   *
   * Supports: - ISO 8601: 2025-10-02 - Java Date.toString(): Thu Oct 02 08:00:00 CST 2025 - Other
   * common formats
   */
  private LocalDate parseFlexibleDate(String dateStr) {
    if (dateStr == null || dateStr.equals("null") || dateStr.trim().isEmpty()) {
      return LocalDate.now();
    }

    // Try ISO 8601 format first (YYYY-MM-DD)
    try {
      return LocalDate.parse(dateStr.trim());
    } catch (DateTimeParseException e) {
      // Continue to next format
    }

    // Try Java Date.toString() format: "EEE MMM dd HH:mm:ss zzz yyyy"
    // Example: "Thu Oct 02 08:00:00 CST 2025"
    try {
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
      ZonedDateTime zdt = ZonedDateTime.parse(dateStr.trim(), formatter);
      return zdt.toLocalDate();
    } catch (DateTimeParseException e) {
      // Continue to next format
    }

    // Try other common formats
    String[] patterns = {"yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "MM-dd-yyyy"};

    for (String pattern : patterns) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(dateStr.trim(), formatter);
      } catch (DateTimeParseException e) {
        // Continue to next format
      }
    }

    // If all parsing fails, log warning and return current date
    log.warn("Failed to parse date '{}', using current date", dateStr);
    return LocalDate.now();
  }

  private ConversationSummaryDto parseSummary(String content) {
    return ConversationSummaryDto.builder().initialQuery(parseInitialQuery(content))
        .keyInsights(parseKeyInsights(content))
        .followUpExplorations(parseFollowUpExplorations(content))
        .references(parseReferences(content)).build();
  }

  private QuerySectionDto parseInitialQuery(String content) {
    // Extract section content between "## Initial Query" and next "##" or "---"
    Pattern sectionPattern =
        Pattern.compile("## Initial Query\\s*\\n\\n(.+?)(?=\\n##|\\n---)", Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (!matcher.find()) {
      return QuerySectionDto.builder().build();
    }

    String sectionContent = matcher.group(1).trim();

    // Extract description (everything before artifacts/attachments references)
    Pattern descPattern = Pattern.compile(
        "^(.+?)(?=\\n\\*\\*(?:Attachments referenced|Artifacts (?:created|referenced)))",
        Pattern.DOTALL);
    Matcher descMatcher = descPattern.matcher(sectionContent);
    String description = descMatcher.find() ? descMatcher.group(1).trim() : sectionContent;

    // Extract attachments referenced (flexible format - with or without brackets)
    List<String> attachmentsReferenced =
        extractReferencedItems(sectionContent, "Attachments referenced");

    // Extract artifacts created/referenced (flexible format)
    List<String> artifactsCreated =
        extractReferencedItems(sectionContent, "Artifacts (?:created|referenced)");

    return QuerySectionDto.builder().description(description)
        .attachmentsReferenced(attachmentsReferenced).artifactsCreated(artifactsCreated).build();
  }

  private InsightsSectionDto parseKeyInsights(String content) {
    // Extract section content between "## Key Insights" and next "##" or "---"
    Pattern sectionPattern =
        Pattern.compile("## Key Insights\\s*\\n\\n(.+?)(?=\\n##|\\n---)", Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (!matcher.find()) {
      return InsightsSectionDto.builder().build();
    }

    String sectionContent = matcher.group(1).trim();

    // Extract description (everything before artifacts/attachments references)
    Pattern descPattern = Pattern.compile(
        "^(.+?)(?=\\n\\*\\*(?:Attachments referenced|Artifacts (?:created|referenced)))",
        Pattern.DOTALL);
    Matcher descMatcher = descPattern.matcher(sectionContent);
    String description = descMatcher.find() ? descMatcher.group(1).trim() : sectionContent;

    // Extract key points (all bullet points in the description)
    List<String> keyPoints = new ArrayList<>();
    Pattern bulletPattern = Pattern.compile("^[-*]\\s+(.+)$", Pattern.MULTILINE);
    Matcher bulletMatcher = bulletPattern.matcher(description);
    while (bulletMatcher.find()) {
      keyPoints.add(bulletMatcher.group(1).trim());
    }

    // Extract attachments referenced
    List<String> attachmentsReferenced =
        extractReferencedItems(sectionContent, "Attachments referenced");

    // Extract artifacts created/referenced
    List<String> artifactsCreated =
        extractReferencedItems(sectionContent, "Artifacts (?:created|referenced)");

    return InsightsSectionDto.builder().description(description).keyPoints(keyPoints)
        .attachmentsReferenced(attachmentsReferenced).artifactsCreated(artifactsCreated).build();
  }

  private FollowUpSectionDto parseFollowUpExplorations(String content) {
    // Extract section content between "## Follow-up Explorations" and next "##" or "---"
    Pattern sectionPattern =
        Pattern.compile("## Follow-up Explorations\\s*\\n\\n(.+?)(?=\\n##|\\n---)", Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (!matcher.find()) {
      return FollowUpSectionDto.builder().build();
    }

    String sectionContent = matcher.group(1).trim();

    // Extract description (everything before artifacts/attachments references)
    Pattern descPattern = Pattern.compile(
        "^(.+?)(?=\\n\\*\\*(?:Attachments referenced|Artifacts (?:created|referenced)))",
        Pattern.DOTALL);
    Matcher descMatcher = descPattern.matcher(sectionContent);
    String description = descMatcher.find() ? descMatcher.group(1).trim() : sectionContent;

    // Extract attachments referenced
    List<String> attachmentsReferenced =
        extractReferencedItems(sectionContent, "Attachments referenced");

    // Extract artifacts created/referenced
    List<String> artifactsCreated =
        extractReferencedItems(sectionContent, "Artifacts (?:created|referenced)");

    return FollowUpSectionDto.builder().description(description)
        .attachmentsReferenced(attachmentsReferenced).artifactsCreated(artifactsCreated).build();
  }

  private List<ReferenceDto> parseReferences(String content) {
    List<ReferenceDto> references = new ArrayList<>();
    Pattern refsPattern =
        Pattern.compile("## References/Links\\s*\\n\\n(.+?)(?=\\n##|\\n---)", Pattern.DOTALL);
    Matcher matcher = refsPattern.matcher(content);

    if (!matcher.find()) {
      return references;
    }

    String refsSection = matcher.group(1);
    String[] lines = refsSection.split("\n");

    for (String line : lines) {
      String trimmedLine = line.trim();

      // Skip empty lines and lines that don't start with bullet point
      if (trimmedLine.isEmpty() || !trimmedLine.startsWith("-")) {
        continue;
      }

      // Remove bullet point and trim
      String lineContent = trimmedLine.substring(1).trim();

      // Try Format 1: Markdown link [Description](URL)
      Pattern mdLinkPattern = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");
      Matcher mdLinkMatcher = mdLinkPattern.matcher(lineContent);
      if (mdLinkMatcher.find()) {
        references.add(ReferenceDto.builder().description(mdLinkMatcher.group(1).trim())
            .url(mdLinkMatcher.group(2).trim()).type(ReferenceType.EXTERNAL_LINK).build());
        continue;
      }

      // Try Format 2: Plain "Description: URL (optional comment)"
      if (lineContent.contains("http://") || lineContent.contains("https://")) {
        int colonIndex = lineContent.indexOf(':');
        if (colonIndex > 0) {
          String description = lineContent.substring(0, colonIndex).trim();
          String remainder = lineContent.substring(colonIndex + 1).trim();

          // Extract URL (first token, stop at space or parenthesis)
          String[] tokens = remainder.split("[\\s()]");
          String url = tokens[0].trim();

          // Only add if URL is not empty - otherwise treat as descriptive reference
          if (!url.isEmpty()) {
            references.add(ReferenceDto.builder().description(description).url(url)
                .type(ReferenceType.EXTERNAL_LINK).build());
            continue;
          }
        }
      }

      // Format 3: Description-only (no URL) - Descriptive reference
      // The description itself conveys the nature (concept, contextual info, etc.)
      references.add(ReferenceDto.builder().description(lineContent).url(null)
          .type(ReferenceType.DESCRIPTIVE).build());
    }

    return references;
  }

  private List<ArtifactDto> extractArtifacts(String content) {
    List<ArtifactDto> artifacts = new ArrayList<>();

    // Find the end of YAML frontmatter to avoid matching examples in INSTRUCTIONS_FOR_AI
    int yamlEnd = findYamlFrontmatterEnd(content);
    String contentAfterYaml = yamlEnd > 0 ? content.substring(yamlEnd) : content;

    Pattern artifactPattern = Pattern.compile(
        ":::artifact (.*?)\\n(.*?)\\n:::", Pattern.DOTALL);

    Matcher matcher = artifactPattern.matcher(contentAfterYaml);

    while (matcher.find()) {
      String attributes = matcher.group(1);
      String artifactContent = matcher.group(2).trim();

      Map<String, String> attrs = parseAttributes(attributes);

      // Extract evolution notes (lines starting with #)
      String evolutionNotes = extractEvolutionNotes(artifactContent);
      String cleanContent = removeEvolutionNotes(artifactContent);

      artifacts.add(ArtifactDto.builder().type(attrs.get("type")).title(attrs.get("title"))
          .language(attrs.get("language")).version(attrs.get("version"))
          .iterations(attrs.get("iterations")).evolutionNotes(evolutionNotes).content(cleanContent)
          .build());
    }

    return artifacts;
  }

  /**
   * Find the end position of YAML frontmatter (second "---" marker). Returns the index after the
   * closing "---", or -1 if not found.
   */
  private int findYamlFrontmatterEnd(String content) {
    Pattern yamlPattern =
        Pattern.compile("^---\\s*\\n.*?\\n---", Pattern.DOTALL | Pattern.MULTILINE);
    Matcher matcher = yamlPattern.matcher(content);
    if (matcher.find()) {
      return matcher.end();
    }
    return -1;
  }

  private List<AttachmentDto> extractAttachments(String content) {
    List<AttachmentDto> attachments = new ArrayList<>();

    // Find the end of YAML frontmatter to avoid matching examples in INSTRUCTIONS_FOR_AI
    int yamlEnd = findYamlFrontmatterEnd(content);
    String contentAfterYaml = yamlEnd > 0 ? content.substring(yamlEnd) : content;

    Pattern attachmentPattern = Pattern.compile(
        ":::attachment filename=\"(.*?)\"\\n(.*?)\\n:::",
        Pattern.DOTALL);

    Matcher matcher = attachmentPattern.matcher(contentAfterYaml);

    while (matcher.find()) {
      String filename = matcher.group(1);
      String attachmentContent = matcher.group(2).trim();

      boolean isSummarized = attachmentContent.contains("⚠️ NOTE:");

      AttachmentDto.AttachmentDtoBuilder builder = AttachmentDto.builder().filename(filename)
          .content(attachmentContent).isSummarized(isSummarized);

      if (isSummarized) {
        parseWarningDetails(attachmentContent, builder);
      }

      attachments.add(builder.build());
    }

    return attachments;
  }

  private List<WorkaroundDto> extractWorkarounds(String content) {
    List<WorkaroundDto> workarounds = new ArrayList<>();
    Pattern workaroundsPattern = Pattern.compile(
        "## Workarounds Used\\s*\\n\\n.*?\\n\\n((?:- \\*\\*.+?\\*\\*:.+?\\n\\n)+)", Pattern.DOTALL);

    Matcher matcher = workaroundsPattern.matcher(content);

    if (matcher.find()) {
      String workaroundsSection = matcher.group(1);
      Pattern entryPattern =
          Pattern.compile("- \\*\\*(.+?)\\*\\*: (.+?)(?=\\n\\n|$)", Pattern.DOTALL);
      Matcher entryMatcher = entryPattern.matcher(workaroundsSection);

      while (entryMatcher.find()) {
        String filename = entryMatcher.group(1);
        String description = entryMatcher.group(2).trim();

        workarounds.add(WorkaroundDto.builder().filename(filename).workaround(description).build());
      }
    }

    return workarounds;
  }

  private Map<String, String> parseAttributes(String attributeString) {
    Map<String, String> attrs = new HashMap<>();
    Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]+)\"");
    Matcher matcher = attrPattern.matcher(attributeString);

    while (matcher.find()) {
      attrs.put(matcher.group(1), matcher.group(2));
    }

    return attrs;
  }

  private String extractEvolutionNotes(String content) {
    StringBuilder notes = new StringBuilder();
    String[] lines = content.split("\n");

    for (String line : lines) {
      if (line.trim().startsWith("#")) {
        notes.append(line.trim()).append("\n");
      } else if (!line.trim().isEmpty()) {
        break; // Stop at first non-comment, non-empty line
      }
    }

    return notes.toString().trim();
  }

  private String removeEvolutionNotes(String content) {
    String[] lines = content.split("\n");
    StringBuilder cleaned = new StringBuilder();
    boolean foundContent = false;

    for (String line : lines) {
      String trimmed = line.trim();

      // Skip evolution notes at the beginning: lines starting with # (but NOT shebangs)
      // Shebangs start with #! and should be preserved as actual code
      if (!foundContent
          && (trimmed.isEmpty() || (trimmed.startsWith("#") && !trimmed.startsWith("#!")))) {
        continue;
      }

      foundContent = true;
      cleaned.append(line).append("\n");
    }

    return cleaned.toString().trim();
  }

  private void parseWarningDetails(String content, AttachmentDto.AttachmentDtoBuilder builder) {
    Pattern sizePattern = Pattern.compile("- Original size: (.+)");
    Pattern levelPattern = Pattern.compile("- Summarization level: (.+)");
    Pattern preservedPattern = Pattern.compile("- Content preserved: (.+)");
    Pattern limitationPattern = Pattern.compile("- Processing limitation: (.+)");

    Matcher sizeMatcher = sizePattern.matcher(content);
    if (sizeMatcher.find()) {
      builder.originalSize(sizeMatcher.group(1).trim());
    }

    Matcher levelMatcher = levelPattern.matcher(content);
    if (levelMatcher.find()) {
      builder.summarizationLevel(levelMatcher.group(1).trim());
    }

    Matcher preservedMatcher = preservedPattern.matcher(content);
    if (preservedMatcher.find()) {
      builder.contentPreserved(preservedMatcher.group(1).trim());
    }

    Matcher limitationMatcher = limitationPattern.matcher(content);
    if (limitationMatcher.find()) {
      builder.processingLimitation(limitationMatcher.group(1).trim());
    }
  }

  /**
   * Extract referenced items (attachments/artifacts) from section content. Handles both formats: -
   * With brackets: **Attachments referenced:** [file1.md, file2.png] - Without brackets:
   * **Artifacts referenced:** script.sh (final version)
   */
  private List<String> extractReferencedItems(String sectionContent, String fieldName) {
    // Pattern matches: **FieldName:** [items] or **FieldName:** items
    // Use .*? instead of .+? to allow empty brackets []
    Pattern pattern = Pattern.compile(
        "\\*\\*" + fieldName + ":\\*\\*\\s*(?:\\[(.*?)\\]|(.+?)(?=\\n|$))", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(sectionContent);

    if (matcher.find()) {
      // Group 1: items inside brackets [...]
      // Group 2: items without brackets
      String itemsStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

      if (itemsStr != null && !itemsStr.trim().isEmpty()) {
        // Split by comma and clean up
        return Arrays.stream(itemsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
      }
    }

    return Collections.emptyList();
  }

  /**
   * Validates the basic structure of the archive markdown. Checks for required sections and proper
   * formatting.
   *
   * @param content the markdown content
   * @return list of validation error messages (empty if valid)
   */
  private List<String> validateBasicStructure(String content) {
    List<String> errors = new ArrayList<>();

    // Check for YAML frontmatter
    if (!content.trim().startsWith("---")) {
      errors.add(
          "Missing YAML frontmatter at the beginning of the archive. Archives must start with '---' followed by metadata fields. "
              + "This may indicate the AI did not follow the archiving specification correctly.");
      return errors; // Can't continue without YAML
    }

    Pattern yamlPattern = Pattern.compile("^---\\s*\\n(.*?)\\n---", Pattern.DOTALL);
    Matcher yamlMatcher = yamlPattern.matcher(content);
    if (!yamlMatcher.find()) {
      errors.add(
          "Malformed YAML frontmatter. Expected format: '---' (opening), metadata fields, '---' (closing). "
              + "Make sure the YAML section is properly closed with '---' on its own line.");
      return errors;
    }

    // Check for required sections
    if (!content.contains("## Initial Query")) {
      errors.add(
          "Missing required section: '## Initial Query'. This section should describe what the user was trying to accomplish. "
              + "The AI may have skipped this section or used a different heading.");
    }

    if (!content.contains("## Key Insights")) {
      errors.add(
          "Missing required section: '## Key Insights'. This section should contain the main findings or solutions. "
              + "The AI may have skipped this section or used a different heading.");
    }

    // Check for Conversation Artifacts section (if ARTIFACT_COUNT > 0)
    // Accept both "## Conversation Artifacts" (current spec) and "## Artifacts" (legacy)
    String yamlContent = yamlMatcher.group(1);
    Pattern artifactCountPattern = Pattern.compile("ARTIFACT_COUNT:\\s*(\\d+)");
    Matcher artifactCountMatcher = artifactCountPattern.matcher(yamlContent);
    if (artifactCountMatcher.find()) {
      int artifactCount = Integer.parseInt(artifactCountMatcher.group(1));
      if (artifactCount > 0 && !content.contains("## Conversation Artifacts")
          && !content.contains("## Artifacts")) {
        errors.add(
            "Missing '## Conversation Artifacts' (or '## Artifacts') section, but ARTIFACT_COUNT is "
                + artifactCount + ". "
                + "Either the section is missing or the count is incorrect. Artifacts should be outputs created during the conversation.");
      }
    }

    // Check for Attachments section (if ATTACHMENT_COUNT > 0)
    Pattern attachmentCountPattern = Pattern.compile("ATTACHMENT_COUNT:\\s*(\\d+)");
    Matcher attachmentCountMatcher = attachmentCountPattern.matcher(yamlContent);
    if (attachmentCountMatcher.find()) {
      int attachmentCount = Integer.parseInt(attachmentCountMatcher.group(1));
      if (attachmentCount > 0 && !content.contains("## Attachments")) {
        errors.add("Missing '## Attachments' section, but ATTACHMENT_COUNT is " + attachmentCount
            + ". "
            + "Either the section is missing or the count is incorrect. Attachments should be inputs provided to the conversation.");
      }
    }

    return errors;
  }

  /**
   * Calculates and sets metadata counts from actual content. Backend is authoritative source for
   * all counts - YAML values are ignored if present.
   *
   * @param metadata the metadata object to update
   * @param artifacts the list of extracted artifacts
   * @param attachments the list of extracted attachments
   * @param workarounds the list of extracted workarounds
   * @param markdownContent the full markdown content for size calculation
   */
  private void calculateAndSetMetadataCounts(ChatNoteMetadataDto metadata,
      List<ArtifactDto> artifacts, List<AttachmentDto> attachments,
      List<WorkaroundDto> workarounds, String markdownContent) {

    // Calculate counts from actual content
    metadata.setArtifactCount(artifacts.size());
    metadata.setAttachmentCount(attachments.size());
    metadata.setWorkaroundsCount(workarounds.size());

    // Calculate total file size from markdown content
    long sizeInBytes = markdownContent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    long sizeInKB = (sizeInBytes + 512) / 1024; // Round to nearest KB
    metadata.setTotalFileSize(sizeInKB + " KB");

    // Derive chatNoteCompleteness from workarounds
    if (workarounds.isEmpty()) {
      metadata.setChatNoteCompleteness("COMPLETE");
    } else {
      // If attachments exist and workarounds cover most/all of them, it's SUMMARIZED
      // Otherwise it's PARTIAL
      int attachmentCount = attachments.size();
      int workaroundCount = workarounds.size();

      if (attachmentCount > 0 && workaroundCount >= attachmentCount) {
        metadata.setChatNoteCompleteness("SUMMARIZED");
      } else {
        metadata.setChatNoteCompleteness("PARTIAL");
      }
    }

    log.debug("Calculated metadata: artifacts={}, attachments={}, workarounds={}, completeness={}, size={}",
        metadata.getArtifactCount(), metadata.getAttachmentCount(), metadata.getWorkaroundsCount(),
        metadata.getChatNoteCompleteness(), metadata.getTotalFileSize());
  }

}
