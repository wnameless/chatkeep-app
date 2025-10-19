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
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.dto.ChatNoteMetadataDto;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

/**
 * Test class for verifying parsing of mongodb_newsql.md archive.
 *
 * This test was created to investigate a bug where tags were missing after importing the archive.
 */
@SpringBootTest
class MongoDbNewsqlMarkdownTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private MarkdownChatNotePreprocessor preprocessor;
  private String mongodbNewsqlMarkdown;

  @BeforeEach
  void setUp() throws IOException {
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);

    // Load the test markdown file
    mongodbNewsqlMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/mongodb_newsql.md"));
  }

  @Test
  void testPreprocess_WithMongoDbNewsqlMarkdown_ShouldReturnSuccessfulValidation() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);

    // Assert
    assertTrue(result.isValid(), "Validation should succeed for mongodb_newsql.md");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should not be null");
    assertTrue(result.getErrors().isEmpty(), "Errors should be empty");
  }

  @Test
  void testPreprocess_ShouldParseYamlMetadataCorrectly() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - YAML frontmatter metadata
    assertNotNull(dto.getMetadata(), "Metadata should not be null");
    ChatNoteMetadataDto metadata = dto.getMetadata();

    assertEquals("1.0", metadata.getArchiveVersion(), "Archive version should be 1.0");
    assertEquals("conversation_summary", metadata.getArchiveType(),
        "Archive type should be conversation_summary");
    assertEquals(LocalDate.of(2025, 10, 15), metadata.getCreatedDate(),
        "Created date should be 2025-10-15");
    assertEquals("Claude", metadata.getOriginalPlatform(), "Original platform should be Claude");
    assertEquals(0, metadata.getAttachmentCount(), "Attachment count should be 0");
    assertEquals(0, metadata.getArtifactCount(), "Artifact count should be 0");
    assertEquals("COMPLETE", metadata.getChatNoteCompleteness(),
        "Archive completeness should be COMPLETE");
    assertEquals(0, metadata.getWorkaroundsCount(), "Workarounds count should be 0");
    assertEquals("3 KB", metadata.getTotalFileSize(), "Total file size should be 3 KB");
  }

  @Test
  void testPreprocess_ShouldExtractTitleFromMarkdown() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Title from main content (first H1)
    assertEquals("Database Architecture: SQL, NoSQL, NewSQL, and MongoDB's Position",
        dto.getMetadata().getTitle(),
        "Title should be extracted from first H1 in markdown content");
  }

  @Test
  void testPreprocess_ShouldExtractConversationDate() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Conversation date from **Date:** line
    assertEquals(LocalDate.of(2025, 10, 15), dto.getMetadata().getConversationDate(),
        "Conversation date should be extracted from **Date:** line");
  }

  @Test
  void testPreprocess_ShouldExtractTags_BugReport() {
    // This test documents the bug: Tags in format "**Tags:** tag1, tag2, tag3" are not parsed
    // The current implementation only handles format "**Tags:** [tag1, tag2, tag3]"

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Tags from **Tags:** line (line 41 in mongodb_newsql.md)
    // Expected tags: databases, mongodb, newsql, acid, architecture, scalability
    assertNotNull(dto.getMetadata().getTags(), "Tags should not be null");

    // BUG: This assertion will FAIL because tags are not being parsed
    // The line in the markdown is: **Tags:** databases, mongodb, newsql, acid, architecture,
    // scalability
    // But the parser expects: **Tags:** [databases, mongodb, newsql, acid, architecture,
    // scalability]
    assertEquals(6, dto.getMetadata().getTags().size(),
        "Should have 6 tags (BUG: currently returns 0)");

    assertTrue(dto.getMetadata().getTags().contains("databases"), "Should contain 'databases' tag");
    assertTrue(dto.getMetadata().getTags().contains("mongodb"), "Should contain 'mongodb' tag");
    assertTrue(dto.getMetadata().getTags().contains("newsql"), "Should contain 'newsql' tag");
    assertTrue(dto.getMetadata().getTags().contains("acid"), "Should contain 'acid' tag");
    assertTrue(dto.getMetadata().getTags().contains("architecture"),
        "Should contain 'architecture' tag");
    assertTrue(dto.getMetadata().getTags().contains("scalability"),
        "Should contain 'scalability' tag");
  }

  @Test
  void testPreprocess_ShouldParseInitialQuerySection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Initial Query section
    assertNotNull(dto.getSummary(), "Summary should not be null");
    assertNotNull(dto.getSummary().getInitialQuery(), "Initial query should not be null");

    String description = dto.getSummary().getInitialQuery().getDescription();
    assertNotNull(description, "Query description should not be null");
    assertTrue(description.contains("SQL, NoSQL, and NewSQL"),
        "Description should mention database categories");
    assertTrue(description.contains("fundamental differences"),
        "Description should mention fundamental differences");
  }

  @Test
  void testPreprocess_ShouldParseKeyInsightsSection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Key Insights section
    assertNotNull(dto.getSummary().getKeyInsights(), "Key insights should not be null");

    String description = dto.getSummary().getKeyInsights().getDescription();
    assertNotNull(description, "Insights description should not be null");
    assertTrue(description.contains("Database Category Definitions"),
        "Description should contain category definitions");
    assertTrue(description.contains("MongoDB's NewSQL-like Characteristics"),
        "Description should mention MongoDB's NewSQL characteristics");
    assertTrue(description.contains("ACID transactions"),
        "Description should mention ACID transactions");
  }

  @Test
  void testPreprocess_ShouldParseFollowUpExplorationsSection() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - Follow-up Explorations section
    assertNotNull(dto.getSummary().getFollowUpExplorations(),
        "Follow-up explorations should not be null");

    String description = dto.getSummary().getFollowUpExplorations().getDescription();
    assertNotNull(description, "Follow-up description should not be null");
    assertTrue(description.contains("terminology matters"),
        "Description should mention terminology discussion");
    assertTrue(description.contains("distributed ACID transactions"),
        "Description should mention distributed ACID transactions");
  }

  @Test
  void testPreprocess_ShouldParseReferences() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - References section
    assertNotNull(dto.getSummary().getReferences(), "References should not be null");
    assertEquals(3, dto.getSummary().getReferences().size(),
        "Should have 3 references (MongoDB docs, CAP theorem, versions)");

    // Check for specific references
    boolean foundMongoDbDocs = dto.getSummary().getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("docs.mongodb.com"));
    assertTrue(foundMongoDbDocs, "Should contain MongoDB documentation reference");
  }

  @Test
  void testPreprocess_ShouldHandleNoArtifacts() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - No artifacts in mongodb_newsql.md
    assertNotNull(dto.getArtifacts(), "Artifacts list should not be null");
    assertTrue(dto.getArtifacts().isEmpty(),
        "Should have no artifacts (conversation was discussion-only)");
  }

  @Test
  void testPreprocess_ShouldHandleNoAttachments() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - No attachments in mongodb_newsql.md
    assertNotNull(dto.getAttachments(), "Attachments list should not be null");
    assertTrue(dto.getAttachments().isEmpty(), "Should have no attachments");
  }

  @Test
  void testPreprocess_ShouldHandleNoWorkarounds() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);
    ChatNoteDto dto = result.getChatNoteDto();

    // Assert - No workarounds in mongodb_newsql.md
    assertNotNull(dto.getWorkarounds(), "Workarounds list should not be null");
    assertTrue(dto.getWorkarounds().isEmpty(),
        "Should have no workarounds (no attachments were provided)");
  }

  @Test
  void testPreprocess_ShouldValidateAgainstJsonSchema() {
    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(mongodbNewsqlMarkdown);

    // Assert - The preprocessing includes JSON schema validation
    assertTrue(result.isValid(),
        "Should pass JSON schema validation for mongodb_newsql.md archive");

    // The ChatNoteDto structure should conform to the schema requirements
    ChatNoteDto dto = result.getChatNoteDto();
    assertNotNull(dto.getMetadata(), "Metadata is required by schema");
    assertNotNull(dto.getMetadata().getArchiveVersion(), "Archive version is required");
    assertNotNull(dto.getMetadata().getTitle(), "Title is required");
  }
}
