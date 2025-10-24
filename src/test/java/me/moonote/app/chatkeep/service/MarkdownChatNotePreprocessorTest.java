package me.moonote.app.chatkeep.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.moonote.app.chatkeep.dto.ArtifactDto;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.ChatNoteMetadataDto;
import me.moonote.app.chatkeep.dto.ConversationSummaryDto;
import me.moonote.app.chatkeep.dto.FollowUpSectionDto;
import me.moonote.app.chatkeep.dto.InsightsSectionDto;
import me.moonote.app.chatkeep.dto.QuerySectionDto;
import me.moonote.app.chatkeep.dto.ReferenceDto;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

@SpringBootTest
class MarkdownChatNotePreprocessorTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private MarkdownChatNotePreprocessor preprocessor;
  private String dragonwellMarkdown;

  @BeforeEach
  void setUp() throws IOException {
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);

    // Load the test markdown file
    dragonwellMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/dragonwell.md"));
  }

  @Test
  void testPreprocess_WithDragonwellMarkdown_ShouldReturnSuccessfulValidation() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);

    // Assert
    if (!result.isValid()) {
      System.out.println("\n=== Dragonwell Validation Errors ===");
      result.getErrors().forEach(error -> System.out.println("- " + error));
      System.out.println("====================================\n");
    }

    assertTrue(result.isValid(), "Validation should succeed");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
    assertTrue(result.getErrors().isEmpty(), "Errors should be empty");
  }

  @Test
  void testPreprocess_ShouldParseYamlMetadataCorrectly() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - YAML frontmatter metadata
    assertNotNull(dto.getMetadata(), "Metadata should not be null");
    ChatNoteMetadataDto metadata = dto.getMetadata();

    assertEquals("1.0", metadata.getArchiveVersion(), "Archive version should match");
    assertEquals("conversation_summary", metadata.getArchiveType(), "Archive type should match");
    assertEquals(LocalDate.of(2025, 10, 2), metadata.getCreatedDate(), "Created date should match");
    assertEquals("Claude (Anthropic)", metadata.getOriginalPlatform(),
        "Original platform should match");
    assertEquals(0, metadata.getAttachmentCount(), "Attachment count should be 0");
    assertEquals(1, metadata.getArtifactCount(), "Artifact count should be 1");
    assertEquals("COMPLETE", metadata.getChatNoteCompleteness(),
        "Archive completeness should be COMPLETE");
    assertEquals(0, metadata.getWorkaroundsCount(), "Workarounds count should be 0");
    assertEquals("14 KB", metadata.getTotalFileSize(), "Total file size should match");
  }

  @Test
  void testPreprocess_ShouldExtractTitleFromMarkdown() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Title from main content (first H1)
    assertEquals("Building Dragonwell JDK 21 on macOS with Compact Object Headers",
        dto.getMetadata().getTitle(), "Title should be extracted from first H1");
  }

  @Test
  void testPreprocess_ShouldExtractConversationDate() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Conversation date from **Date:** line
    assertEquals(LocalDate.of(2025, 10, 2), dto.getMetadata().getConversationDate(),
        "Conversation date should be extracted from **Date:** line");
  }

  @Test
  void testPreprocess_ShouldExtractTags() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Tags from **Tags:** line
    assertNotNull(dto.getMetadata().getTags(), "Tags should not be null");
    assertEquals(7, dto.getMetadata().getTags().size(), "Should have 7 tags");
    assertTrue(dto.getMetadata().getTags().contains("java"), "Should contain 'java' tag");
    assertTrue(dto.getMetadata().getTags().contains("dragonwell"),
        "Should contain 'dragonwell' tag");
    assertTrue(dto.getMetadata().getTags().contains("jdk21"), "Should contain 'jdk21' tag");
    assertTrue(dto.getMetadata().getTags().contains("macos"), "Should contain 'macos' tag");
    assertTrue(dto.getMetadata().getTags().contains("compact-object-headers"),
        "Should contain 'compact-object-headers' tag");
    assertTrue(dto.getMetadata().getTags().contains("build-from-source"),
        "Should contain 'build-from-source' tag");
    assertTrue(dto.getMetadata().getTags().contains("jep-519"), "Should contain 'jep-519' tag");
  }

  @Test
  void testPreprocess_ShouldParseInitialQuerySection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Initial Query section
    assertNotNull(dto.getSummary(), "Summary should not be null");
    ConversationSummaryDto summary = dto.getSummary();
    assertNotNull(summary.getInitialQuery(), "Initial query should not be null");

    QuerySectionDto initialQuery = summary.getInitialQuery();
    assertNotNull(initialQuery.getDescription(), "Query description should not be null");
    assertTrue(initialQuery.getDescription().contains("Java 21 builds"),
        "Description should contain expected content");
    assertTrue(initialQuery.getDescription().contains("Compact Object Headers"),
        "Description should mention Compact Object Headers");

    // Check artifacts referenced
    assertNotNull(initialQuery.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, initialQuery.getArtifactsCreated().size(), "Should have 1 artifact referenced");
    assertEquals("build-osx.sh (final build script)", initialQuery.getArtifactsCreated().get(0),
        "Artifact reference should match");

    // No attachments in this section
    assertNotNull(initialQuery.getAttachmentsReferenced(),
        "Attachments referenced should not be null");
    assertTrue(initialQuery.getAttachmentsReferenced().isEmpty(),
        "Should have no attachments referenced");
  }

  @Test
  void testPreprocess_ShouldParseKeyInsightsSection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Key Insights section
    ConversationSummaryDto summary = dto.getSummary();
    assertNotNull(summary.getKeyInsights(), "Key insights should not be null");

    InsightsSectionDto keyInsights = summary.getKeyInsights();
    assertNotNull(keyInsights.getDescription(), "Insights description should not be null");
    assertTrue(keyInsights.getDescription().contains("Compact Object Headers"),
        "Description should mention Compact Object Headers");

    // Check key points extracted from bullet points
    assertNotNull(keyInsights.getKeyPoints(), "Key points should not be null");
    assertTrue(keyInsights.getKeyPoints().size() > 0, "Should have key points extracted");

    // Check artifacts referenced
    assertNotNull(keyInsights.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, keyInsights.getArtifactsCreated().size(), "Should have 1 artifact referenced");
    assertEquals("build-osx.sh", keyInsights.getArtifactsCreated().get(0),
        "Artifact reference should match");
  }

  @Test
  void testPreprocess_ShouldParseFollowUpExplorationsSection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Follow-up Explorations section
    ConversationSummaryDto summary = dto.getSummary();
    assertNotNull(summary.getFollowUpExplorations(), "Follow-up explorations should not be null");

    FollowUpSectionDto followUp = summary.getFollowUpExplorations();
    assertNotNull(followUp.getDescription(), "Follow-up description should not be null");
    assertTrue(followUp.getDescription().contains("Metal compiler"),
        "Description should mention Metal compiler issues");
    assertTrue(followUp.getDescription().contains("VLA"), "Description should mention VLA errors");

    // Check artifacts referenced
    assertNotNull(followUp.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, followUp.getArtifactsCreated().size(), "Should have 1 artifact referenced");
    assertTrue(followUp.getArtifactsCreated().get(0).contains("build-osx.sh"),
        "Artifact reference should contain build-osx.sh");
  }

  @Test
  void testPreprocess_ShouldParseReferences() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - References section
    ConversationSummaryDto summary = dto.getSummary();
    assertNotNull(summary.getReferences(), "References should not be null");
    assertTrue(summary.getReferences().size() > 0, "Should have references");

    // Check for specific references
    boolean foundDragonwellRepo = summary.getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("github.com/dragonwell-project"));
    assertTrue(foundDragonwellRepo, "Should contain Dragonwell GitHub repository reference");

    boolean foundJEP519 = summary.getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("openjdk.org/jeps/519"));
    assertTrue(foundJEP519, "Should contain JEP 519 reference");

    // Verify reference structure
    ReferenceDto firstRef = summary.getReferences().get(0);
    assertNotNull(firstRef.getDescription(), "Reference description should not be null");
    assertNotNull(firstRef.getUrl(), "Reference URL should not be null");
    assertTrue(firstRef.getUrl().startsWith("http"), "URL should start with http");
  }

  @Test
  void testPreprocess_ShouldExtractArtifacts() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Artifacts
    assertNotNull(dto.getArtifacts(), "Artifacts list should not be null");
    assertEquals(1, dto.getArtifacts().size(), "Should have exactly 1 artifact");

    ArtifactDto artifact = dto.getArtifacts().get(0);
    assertEquals("script", artifact.getType(), "Artifact type should be 'script'");
    assertEquals("bash", artifact.getLanguage(), "Artifact language should be 'bash'");
    assertEquals("Dragonwell macOS Build Script", artifact.getTitle(),
        "Artifact title should match");
    assertEquals("final", artifact.getVersion(), "Artifact version should be 'final'");

    // Check artifact content
    assertNotNull(artifact.getContent(), "Artifact content should not be null");
    assertTrue(artifact.getContent().startsWith("#!/bin/bash"), "Script should start with shebang");
    assertTrue(artifact.getContent().contains("bash configure"),
        "Script should contain configure command");
    assertTrue(artifact.getContent().contains("make images"),
        "Script should contain make images command");
    assertTrue(artifact.getContent().contains("--disable-warnings-as-errors"),
        "Script should contain expected configuration flag");
  }

  @Test
  void testPreprocess_ShouldHandleNoAttachments() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - No attachments in dragonwell.md
    assertNotNull(dto.getAttachments(), "Attachments list should not be null");
    assertTrue(dto.getAttachments().isEmpty(), "Should have no attachments");
  }

  @Test
  void testPreprocess_ShouldHandleNoWorkarounds() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - No workarounds in dragonwell.md (section says "None")
    assertNotNull(dto.getWorkarounds(), "Workarounds list should not be null");
    assertTrue(dto.getWorkarounds().isEmpty(), "Should have no workarounds");
  }

  @Test
  void testPreprocess_WithInvalidMarkdown_ShouldReturnFailure() {
    // Arrange - markdown without YAML frontmatter
    String invalidMarkdown = "# Just a title\nNo YAML frontmatter here";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(invalidMarkdown);

    // Assert
    assertFalse(result.isValid(), "Validation should fail");
    assertNull(result.getChatNoteDto(), "ChatNoteDto should be null on failure");
    assertFalse(result.getErrors().isEmpty(), "Should have validation errors");
  }

  @Test
  void testPreprocess_WithEmptyString_ShouldReturnFailure() {
    // Arrange
    String emptyMarkdown = "";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(emptyMarkdown);

    // Assert
    assertFalse(result.isValid(), "Validation should fail");
    assertFalse(result.getErrors().isEmpty(), "Should have validation errors");
  }

  @Test
  void testPreprocess_ShouldHandleArtifactContentCorrectly() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Verify artifact content is cleaned (no evolution notes mixed in)
    ArtifactDto artifact = dto.getArtifacts().get(0);
    String content = artifact.getContent();

    // Content should start with actual script (shebang should be preserved)
    assertTrue(content.startsWith("#!/bin/bash"), "Content should start with shebang");

    // Verify key script sections are present
    assertTrue(content.contains("Step 1:"), "Script should contain step markers");
    assertTrue(content.contains("Step 10:"), "Script should contain all 10 steps");
    assertTrue(content.contains("Build Summary"), "Script should contain summary section");
  }

  @Test
  void testPreprocess_ShouldValidateAgainstJsonSchema() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);

    // Assert - The preprocessing includes JSON schema validation
    assertTrue(result.isValid(), "Should pass JSON schema validation");

    // The ChatNoteDto structure should conform to the schema requirements
    ChatNoteDto dto = result.getChatNoteDto();
    assertNotNull(dto.getMetadata(), "Metadata is required by schema");
    assertNotNull(dto.getMetadata().getArchiveVersion(), "Archive version is required");
    assertNotNull(dto.getMetadata().getTitle(), "Title is required");
  }

  @Test
  void testPreprocess_ShouldStripCodeBlockWrappers_WithPlainBackticks() {
    // Arrange - wrap the markdown in plain code block
    String wrappedMarkdown = "```\n" + dragonwellMarkdown + "\n```";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(wrappedMarkdown);

    // Assert - should successfully parse the unwrapped content
    assertTrue(result.isValid(), "Validation should succeed after stripping code block wrappers");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
    assertEquals("Building Dragonwell JDK 21 on macOS with Compact Object Headers",
        result.getChatNoteDto().getMetadata().getTitle(), "Should extract title correctly");
  }

  @Test
  void testPreprocess_ShouldStripCodeBlockWrappers_WithMarkdownLanguage() {
    // Arrange - wrap the markdown with language identifier
    String wrappedMarkdown = "```markdown\n" + dragonwellMarkdown + "\n```";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(wrappedMarkdown);

    // Assert - should successfully parse the unwrapped content
    assertTrue(result.isValid(), "Validation should succeed after stripping code block wrappers");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
    assertEquals("1.0", result.getChatNoteDto().getMetadata().getArchiveVersion(),
        "Should parse metadata correctly");
  }

  @Test
  void testPreprocess_ShouldStripCodeBlockWrappers_WithQuadrupleBackticks() {
    // Arrange - wrap with quadruple backticks
    String wrappedMarkdown = "````\n" + dragonwellMarkdown + "\n````";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(wrappedMarkdown);

    // Assert - should successfully parse the unwrapped content
    assertTrue(result.isValid(), "Validation should succeed after stripping code block wrappers");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
    assertEquals(1, result.getChatNoteDto().getArtifacts().size(),
        "Should extract artifacts correctly");
  }

  @Test
  void testPreprocess_ShouldNotModify_WhenNoCodeBlockWrappers() {
    // Act - process original markdown without wrappers
    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);

    // Assert - should work as before
    assertTrue(result.isValid(), "Validation should succeed");
    assertEquals("Building Dragonwell JDK 21 on macOS with Compact Object Headers",
        result.getChatNoteDto().getMetadata().getTitle(), "Should extract title correctly");
  }

  @Test
  void testPreprocess_ShouldNotModify_WhenOnlyOpeningFence() {
    // Arrange - markdown with only opening fence (incomplete code block)
    String partialMarkdown = "```\n" + dragonwellMarkdown;

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(partialMarkdown);

    // Assert - should still parse (fence is treated as part of content)
    // The YAML parser will handle this appropriately
    assertNotNull(result, "Result should not be null");
  }

  @Test
  void testPreprocess_ShouldStripCodeBlockWrappers_WithWhitespace() {
    // Arrange - wrap with extra whitespace
    String wrappedMarkdown = "  ```markdown  \n" + dragonwellMarkdown + "\n```  ";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(wrappedMarkdown);

    // Assert - should handle whitespace gracefully
    assertTrue(result.isValid(), "Validation should succeed with whitespace around fences");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
  }
}
