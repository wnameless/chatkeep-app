package me.moonote.app.chatkeep.mapper;

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
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.ChatNoteCompleteness;
import me.moonote.app.chatkeep.model.ConversationSummary;
import me.moonote.app.chatkeep.model.FollowUpSection;
import me.moonote.app.chatkeep.model.InsightsSection;
import me.moonote.app.chatkeep.model.QuerySection;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.service.MarkdownChatNotePreprocessor;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

@SpringBootTest
class ChatNoteMapperTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private ChatNoteMapper mapper;
  private MarkdownChatNotePreprocessor preprocessor;
  private String dragonwellMarkdown;
  private ChatNoteDto chatNoteDto;

  @BeforeEach
  void setUp() throws IOException {
    mapper = new ChatNoteMapper();
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);

    // Load and preprocess the test markdown file
    dragonwellMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/dragonwell.md"));

    ChatNoteValidationResult result = preprocessor.preprocess(dragonwellMarkdown);
    chatNoteDto = result.getChatNoteDto();
  }

  @Test
  void testToEntity_ShouldMapMetadataFieldsCorrectly() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Metadata from YAML frontmatter
    assertEquals("1.0", chatNote.getArchiveVersion(), "Archive version should match");
    assertEquals("conversation_summary", chatNote.getArchiveType(), "Archive type should match");
    assertEquals(LocalDate.of(2025, 10, 2), chatNote.getCreatedDate(), "Created date should match");
    assertEquals("Claude (Anthropic)", chatNote.getOriginalPlatform(),
        "Original platform should match");
    assertEquals(0, chatNote.getAttachmentCount(), "Attachment count should match");
    assertEquals(1, chatNote.getArtifactCount(), "Artifact count should match");
    assertEquals(ChatNoteCompleteness.COMPLETE, chatNote.getChatNoteCompleteness(),
        "Archive completeness should be COMPLETE");
    assertEquals(0, chatNote.getWorkaroundsCount(), "Workarounds count should match");
    assertEquals("18 KB", chatNote.getTotalFileSize(), "Total file size should match");
  }

  @Test
  void testToEntity_ShouldMapTitleAndConversationDate() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertEquals("Building Dragonwell JDK 21 on macOS with Compact Object Headers",
        chatNote.getTitle(), "Title should be extracted from markdown");
    assertEquals(LocalDate.of(2025, 10, 2), chatNote.getConversationDate(),
        "Conversation date should match");
  }

  @Test
  void testToEntity_ShouldMapTags() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertNotNull(chatNote.getTags(), "Tags should not be null");
    assertEquals(7, chatNote.getTags().size(), "Should have 7 tags");
    assertTrue(chatNote.getTags().contains("java"), "Should contain 'java' tag");
    assertTrue(chatNote.getTags().contains("dragonwell"), "Should contain 'dragonwell' tag");
    assertTrue(chatNote.getTags().contains("compact-object-headers"),
        "Should contain 'compact-object-headers' tag");
  }

  @Test
  void testToEntity_ShouldMapConversationSummary() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertNotNull(chatNote.getSummary(), "Summary should not be null");
    ConversationSummary summary = chatNote.getSummary();

    // Check all summary sections are mapped
    assertNotNull(summary.getInitialQuery(), "Initial query should not be null");
    assertNotNull(summary.getKeyInsights(), "Key insights should not be null");
    assertNotNull(summary.getFollowUpExplorations(), "Follow-up explorations should not be null");
    assertNotNull(summary.getReferences(), "References should not be null");
  }

  @Test
  void testToEntity_ShouldMapInitialQuerySection() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    QuerySection initialQuery = chatNote.getSummary().getInitialQuery();
    assertNotNull(initialQuery.getDescription(), "Query description should not be null");
    assertTrue(initialQuery.getDescription().contains("Java 21 builds"),
        "Description should contain expected content");

    // Check artifacts and attachments lists
    assertNotNull(initialQuery.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, initialQuery.getArtifactsCreated().size(), "Should have 1 artifact referenced");

    assertNotNull(initialQuery.getAttachmentsReferenced(),
        "Attachments referenced should not be null");
    assertTrue(initialQuery.getAttachmentsReferenced().isEmpty(),
        "Should have no attachments referenced");
  }

  @Test
  void testToEntity_ShouldMapKeyInsightsSection() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    InsightsSection keyInsights = chatNote.getSummary().getKeyInsights();
    assertNotNull(keyInsights.getDescription(), "Insights description should not be null");
    assertTrue(keyInsights.getDescription().contains("Compact Object Headers"),
        "Description should mention Compact Object Headers");

    // Check key points are mapped
    assertNotNull(keyInsights.getKeyPoints(), "Key points should not be null");
    assertTrue(keyInsights.getKeyPoints().size() > 0, "Should have key points");

    // Check artifacts referenced
    assertNotNull(keyInsights.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, keyInsights.getArtifactsCreated().size(), "Should have 1 artifact referenced");
  }

  @Test
  void testToEntity_ShouldMapFollowUpExplorationsSection() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    FollowUpSection followUp = chatNote.getSummary().getFollowUpExplorations();
    assertNotNull(followUp.getDescription(), "Follow-up description should not be null");
    assertTrue(followUp.getDescription().contains("Metal compiler"),
        "Description should mention Metal compiler");

    // Check artifacts referenced
    assertNotNull(followUp.getArtifactsCreated(), "Artifacts created should not be null");
    assertEquals(1, followUp.getArtifactsCreated().size(), "Should have 1 artifact referenced");
  }

  @Test
  void testToEntity_ShouldMapReferences() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertNotNull(chatNote.getSummary().getReferences(), "References should not be null");
    assertTrue(chatNote.getSummary().getReferences().size() > 0, "Should have references");

    // Verify reference structure
    Reference firstRef = chatNote.getSummary().getReferences().get(0);
    assertNotNull(firstRef.getDescription(), "Reference description should not be null");
    assertNotNull(firstRef.getUrl(), "Reference URL should not be null");
    assertTrue(firstRef.getUrl().startsWith("http"), "URL should start with http");
  }

  @Test
  void testToEntity_ShouldMapArtifactCount() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Artifacts are no longer embedded in entity, only count is stored
    assertEquals(1, chatNote.getArtifactCount(), "Should have artifact count of 1");

    // Verify artifact data is available in DTO
    assertNotNull(chatNoteDto.getArtifacts(), "DTO artifacts should not be null");
    assertEquals(1, chatNoteDto.getArtifacts().size(), "DTO should have exactly 1 artifact");

    var artifact = chatNoteDto.getArtifacts().get(0);
    assertEquals("script", artifact.getType(), "Artifact type should match");
    assertEquals("bash", artifact.getLanguage(), "Artifact language should match");
    assertEquals("Dragonwell macOS Build Script", artifact.getTitle(),
        "Artifact title should match");
    assertEquals("final", artifact.getVersion(), "Artifact version should match");

    // Verify content is in DTO
    assertNotNull(artifact.getContent(), "Artifact content should not be null");
    assertTrue(artifact.getContent().startsWith("#!/bin/bash"),
        "Content should start with shebang");
    assertTrue(artifact.getContent().contains("bash configure"),
        "Content should contain configure command");
  }

  @Test
  void testToEntity_ShouldMapAttachmentCount() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - dragonwell.md has no attachments
    // Attachments are no longer embedded in entity, only count is stored
    assertEquals(0, chatNote.getAttachmentCount(), "Should have attachment count of 0");

    // Verify attachment data is available in DTO
    assertNotNull(chatNoteDto.getAttachments(), "DTO attachments list should not be null");
    assertTrue(chatNoteDto.getAttachments().isEmpty(), "DTO should have no attachments");
  }

  @Test
  void testToEntity_ShouldMapWorkarounds() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - dragonwell.md has no workarounds
    assertNotNull(chatNote.getWorkarounds(), "Workarounds list should not be null");
    assertTrue(chatNote.getWorkarounds().isEmpty(), "Should have no workarounds");
  }

  @Test
  void testToEntity_ShouldStoreOriginalMarkdownContent() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertNotNull(chatNote.getMarkdownContent(), "Markdown content should not be null");
    assertEquals(dragonwellMarkdown, chatNote.getMarkdownContent(),
        "Should store original markdown content");

    // Verify markdown content is complete
    assertTrue(chatNote.getMarkdownContent().contains("ARCHIVE_FORMAT_VERSION: 1.0"),
        "Markdown should contain YAML frontmatter");
    assertTrue(chatNote.getMarkdownContent().contains("# Building Dragonwell"),
        "Markdown should contain title");
    assertTrue(chatNote.getMarkdownContent().contains("## Initial Query"),
        "Markdown should contain sections");
    assertTrue(chatNote.getMarkdownContent().contains("<!-- ARTIFACT_START:"),
        "Markdown should contain artifacts");
  }

  @Test
  void testToEntity_ShouldSetUserId() {
    // Arrange
    String userId = "test-user-456";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertEquals(userId, chatNote.getUserId(), "User ID should be set correctly");
  }

  @Test
  void testToEntity_ShouldSetDefaultLifecycleFlags() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Default lifecycle flags
    assertEquals(false, chatNote.getIsPublic(), "Should default to private (isPublic=false)");
    assertEquals(false, chatNote.getIsArchived(), "Should default to active (isArchived=false)");
    assertEquals(false, chatNote.getIsTrashed(), "Should default to not trashed");
    assertEquals(false, chatNote.getIsFavorite(), "Should default to not favorited");
    assertNull(chatNote.getTrashedAt(), "Trash timestamp should be null initially");
  }

  @Test
  void testToEntity_ShouldSetDefaultViewCount() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert
    assertEquals(0L, chatNote.getViewCount(), "View count should default to 0");
  }

  @Test
  void testToEntity_ShouldNotSetAuditFields() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Audit fields should be null (set by MongoDB @CreatedDate/@LastModifiedDate)
    assertNull(chatNote.getCreatedAt(), "CreatedAt should be null (set by MongoDB on insert)");
    assertNull(chatNote.getUpdatedAt(), "UpdatedAt should be null (set by MongoDB on save)");
  }

  @Test
  void testToEntity_ShouldNotSetId() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - ID should be null (set by MongoDB on insert)
    assertNull(chatNote.getId(), "ID should be null (set by MongoDB on insert)");
  }

  @Test
  void testToEntity_VerifyCompleteDataMapping() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Comprehensive verification of all data preservation

    // Metadata
    assertNotNull(chatNote.getArchiveVersion(), "Archive version should be mapped");
    assertNotNull(chatNote.getTitle(), "Title should be mapped");
    assertNotNull(chatNote.getTags(), "Tags should be mapped");

    // Summary
    assertNotNull(chatNote.getSummary(), "Summary should be mapped");
    assertNotNull(chatNote.getSummary().getInitialQuery(), "Initial query should be mapped");
    assertNotNull(chatNote.getSummary().getKeyInsights(), "Key insights should be mapped");
    assertNotNull(chatNote.getSummary().getFollowUpExplorations(), "Follow-up should be mapped");
    assertNotNull(chatNote.getSummary().getReferences(), "References should be mapped");

    // Artifact count (artifacts are in separate collection)
    assertEquals(1, chatNote.getArtifactCount(), "Should have artifact count of 1");

    // Verify artifact data is available in DTO
    assertNotNull(chatNoteDto.getArtifacts(), "DTO artifacts should be mapped");
    assertEquals(1, chatNoteDto.getArtifacts().size(), "DTO should have exactly 1 artifact");

    // Verify artifact data integrity in DTO
    var artifact = chatNoteDto.getArtifacts().get(0);
    assertNotNull(artifact.getType(), "Artifact type should be set");
    assertNotNull(artifact.getTitle(), "Artifact title should be set");
    assertNotNull(artifact.getContent(), "Artifact content should be set");
    assertTrue(artifact.getContent().length() > 100, "Artifact content should be substantial");

    // Original content
    assertNotNull(chatNote.getMarkdownContent(), "Markdown content should be preserved");
    assertEquals(dragonwellMarkdown.length(), chatNote.getMarkdownContent().length(),
        "Markdown content length should match original");
  }

  @Test
  void testToEntity_WithDifferentUserId_ShouldMapCorrectly() {
    // Arrange
    String userId1 = "user-alice";
    String userId2 = "user-bob";

    // Act
    ChatNote chatNote1 = mapper.toEntity(chatNoteDto, userId1, dragonwellMarkdown);
    ChatNote chatNote2 = mapper.toEntity(chatNoteDto, userId2, dragonwellMarkdown);

    // Assert - Same DTO mapped to different users
    assertEquals(userId1, chatNote1.getUserId(), "First user ID should match");
    assertEquals(userId2, chatNote2.getUserId(), "Second user ID should match");

    // Other fields should be identical
    assertEquals(chatNote1.getTitle(), chatNote2.getTitle(), "Titles should be identical");
    assertEquals(chatNote1.getArtifactCount(), chatNote2.getArtifactCount(),
        "Artifact counts should be identical");
  }

  @Test
  void testToEntity_ShouldPreserveAllSectionDescriptions() {
    // Arrange
    String userId = "test-user-123";

    // Act
    ChatNote chatNote = mapper.toEntity(chatNoteDto, userId, dragonwellMarkdown);

    // Assert - Verify all section descriptions are non-empty and meaningful
    String queryDesc = chatNote.getSummary().getInitialQuery().getDescription();
    assertNotNull(queryDesc, "Query description should not be null");
    assertTrue(queryDesc.length() > 50, "Query description should be substantial");

    String insightsDesc = chatNote.getSummary().getKeyInsights().getDescription();
    assertNotNull(insightsDesc, "Insights description should not be null");
    assertTrue(insightsDesc.length() > 50, "Insights description should be substantial");

    String followUpDesc = chatNote.getSummary().getFollowUpExplorations().getDescription();
    assertNotNull(followUpDesc, "Follow-up description should not be null");
    assertTrue(followUpDesc.length() > 50, "Follow-up description should be substantial");
  }
}
