package me.moonote.app.chatkeep.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.mapper.ChatNoteMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.ChatNoteCompleteness;
import me.moonote.app.chatkeep.model.ConversationSummary;
import me.moonote.app.chatkeep.model.FollowUpSection;
import me.moonote.app.chatkeep.model.InsightsSection;
import me.moonote.app.chatkeep.model.QuerySection;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.model.Workaround;
import me.moonote.app.chatkeep.repository.ArtifactRepository;
import me.moonote.app.chatkeep.repository.AttachmentRepository;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

@SpringBootTest
class ChatNoteMarkdownGeneratorTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  @Mock
  private ArtifactRepository artifactRepository;

  @Mock
  private AttachmentRepository attachmentRepository;

  private ChatNoteMarkdownGenerator generator;
  private MarkdownChatNotePreprocessor preprocessor;
  private ChatNoteMapper mapper;

  private String dragonwellMarkdown;
  private ChatNote dragonwellChatNote;
  private ChatNoteDto dragonwellDto;

  // In-memory storage for test artifacts/attachments
  private List<Artifact> testArtifacts = new ArrayList<>();
  private List<Attachment> testAttachments = new ArrayList<>();

  @SuppressWarnings("unused")
  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);

    generator = new ChatNoteMarkdownGenerator(artifactRepository, attachmentRepository);
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);
    mapper = new ChatNoteMapper();

    // Mock repository behavior to return our test data regardless of chatNoteId
    // (chatNoteId might be null in tests since entities aren't saved to MongoDB)
    when(artifactRepository.findByChatNoteIdOrderByCreatedAtDesc(anyString()))
        .thenAnswer(invocation -> new ArrayList<>(testArtifacts));
    when(attachmentRepository.findByChatNoteIdOrderByCreatedAtDesc(anyString()))
        .thenAnswer(invocation -> new ArrayList<>(testAttachments));

    // Also handle null ID case
    when(artifactRepository.findByChatNoteIdOrderByCreatedAtDesc(null))
        .thenAnswer(invocation -> new ArrayList<>(testArtifacts));
    when(attachmentRepository.findByChatNoteIdOrderByCreatedAtDesc(null))
        .thenAnswer(invocation -> new ArrayList<>(testAttachments));

    // Load and preprocess the dragonwell markdown for reference
    dragonwellMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/dragonwell.md"));

    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    dragonwellDto = result.getChatNoteDto();
    dragonwellChatNote = mapper.toEntity(dragonwellDto, "test-user");

    // Populate test data from dragonwell DTO
    populateTestDataFromDto(dragonwellDto);
  }

  @Test
  void testGenerateMarkdown_WithNullChatNote_ShouldThrowException() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> generator.generateMarkdown(null),
        "Should throw exception for null ChatNote");
  }

  @Test
  void testGenerateMarkdown_WithDragonwellChatNote_ShouldGenerateValidMarkdown() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertNotNull(generatedMarkdown, "Generated markdown should not be null");
    assertTrue(generatedMarkdown.length() > 1000, "Generated markdown should be substantial");

    // Verify key sections are present
    assertTrue(generatedMarkdown.contains("---\nARCHIVE_FORMAT_VERSION:"),
        "Should contain YAML frontmatter");
    assertTrue(generatedMarkdown.contains("# Building Dragonwell"), "Should contain title");
    assertTrue(generatedMarkdown.contains("## Initial Query"), "Should contain Initial Query");
    assertTrue(generatedMarkdown.contains("## Key Insights"), "Should contain Key Insights");
    assertTrue(generatedMarkdown.contains("## Follow-up Explorations"), "Should contain Follow-up");
    assertTrue(generatedMarkdown.contains("## References/Links"), "Should contain References");
    assertTrue(generatedMarkdown.contains("## Conversation Artifacts"), "Should contain Artifacts");
    // Dragonwell has no attachments, so Attachments section should not be present
    assertFalse(generatedMarkdown.contains("## Attachments"),
        "Should not contain Attachments section if no attachments");
    assertTrue(generatedMarkdown.contains("## Workarounds Used"), "Should contain Workarounds");
    assertTrue(generatedMarkdown.contains("## Archive Metadata"),
        "Should contain Archive Metadata");
    assertTrue(generatedMarkdown.contains("_End of archived conversation_"),
        "Should contain end marker");
  }

  @Test
  void testGenerateMarkdown_ShouldContainYamlFrontmatter() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert - YAML structure
    assertTrue(generatedMarkdown.startsWith("---\n"), "Should start with YAML delimiter");
    assertTrue(generatedMarkdown.contains("ARCHIVE_FORMAT_VERSION: 1.0"), "Should contain version");
    assertTrue(generatedMarkdown.contains("ARCHIVE_TYPE: conversation_summary"),
        "Should contain type");
    assertTrue(generatedMarkdown.contains("CREATED_DATE: 2025-10-02"), "Should contain date");
    assertTrue(generatedMarkdown.contains("ORIGINAL_PLATFORM: Claude (Anthropic)"),
        "Should contain platform");
    assertTrue(generatedMarkdown.contains("INSTRUCTIONS_FOR_AI: |"),
        "Should contain AI instructions");
  }

  @Test
  void testGenerateMarkdown_ShouldContainTitleAndTags() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown
        .contains("# Building Dragonwell JDK 21 on macOS with Compact Object Headers"));
    assertTrue(generatedMarkdown.contains("**Date:** 2025-10-02"));
    assertTrue(generatedMarkdown.contains("**Tags:**"));
    assertTrue(generatedMarkdown.contains("java"));
    assertTrue(generatedMarkdown.contains("dragonwell"));
    assertTrue(generatedMarkdown.contains("jdk21"));
    assertTrue(generatedMarkdown.contains("macos"));
  }

  @Test
  void testGenerateMarkdown_ShouldContainInitialQuerySection() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Initial Query"));
    assertTrue(generatedMarkdown.contains("Java 21 builds"),
        "Should contain query description content");
    assertTrue(generatedMarkdown.contains("**Artifacts created:**"),
        "Should contain artifacts created");
    assertTrue(generatedMarkdown.contains("build-osx.sh"),
        "Should reference artifact in query section");
  }

  @Test
  void testGenerateMarkdown_ShouldContainKeyInsightsSection() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Key Insights"));
    assertTrue(generatedMarkdown.contains("Compact Object Headers"),
        "Should contain insights description");
    assertTrue(generatedMarkdown.contains("**Key points:**"), "Should contain key points");
    assertTrue(generatedMarkdown.contains("**Artifacts created:**"),
        "Should contain artifacts in insights");
  }

  @Test
  void testGenerateMarkdown_ShouldContainFollowUpSection() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Follow-up Explorations"));
    assertTrue(generatedMarkdown.contains("Metal compiler"),
        "Should contain follow-up description");
  }

  @Test
  void testGenerateMarkdown_ShouldContainReferences() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## References/Links"));
    assertTrue(generatedMarkdown.contains("github.com/dragonwell-project"),
        "Should contain GitHub reference");
    assertTrue(generatedMarkdown.contains("openjdk.org/jeps"), "Should contain JEP reference");

    // References should be in markdown link format
    assertTrue(generatedMarkdown.contains("[") && generatedMarkdown.contains("]("),
        "References should use markdown link format");
  }

  @Test
  void testGenerateMarkdown_ShouldContainArtifacts() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Conversation Artifacts"));
    assertTrue(generatedMarkdown.contains(":::artifact"),
        "Should contain artifact start marker");
    assertTrue(generatedMarkdown.contains(":::"),
        "Should contain artifact end marker");
    assertTrue(generatedMarkdown.contains("type=\"script\""), "Should contain artifact type");
    assertTrue(generatedMarkdown.contains("language=\"bash\""), "Should contain artifact language");
    assertTrue(generatedMarkdown.contains("title=\"Dragonwell macOS Build Script\""),
        "Should contain artifact title");
    assertTrue(generatedMarkdown.contains("version=\"final\""), "Should contain artifact version");

    // Artifact content should be preserved
    assertTrue(generatedMarkdown.contains("#!/bin/bash"), "Should preserve shebang");
    assertTrue(generatedMarkdown.contains("bash configure"), "Should preserve script content");
    assertTrue(generatedMarkdown.contains("make images"), "Should preserve build commands");
  }

  @Test
  void testGenerateMarkdown_ShouldPreserveArtifactContent() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert - Verify artifact markers and key content are present
    assertTrue(generatedMarkdown.contains(":::artifact"),
        "Should contain artifact start marker");
    assertTrue(generatedMarkdown.contains(":::"),
        "Should contain artifact end marker");

    // Verify key content from original artifact is preserved
    assertTrue(generatedMarkdown.contains("#!/bin/bash"),
        "Should preserve shebang from artifact content");
    assertTrue(generatedMarkdown.contains("bash configure"), "Should preserve key script commands");
    assertTrue(generatedMarkdown.contains("make images"), "Should preserve build commands");

    // Note: Exact whitespace preservation is tested in round-trip conversion tests
  }

  @Test
  void testGenerateMarkdown_WithNoAttachments_ShouldNotHaveAttachmentsSection() {
    // Dragonwell has no attachments, so this is a good test case
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert - Section should be absent if no attachments
    assertFalse(generatedMarkdown.contains("## Attachments"),
        "Should not have Attachments section if no attachments");
  }

  @Test
  void testGenerateMarkdown_WithNoWorkarounds_ShouldShowNoneMessage() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Workarounds Used"));
    assertTrue(generatedMarkdown.contains("None - All attachments were successfully converted"),
        "Should show 'None' message for empty workarounds");
  }

  @Test
  void testGenerateMarkdown_ShouldContainArchiveMetadata() {
    // Act
    String generatedMarkdown = generator.generateMarkdown(dragonwellChatNote);

    // Assert
    assertTrue(generatedMarkdown.contains("## Archive Metadata"));
    assertTrue(generatedMarkdown.contains("**Original conversation date:** 2025-10-02"));
    assertTrue(generatedMarkdown.contains("**Archive created:** 2025-10-02"));
    assertTrue(generatedMarkdown.contains("**Archive version:** 1.0"));
    assertTrue(generatedMarkdown.contains("**Archive completeness:** COMPLETE"));
    assertTrue(generatedMarkdown.contains("**Total attachments:** 0"));
    assertTrue(generatedMarkdown.contains("**Total artifacts:** 1"));
    assertTrue(generatedMarkdown.contains("**Attachments with workarounds:** 0"));
    assertTrue(generatedMarkdown.contains("**Total file size:** 14 KB"));
  }

  @Test
  void testGenerateMarkdown_WithMinimalChatNote_ShouldHandleEmptyFields() {
    // Arrange - Create minimal ChatNote with required fields only
    ChatNote minimalNote = ChatNote.builder().archiveVersion("1.0")
        .archiveType("conversation_summary").createdDate(LocalDate.of(2025, 1, 1))
        .originalPlatform("Test Platform").attachmentCount(0).artifactCount(0)
        .chatNoteCompleteness(ChatNoteCompleteness.COMPLETE).workaroundsCount(0)
        .totalFileSize("1 KB").title("Test Title").conversationDate(LocalDate.of(2025, 1, 1))
        .tags(Collections.emptyList())
        .summary(ConversationSummary.builder()
            .initialQuery(QuerySection.builder().description("Test query")
                .attachmentsReferenced(Collections.emptyList())
                .artifactsCreated(Collections.emptyList()).build())
            .keyInsights(InsightsSection.builder().description("Test insights")
                .keyPoints(Collections.emptyList()).attachmentsReferenced(Collections.emptyList())
                .artifactsCreated(Collections.emptyList()).build())
            .followUpExplorations(FollowUpSection.builder().description("Test follow-up")
                .attachmentsReferenced(Collections.emptyList())
                .artifactsCreated(Collections.emptyList()).build())
            .references(Collections.emptyList()).build())
        .workarounds(Collections.emptyList()).build();

    // Clear test data (no artifacts/attachments for minimal test)
    testArtifacts.clear();
    testAttachments.clear();

    // Act
    String generatedMarkdown = generator.generateMarkdown(minimalNote);

    // Assert - Should not throw exceptions and should produce valid markdown
    assertNotNull(generatedMarkdown);
    assertTrue(generatedMarkdown.contains("# Test Title"));
    assertTrue(generatedMarkdown.contains("## Initial Query"));
    assertTrue(generatedMarkdown.contains("## Key Insights"));
    assertTrue(generatedMarkdown.contains("## Follow-up Explorations"));
    assertTrue(generatedMarkdown.contains("## Workarounds Used"));
    assertTrue(generatedMarkdown.contains("None - All attachments were successfully converted"));
  }

  @Test
  void testGenerateMarkdown_WithAttachments_ShouldFormatCorrectly() {
    // Arrange - Create ChatNote with attachment count
    Attachment attachment1 = Attachment.builder().chatNoteId("test-note-id").filename("test.md")
        .content("# Test Document\n\nSome content here.").isSummarized(false).build();

    Attachment attachment2 = Attachment.builder().chatNoteId("test-note-id").filename("large.pdf")
        .content("# Summary\n\nSummarized content.").isSummarized(true)
        .originalSize("150 pages / 2.5 MB").summarizationLevel("Partial")
        .contentPreserved("Executive summary, key findings").build();

    // Populate test attachments
    testArtifacts.clear();
    testAttachments.clear();
    testAttachments.addAll(Arrays.asList(attachment1, attachment2));

    ChatNote noteWithAttachments = createBasicChatNote();
    noteWithAttachments.setAttachmentCount(2);

    // Act
    String generatedMarkdown = generator.generateMarkdown(noteWithAttachments);

    // Assert
    assertTrue(generatedMarkdown.contains("## Attachments"));
    assertTrue(generatedMarkdown.contains(":::attachment filename=\"test.md\""));
    assertTrue(generatedMarkdown.contains(":::"));
    assertTrue(generatedMarkdown.contains("# Test Document"));

    // Summarized attachment should have warning
    assertTrue(generatedMarkdown.contains(":::attachment filename=\"large.pdf\""));
    assertTrue(generatedMarkdown.contains("⚠️ NOTE: This attachment was summarized"));
    assertTrue(generatedMarkdown.contains("Original size: 150 pages / 2.5 MB"));
    assertTrue(generatedMarkdown.contains("Summarization level: Partial"));
    assertTrue(generatedMarkdown.contains("Content preserved: Executive summary, key findings"));
  }

  @Test
  void testGenerateMarkdown_WithWorkarounds_ShouldFormatCorrectly() {
    // Arrange
    Workaround workaround1 = Workaround.builder().filename("large_document.pdf")
        .workaround("Summarized to key sections (30% of original)")
        .reason("context length limitations")
        .preserved("executive summary, methodology, key findings, conclusions")
        .lost("detailed appendices").build();

    ChatNote noteWithWorkarounds = createBasicChatNote();
    noteWithWorkarounds.setWorkarounds(Collections.singletonList(workaround1));
    noteWithWorkarounds.setWorkaroundsCount(1);

    // Act
    String generatedMarkdown = generator.generateMarkdown(noteWithWorkarounds);

    // Assert
    assertTrue(generatedMarkdown.contains("## Workarounds Used"));
    assertTrue(generatedMarkdown.contains("**large_document.pdf**:"));
    assertTrue(generatedMarkdown.contains("Summarized to key sections"));
    assertTrue(generatedMarkdown.contains("context length limitations"));
    assertTrue(generatedMarkdown.contains("Preserved: executive summary"));
    assertTrue(generatedMarkdown.contains("Omitted: detailed appendices"));
  }

  @Test
  void testGenerateMarkdown_WithArtifactMissingOptionalFields_ShouldHandleGracefully() {
    // Arrange - Artifact without language and version
    Artifact artifact = Artifact.builder().chatNoteId("test-note-id").type("poem")
        .title("Morning Thoughts").content("Roses are red\nViolets are blue").build();

    // Populate test artifacts
    testArtifacts.clear();
    testAttachments.clear();
    testArtifacts.add(artifact);

    ChatNote noteWithArtifact = createBasicChatNote();
    noteWithArtifact.setArtifactCount(1);

    // Act
    String generatedMarkdown = generator.generateMarkdown(noteWithArtifact);

    // Assert
    assertTrue(generatedMarkdown.contains("type=\"poem\""));
    assertTrue(generatedMarkdown.contains("title=\"Morning Thoughts\""));

    // Extract just the artifact section to check for optional fields
    int artifactStart = generatedMarkdown.indexOf("## Conversation Artifacts");
    int artifactEnd = generatedMarkdown.indexOf("---\n\n##", artifactStart);
    String artifactSection = generatedMarkdown.substring(artifactStart, artifactEnd);

    // The poem artifact should NOT have language or version attributes
    int poemStart = artifactSection.indexOf(":::artifact");
    int poemEnd = artifactSection.indexOf("\n", poemStart);
    String poemMarker = artifactSection.substring(poemStart, poemEnd);

    assertFalse(poemMarker.contains("language="), "Should not include language if null");
    assertFalse(poemMarker.contains("version="), "Should not include version if null");
    assertTrue(generatedMarkdown.contains("Roses are red"));
  }

  @Test
  void testGenerateMarkdown_WithReferenceWithoutDescription_ShouldShowUrlOnly() {
    // Arrange
    Reference ref1 =
        Reference.builder().url("https://example.com").description("Example Site").build();

    Reference ref2 = Reference.builder().url("https://another.com").description(null).build();

    ChatNote noteWithRefs = createBasicChatNote();
    noteWithRefs.getSummary().setReferences(Arrays.asList(ref1, ref2));

    // Act
    String generatedMarkdown = generator.generateMarkdown(noteWithRefs);

    // Assert
    assertTrue(generatedMarkdown.contains("[Example Site](https://example.com)"),
        "Should format reference with description as markdown link");
    assertTrue(generatedMarkdown.contains("- https://another.com"),
        "Should show URL only if description is null");
  }

  @Test
  void testGenerateConversationContent_WithNullChatNote_ShouldThrowException() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> generator.generateConversationContent(null),
        "Should throw exception for null ChatNote");
  }

  @Test
  void testGenerateConversationContent_WithDragonwellChatNote_ShouldGenerateContentWithoutArtifacts() {
    // Act
    String conversationContent = generator.generateConversationContent(dragonwellChatNote);

    // Assert
    assertNotNull(conversationContent, "Conversation content should not be null");
    assertTrue(conversationContent.length() > 500,
        "Conversation content should have substantial content");

    // Verify summary sections are present
    assertTrue(conversationContent.contains("# Building Dragonwell"), "Should contain title");
    assertTrue(conversationContent.contains("**Date:** 2025-10-02"), "Should contain date");
    assertTrue(conversationContent.contains("**Tags:**"), "Should contain tags");
    assertTrue(conversationContent.contains("## Initial Query"), "Should contain Initial Query");
    assertTrue(conversationContent.contains("## Key Insights"), "Should contain Key Insights");
    assertTrue(conversationContent.contains("## Follow-up Explorations"),
        "Should contain Follow-up");
    assertTrue(conversationContent.contains("## References/Links"), "Should contain References");

    // Verify artifacts, attachments, and metadata are NOT present
    assertFalse(conversationContent.contains("ARCHIVE_FORMAT_VERSION"),
        "Should NOT contain YAML frontmatter");
    assertFalse(conversationContent.contains("INSTRUCTIONS_FOR_AI"),
        "Should NOT contain AI instructions");
    assertFalse(conversationContent.contains("## Conversation Artifacts"),
        "Should NOT contain Artifacts section");
    assertFalse(conversationContent.contains("## Attachments"),
        "Should NOT contain Attachments section");
    assertFalse(conversationContent.contains("## Workarounds Used"),
        "Should NOT contain Workarounds section");
    assertFalse(conversationContent.contains("## Archive Metadata"),
        "Should NOT contain Archive Metadata section");
    assertFalse(conversationContent.contains("_End of archived conversation_"),
        "Should NOT contain end marker");
    assertFalse(conversationContent.contains(":::artifact"),
        "Should NOT contain artifact markers");
  }

  @Test
  void testGenerateConversationContent_ShouldIncludeAllSummarySections() {
    // Act
    String conversationContent = generator.generateConversationContent(dragonwellChatNote);

    // Assert - Verify all summary sections are present
    assertTrue(conversationContent.contains("## Initial Query"));
    assertTrue(conversationContent.contains("Java 21 builds"), "Should contain query description");
    assertTrue(conversationContent.contains("## Key Insights"));
    assertTrue(conversationContent.contains("Compact Object Headers"),
        "Should contain insights description");
    assertTrue(conversationContent.contains("**Key points:**"), "Should contain key points");
    assertTrue(conversationContent.contains("## Follow-up Explorations"));
    assertTrue(conversationContent.contains("Metal compiler"),
        "Should contain follow-up description");
    assertTrue(conversationContent.contains("## References/Links"));
    assertTrue(conversationContent.contains("github.com/dragonwell-project"),
        "Should contain references");
  }

  @Test
  void testGenerateConversationContent_ShouldBeMuchShorterThanFullMarkdown() {
    // Act
    String fullMarkdown = generator.generateMarkdown(dragonwellChatNote);
    String conversationContent = generator.generateConversationContent(dragonwellChatNote);

    // Assert - Conversation content should be significantly shorter (no YAML, artifacts,
    // attachments)
    assertTrue(conversationContent.length() < fullMarkdown.length(),
        "Conversation content should be shorter than full markdown");

    // For dragonwell, which has a large artifact (bash script), the difference should be
    // substantial
    double ratio = (double) conversationContent.length() / fullMarkdown.length();
    assertTrue(ratio < 0.5,
        "Conversation content should be less than 50% of full markdown size (was " + (ratio * 100)
            + "%)");
  }

  @Test
  void testGenerateConversationContent_WithMinimalChatNote_ShouldWork() {
    // Arrange - Create minimal ChatNote
    ChatNote minimalNote = createBasicChatNote();

    // Act
    String conversationContent = generator.generateConversationContent(minimalNote);

    // Assert
    assertNotNull(conversationContent);
    assertTrue(conversationContent.contains("# Test Title"));
    assertTrue(conversationContent.contains("## Initial Query"));
    assertTrue(conversationContent.contains("## Key Insights"));
    assertTrue(conversationContent.contains("## Follow-up Explorations"));

    // Should NOT contain archive-specific sections
    assertFalse(conversationContent.contains("ARCHIVE_FORMAT_VERSION"));
    assertFalse(conversationContent.contains("## Conversation Artifacts"));
    assertFalse(conversationContent.contains("## Workarounds Used"));
    assertFalse(conversationContent.contains("## Archive Metadata"));
  }

  @Test
  void testGenerateConversationContent_ShouldPreserveReferencesMarkdownFormat() {
    // Act
    String conversationContent = generator.generateConversationContent(dragonwellChatNote);

    // Assert - References should still be in markdown link format
    assertTrue(conversationContent.contains("[") && conversationContent.contains("]("),
        "References should use markdown link format");
    assertTrue(conversationContent.contains("](https://github.com/dragonwell-project"),
        "Should contain GitHub reference with link format");
  }

  // Helper method to create a basic ChatNote for testing
  private ChatNote createBasicChatNote() {
    return ChatNote.builder().archiveVersion("1.0").archiveType("conversation_summary")
        .createdDate(LocalDate.of(2025, 1, 1)).originalPlatform("Test Platform").attachmentCount(0)
        .artifactCount(0).chatNoteCompleteness(ChatNoteCompleteness.COMPLETE).workaroundsCount(0)
        .totalFileSize("1 KB").title("Test Title").conversationDate(LocalDate.of(2025, 1, 1))
        .tags(Arrays.asList("test", "demo"))
        .summary(ConversationSummary.builder()
            .initialQuery(QuerySection.builder().description("Test query description")
                .attachmentsReferenced(Collections.emptyList())
                .artifactsCreated(Collections.emptyList()).build())
            .keyInsights(InsightsSection.builder().description("Test insights description")
                .keyPoints(Arrays.asList("Key point 1", "Key point 2"))
                .attachmentsReferenced(Collections.emptyList())
                .artifactsCreated(Collections.emptyList()).build())
            .followUpExplorations(
                FollowUpSection.builder().description("Test follow-up description")
                    .attachmentsReferenced(Collections.emptyList())
                    .artifactsCreated(Collections.emptyList()).build())
            .references(Collections.emptyList()).build())
        .workarounds(Collections.emptyList()).build();
  }

  // Helper method to populate test artifacts/attachments from DTO
  private void populateTestDataFromDto(ChatNoteDto dto) {
    testArtifacts.clear();
    testAttachments.clear();

    if (dto.getArtifacts() != null) {
      for (var artifactDto : dto.getArtifacts()) {
        testArtifacts.add(Artifact.builder().chatNoteId("test-note-id").type(artifactDto.getType())
            .title(artifactDto.getTitle()).language(artifactDto.getLanguage())
            .version(artifactDto.getVersion()).iterations(artifactDto.getIterations())
            .evolutionNotes(artifactDto.getEvolutionNotes()).content(artifactDto.getContent())
            .build());
      }
    }

    if (dto.getAttachments() != null) {
      for (var attachmentDto : dto.getAttachments()) {
        testAttachments.add(
            Attachment.builder().chatNoteId("test-note-id").filename(attachmentDto.getFilename())
                .content(attachmentDto.getContent()).isSummarized(attachmentDto.getIsSummarized())
                .originalSize(attachmentDto.getOriginalSize())
                .summarizationLevel(attachmentDto.getSummarizationLevel())
                .contentPreserved(attachmentDto.getContentPreserved())
                .processingLimitation(attachmentDto.getProcessingLimitation()).build());
      }
    }
  }
}
