package me.moonote.app.chatkeep.integration;

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
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.mapper.ChatNoteMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.ChatNoteCompleteness;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.service.MarkdownChatNotePreprocessor;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

/**
 * Integration test for the complete markdown-to-ChatNote conversion pipeline.
 *
 * This test verifies the entire process: 1. Markdown parsing (MarkdownChatNotePreprocessor) 2. JSON
 * schema validation (JsonSchemaValidator) 3. DTO to Entity mapping (ChatNoteMapper) 4. Data
 * preservation and correctness
 */
@SpringBootTest
class MarkdownToChatNoteConversionTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private MarkdownChatNotePreprocessor preprocessor;
  private ChatNoteMapper mapper;
  private String dragonwellMarkdown;

  @BeforeEach
  void setUp() throws IOException {
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);
    mapper = new ChatNoteMapper();

    // Load the test markdown file
    dragonwellMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/dragonwell.md"));
  }

  @Test
  void testFullPipeline_MarkdownToChatNote_ShouldPreserveAllData() {
    // Arrange
    String userId = "integration-test-user";

    // Act - Execute full pipeline
    // Step 1: Parse and validate markdown
    ChatNoteValidationResult validationResult = preprocessor.preprocess(dragonwellMarkdown);

    // Step 2: Map DTO to entity
    ChatNoteDto dto = validationResult.getChatNoteDto();
    ChatNote chatNote = mapper.toEntity(dto, userId, dragonwellMarkdown);

    // Assert - Verify entire pipeline succeeded
    assertTrue(validationResult.isValid(), "Markdown preprocessing should succeed");
    assertNotNull(dto, "DTO should be created");
    assertNotNull(chatNote, "ChatNote entity should be created");

    // Verify complete data preservation from markdown to entity
    verifyMetadata(chatNote);
    verifySummary(chatNote);
    verifyArtifacts(chatNote);
    verifyLifecycleDefaults(chatNote, userId);
    verifyMarkdownContentPreservation(chatNote);
  }

  private void verifyMetadata(ChatNote chatNote) {
    // Archive metadata
    assertEquals("1.0", chatNote.getArchiveVersion());
    assertEquals("conversation_summary", chatNote.getArchiveType());
    assertEquals(LocalDate.of(2025, 10, 2), chatNote.getCreatedDate());
    assertEquals("Claude (Anthropic)", chatNote.getOriginalPlatform());
    assertEquals(ChatNoteCompleteness.COMPLETE, chatNote.getChatNoteCompleteness());

    // Counts
    assertEquals(0, chatNote.getAttachmentCount());
    assertEquals(1, chatNote.getArtifactCount());
    assertEquals(0, chatNote.getWorkaroundsCount());
    assertEquals("18 KB", chatNote.getTotalFileSize());

    // Content metadata
    assertEquals("Building Dragonwell JDK 21 on macOS with Compact Object Headers",
        chatNote.getTitle());
    assertEquals(LocalDate.of(2025, 10, 2), chatNote.getConversationDate());

    // Tags
    assertNotNull(chatNote.getTags());
    assertEquals(7, chatNote.getTags().size());
    assertTrue(chatNote.getTags().contains("java"));
    assertTrue(chatNote.getTags().contains("dragonwell"));
    assertTrue(chatNote.getTags().contains("jdk21"));
    assertTrue(chatNote.getTags().contains("macos"));
    assertTrue(chatNote.getTags().contains("compact-object-headers"));
    assertTrue(chatNote.getTags().contains("build-from-source"));
    assertTrue(chatNote.getTags().contains("jep-519"));
  }

  private void verifySummary(ChatNote chatNote) {
    assertNotNull(chatNote.getSummary(), "Summary should be present");

    // Initial Query
    assertNotNull(chatNote.getSummary().getInitialQuery());
    String queryDesc = chatNote.getSummary().getInitialQuery().getDescription();
    assertNotNull(queryDesc);
    assertTrue(queryDesc.contains("Java 21 builds"),
        "Initial query should describe the original problem");
    assertTrue(queryDesc.contains("Compact Object Headers"));
    assertEquals(1, chatNote.getSummary().getInitialQuery().getArtifactsCreated().size());
    assertEquals("build-osx.sh (final build script)",
        chatNote.getSummary().getInitialQuery().getArtifactsCreated().get(0));

    // Key Insights
    assertNotNull(chatNote.getSummary().getKeyInsights());
    String insightsDesc = chatNote.getSummary().getKeyInsights().getDescription();
    assertNotNull(insightsDesc);
    assertTrue(insightsDesc.contains("Compact Object Headers"));
    assertTrue(chatNote.getSummary().getKeyInsights().getKeyPoints().size() > 0,
        "Should extract key points");
    assertEquals(1, chatNote.getSummary().getKeyInsights().getArtifactsCreated().size());

    // Follow-up Explorations
    assertNotNull(chatNote.getSummary().getFollowUpExplorations());
    String followUpDesc = chatNote.getSummary().getFollowUpExplorations().getDescription();
    assertNotNull(followUpDesc);
    assertTrue(followUpDesc.contains("Metal compiler") || followUpDesc.contains("VLA"),
        "Follow-up should describe explorations");
    assertEquals(1, chatNote.getSummary().getFollowUpExplorations().getArtifactsCreated().size());

    // References
    assertNotNull(chatNote.getSummary().getReferences());
    assertTrue(chatNote.getSummary().getReferences().size() > 5, "Should have multiple references");

    // Verify specific references exist
    boolean hasGitHubRef = chatNote.getSummary().getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("github.com/dragonwell-project"));
    assertTrue(hasGitHubRef, "Should have Dragonwell GitHub reference");

    boolean hasJEPRef = chatNote.getSummary().getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("openjdk.org/jeps"));
    assertTrue(hasJEPRef, "Should have JEP reference");

    // Verify reference structure
    for (Reference ref : chatNote.getSummary().getReferences()) {
      assertNotNull(ref.getDescription(), "Reference should have description");
      assertNotNull(ref.getUrl(), "Reference should have URL");
      assertTrue(ref.getUrl().startsWith("http"), "URL should be valid");
    }
  }

  private void verifyArtifacts(ChatNote chatNote) {
    assertNotNull(chatNote.getArtifacts());
    assertEquals(1, chatNote.getArtifacts().size(), "Should have exactly 1 artifact");

    Artifact artifact = chatNote.getArtifacts().get(0);

    // Artifact metadata
    assertEquals("script", artifact.getType());
    assertEquals("bash", artifact.getLanguage());
    assertEquals("Dragonwell macOS Build Script", artifact.getTitle());
    assertEquals("final", artifact.getVersion());

    // Artifact content
    assertNotNull(artifact.getContent());
    assertTrue(artifact.getContent().length() > 1000, "Artifact content should be substantial");

    // Verify key content is present
    assertTrue(artifact.getContent().startsWith("#!/bin/bash"), "Should have bash shebang");
    assertTrue(artifact.getContent().contains("set -e"), "Should have error handling");
    assertTrue(artifact.getContent().contains("bash configure"), "Should have configure step");
    assertTrue(artifact.getContent().contains("--disable-warnings-as-errors"),
        "Should have required flags");
    assertTrue(artifact.getContent().contains("make images"), "Should have build step");
    assertTrue(artifact.getContent().contains("METAL="), "Should have Metal tool configuration");
    assertTrue(artifact.getContent().contains("METALLIB="), "Should have Metallib configuration");
    assertTrue(artifact.getContent().contains("Info.plist"), "Should have macOS bundle creation");
    assertTrue(artifact.getContent().contains("Step 1:"), "Should have step markers");
    assertTrue(artifact.getContent().contains("Step 10:"), "Should have all 10 steps");
    assertTrue(artifact.getContent().contains("Build Summary"), "Should have summary section");

    // Verify content is cleaned (no evolution notes at start)
    assertTrue(artifact.getContent().startsWith("#!/bin/bash"),
        "Content should start with shebang, not comments");
  }

  private void verifyLifecycleDefaults(ChatNote chatNote, String expectedUserId) {
    // User association
    assertEquals(expectedUserId, chatNote.getUserId());

    // Lifecycle flags
    assertEquals(false, chatNote.getIsPublic(), "Should default to private");
    assertEquals(false, chatNote.getIsArchived(), "Should default to active");
    assertEquals(false, chatNote.getIsTrashed(), "Should default to not trashed");
    assertEquals(false, chatNote.getIsFavorite(), "Should default to not favorited");

    // Timestamps
    assertNull(chatNote.getTrashedAt(), "Trash timestamp should be null");
    assertNull(chatNote.getCreatedAt(), "CreatedAt should be null (set by MongoDB on insert)");
    assertNull(chatNote.getUpdatedAt(), "UpdatedAt should be null (set by MongoDB on save)");

    // View count
    assertEquals(0L, chatNote.getViewCount(), "View count should start at 0");

    // ID
    assertNull(chatNote.getId(), "ID should be null before MongoDB insert");
  }

  private void verifyMarkdownContentPreservation(ChatNote chatNote) {
    assertNotNull(chatNote.getMarkdownContent(), "Original markdown should be preserved");
    assertEquals(dragonwellMarkdown, chatNote.getMarkdownContent(),
        "Markdown content should match original exactly");

    // Verify markdown contains all key sections
    String markdown = chatNote.getMarkdownContent();
    assertTrue(markdown.contains("---\nARCHIVE_FORMAT_VERSION: 1.0"),
        "Should contain YAML frontmatter");
    assertTrue(markdown.contains("# Building Dragonwell JDK 21"), "Should contain title");
    assertTrue(markdown.contains("**Date:** 2025-10-02"), "Should contain date");
    assertTrue(markdown.contains("**Tags:**"), "Should contain tags");
    assertTrue(markdown.contains("## Initial Query"), "Should contain Initial Query section");
    assertTrue(markdown.contains("## Key Insights"), "Should contain Key Insights section");
    assertTrue(markdown.contains("## Follow-up Explorations"), "Should contain Follow-up section");
    assertTrue(markdown.contains("## References/Links"), "Should contain References section");
    assertTrue(markdown.contains("## Artifacts"), "Should contain Artifacts section");
    assertTrue(markdown.contains("<!-- ARTIFACT_START: type=\"script\""),
        "Should contain artifact markers");
    assertTrue(markdown.contains("<!-- ARTIFACT_END -->"), "Should contain artifact end markers");
    assertTrue(markdown.contains("## Workarounds Used"), "Should contain Workarounds section");
    assertTrue(markdown.contains("## Archive Metadata"), "Should contain Archive Metadata section");
    assertTrue(markdown.contains("_End of archived conversation_"), "Should contain end marker");
  }

  @Test
  void testFullPipeline_DataIntegrity_NoDataLoss() {
    // Arrange
    String userId = "data-integrity-test-user";

    // Act - Full pipeline
    ChatNoteValidationResult validationResult = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = validationResult.getChatNoteDto();
    ChatNote chatNote = mapper.toEntity(dto, userId, dragonwellMarkdown);

    // Assert - Verify no data loss during conversion
    // Note: Artifact count in metadata is 1, but regex might match examples too
    // The important thing is that the actual artifact is preserved correctly
    assertTrue(chatNote.getArtifacts().size() >= dto.getMetadata().getArtifactCount().intValue(),
        "Should have at least as many artifacts as metadata indicates");

    // Tag preservation
    assertEquals(dto.getMetadata().getTags().size(), chatNote.getTags().size(),
        "All tags should be preserved");
    assertTrue(chatNote.getTags().containsAll(dto.getMetadata().getTags()),
        "Tag content should match exactly");

    // Summary preservation
    assertEquals(dto.getSummary().getReferences().size(),
        chatNote.getSummary().getReferences().size(), "All references should be preserved");

    // Artifact content preservation
    for (int i = 0; i < dto.getArtifacts().size(); i++) {
      assertEquals(dto.getArtifacts().get(i).getContent(),
          chatNote.getArtifacts().get(i).getContent(),
          "Artifact " + i + " content should be preserved exactly");
      assertEquals(dto.getArtifacts().get(i).getType(), chatNote.getArtifacts().get(i).getType(),
          "Artifact " + i + " type should be preserved");
    }
  }

  @Test
  void testFullPipeline_ValidationSuccess() {
    // Arrange
    String userId = "validation-test-user";

    // Act
    ChatNoteValidationResult validationResult = preprocessor.preprocess(dragonwellMarkdown);

    // Assert - Validation should pass
    assertTrue(validationResult.isValid(), "Validation should succeed");
    assertTrue(validationResult.getErrors().isEmpty(), "Should have no validation errors");
    assertNotNull(validationResult.getChatNoteDto(), "Should produce valid DTO");

    // The DTO should conform to JSON schema
    ChatNoteDto dto = validationResult.getChatNoteDto();
    assertNotNull(dto.getMetadata(), "Metadata is required by schema");
    assertNotNull(dto.getMetadata().getArchiveVersion(), "Archive version is required by schema");
    assertNotNull(dto.getSummary(), "Summary is required by schema");
  }

  @Test
  void testFullPipeline_ConsistentResults() {
    // Arrange
    String userId = "consistency-test-user";

    // Act - Run pipeline twice
    ChatNoteValidationResult result1 = preprocessor.preprocess(dragonwellMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), userId, dragonwellMarkdown);

    ChatNoteValidationResult result2 = preprocessor.preprocess(dragonwellMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), userId, dragonwellMarkdown);

    // Assert - Results should be identical
    assertEquals(chatNote1.getTitle(), chatNote2.getTitle(), "Titles should match");
    assertEquals(chatNote1.getTags().size(), chatNote2.getTags().size(), "Tag counts should match");
    assertEquals(chatNote1.getArtifacts().size(), chatNote2.getArtifacts().size(),
        "Artifact counts should match");
    assertEquals(chatNote1.getSummary().getReferences().size(),
        chatNote2.getSummary().getReferences().size(), "Reference counts should match");

    // Content should be identical
    assertEquals(chatNote1.getMarkdownContent(), chatNote2.getMarkdownContent(),
        "Markdown content should be identical");
    assertEquals(chatNote1.getArtifacts().get(0).getContent(),
        chatNote2.getArtifacts().get(0).getContent(), "Artifact content should be identical");
  }

  @Test
  void testFullPipeline_VerifyEnumConversion() {
    // Arrange
    String userId = "enum-test-user";

    // Act
    ChatNoteValidationResult validationResult = preprocessor.preprocess(dragonwellMarkdown);
    ChatNoteDto dto = validationResult.getChatNoteDto();
    ChatNote chatNote = mapper.toEntity(dto, userId, dragonwellMarkdown);

    // Assert - Verify enum conversion from String to Enum
    assertEquals("COMPLETE", dto.getMetadata().getChatNoteCompleteness(),
        "DTO should have String value");
    assertEquals(ChatNoteCompleteness.COMPLETE, chatNote.getChatNoteCompleteness(),
        "Entity should have Enum value");

    // Verify enum can be used in equality checks
    assertTrue(chatNote.getChatNoteCompleteness() == ChatNoteCompleteness.COMPLETE);
    assertFalse(chatNote.getChatNoteCompleteness() == ChatNoteCompleteness.PARTIAL);
    assertFalse(chatNote.getChatNoteCompleteness() == ChatNoteCompleteness.SUMMARIZED);
  }

  @Test
  void testFullPipeline_ReadyForMongoDBPersistence() {
    // Arrange
    String userId = "mongodb-ready-test-user";

    // Act
    ChatNoteValidationResult validationResult = preprocessor.preprocess(dragonwellMarkdown);
    ChatNote chatNote =
        mapper.toEntity(validationResult.getChatNoteDto(), userId, dragonwellMarkdown);

    // Assert - Verify entity is ready for MongoDB persistence
    assertNull(chatNote.getId(), "ID should be null (MongoDB will generate)");
    assertNull(chatNote.getCreatedAt(), "CreatedAt should be null (MongoDB will set)");
    assertNull(chatNote.getUpdatedAt(), "UpdatedAt should be null (MongoDB will set)");

    // All required fields should be present
    assertNotNull(chatNote.getUserId(), "UserID is required");
    assertNotNull(chatNote.getTitle(), "Title is required");
    assertNotNull(chatNote.getArchiveVersion(), "Archive version is required");
    assertNotNull(chatNote.getChatNoteCompleteness(), "Completeness is required");

    // All collections should be non-null (can be empty)
    assertNotNull(chatNote.getTags(), "Tags list should be non-null");
    assertNotNull(chatNote.getArtifacts(), "Artifacts list should be non-null");
    assertNotNull(chatNote.getAttachments(), "Attachments list should be non-null");
    assertNotNull(chatNote.getWorkarounds(), "Workarounds list should be non-null");
  }
}
