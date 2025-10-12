package me.moonote.app.chatkeep.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.ChatNoteMetadataDto;
import me.moonote.app.chatkeep.dto.ArtifactDto;
import me.moonote.app.chatkeep.dto.AttachmentDto;
import me.moonote.app.chatkeep.dto.ConversationSummaryDto;
import me.moonote.app.chatkeep.dto.FollowUpSectionDto;
import me.moonote.app.chatkeep.dto.InsightsSectionDto;
import me.moonote.app.chatkeep.dto.QuerySectionDto;
import me.moonote.app.chatkeep.dto.ReferenceDto;
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

      // Step 1-5: Parse all sections (same as before)
      ChatNoteMetadataDto metadata = parseYamlFrontmatter(markdownContent);
      ConversationSummaryDto summary = parseSummary(markdownContent);
      List<ArtifactDto> artifacts = extractArtifacts(markdownContent);
      List<AttachmentDto> attachments = extractAttachments(markdownContent);
      List<WorkaroundDto> workarounds = extractWorkarounds(markdownContent);

      // Step 6: Create JSON structure
      ChatNoteDto chatNoteDto = ChatNoteDto.builder().metadata(metadata).summary(summary)
          .artifacts(artifacts).attachments(attachments).workarounds(workarounds).build();

      // Step 7: Convert to JSON
      String json = objectMapper.writeValueAsString(chatNoteDto);
      log.debug("Converted archive to JSON, size: {} bytes", json.length());

      // Step 8: Validate against schema
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

    return ChatNoteMetadataDto.builder()
        .archiveVersion(String.valueOf(yamlMap.get("ARCHIVE_FORMAT_VERSION")))
        .archiveType(String.valueOf(yamlMap.get("ARCHIVE_TYPE")))
        .createdDate(LocalDate.parse(String.valueOf(yamlMap.get("CREATED_DATE"))))
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
    Pattern tagsPattern = Pattern.compile("\\*\\*Tags:\\*\\* \\[(.+?)\\]");
    Matcher matcher = tagsPattern.matcher(content);
    if (matcher.find()) {
      String tagsStr = matcher.group(1);
      return Arrays.stream(tagsStr.split(",")).map(String::trim).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private ConversationSummaryDto parseSummary(String content) {
    return ConversationSummaryDto.builder().initialQuery(parseInitialQuery(content))
        .keyInsights(parseKeyInsights(content))
        .followUpExplorations(parseFollowUpExplorations(content))
        .references(parseReferences(content)).build();
  }

  private QuerySectionDto parseInitialQuery(String content) {
    Pattern sectionPattern = Pattern.compile(
        "## Initial Query\\s*\\n\\n(.+?)\\n\\n\\*\\*Attachments referenced:\\*\\* \\[(.+?)\\]\\s*\\n\\*\\*Artifacts created:\\*\\* \\[(.+?)\\]",
        Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (matcher.find()) {
      return QuerySectionDto.builder().description(matcher.group(1).trim())
          .attachmentsReferenced(parseList(matcher.group(2)))
          .artifactsCreated(parseList(matcher.group(3))).build();
    }

    return QuerySectionDto.builder().build();
  }

  private InsightsSectionDto parseKeyInsights(String content) {
    Pattern sectionPattern = Pattern.compile(
        "## Key Insights\\s*\\n\\n(.+?)\\n\\n\\*\\*Key points:\\*\\*\\s*\\n((?:- .+\\n)+)\\n\\*\\*Attachments referenced:\\*\\* \\[(.+?)\\]\\s*\\n\\*\\*Artifacts created:\\*\\* \\[(.+?)\\]",
        Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (matcher.find()) {
      String keyPointsStr = matcher.group(2);
      List<String> keyPoints =
          Arrays.stream(keyPointsStr.split("\n")).map(s -> s.replaceFirst("^- ", "").trim())
              .filter(s -> !s.isEmpty()).collect(Collectors.toList());

      return InsightsSectionDto.builder().description(matcher.group(1).trim()).keyPoints(keyPoints)
          .attachmentsReferenced(parseList(matcher.group(3)))
          .artifactsCreated(parseList(matcher.group(4))).build();
    }

    return InsightsSectionDto.builder().build();
  }

  private FollowUpSectionDto parseFollowUpExplorations(String content) {
    Pattern sectionPattern = Pattern.compile(
        "## Follow-up Explorations\\s*\\n\\n(.+?)\\n\\n\\*\\*Attachments referenced:\\*\\* \\[(.+?)\\]\\s*\\n\\*\\*Artifacts created:\\*\\* \\[(.+?)\\]",
        Pattern.DOTALL);
    Matcher matcher = sectionPattern.matcher(content);

    if (matcher.find()) {
      return FollowUpSectionDto.builder().description(matcher.group(1).trim())
          .attachmentsReferenced(parseList(matcher.group(2)))
          .artifactsCreated(parseList(matcher.group(3))).build();
    }

    return FollowUpSectionDto.builder().build();
  }

  private List<ReferenceDto> parseReferences(String content) {
    List<ReferenceDto> references = new ArrayList<>();
    Pattern refsPattern =
        Pattern.compile("## References/Links\\s*\\n\\n(.+?)\\n\\n---", Pattern.DOTALL);
    Matcher matcher = refsPattern.matcher(content);

    if (matcher.find()) {
      String refsSection = matcher.group(1);
      Pattern refPattern = Pattern.compile("- \\[(.+?)\\]\\((.+?)\\)");
      Matcher refMatcher = refPattern.matcher(refsSection);

      while (refMatcher.find()) {
        references.add(ReferenceDto.builder().description(refMatcher.group(1))
            .url(refMatcher.group(2)).build());
      }
    }

    return references;
  }

  private List<ArtifactDto> extractArtifacts(String content) {
    List<ArtifactDto> artifacts = new ArrayList<>();
    Pattern artifactPattern = Pattern.compile(
        "<!-- ARTIFACT_START: (.*?) -->\\s*\\n(.*?)\\n<!-- ARTIFACT_END -->", Pattern.DOTALL);

    Matcher matcher = artifactPattern.matcher(content);

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

  private List<AttachmentDto> extractAttachments(String content) {
    List<AttachmentDto> attachments = new ArrayList<>();
    Pattern attachmentPattern = Pattern.compile(
        "<!-- MARKDOWN_START: filename=\"(.*?)\" -->\\s*\\n(.*?)\\n<!-- MARKDOWN_END: filename=\".*?\" -->",
        Pattern.DOTALL);

    Matcher matcher = attachmentPattern.matcher(content);

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
      if (!foundContent && (line.trim().startsWith("#") || line.trim().isEmpty())) {
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

  private List<String> parseList(String listStr) {
    if (listStr == null || listStr.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.stream(listStr.split(",")).map(String::trim).filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

}
