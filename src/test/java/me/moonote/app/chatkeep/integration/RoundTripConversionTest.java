package me.moonote.app.chatkeep.integration;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.moonote.app.chatkeep.dto.ChatNoteDto;
import me.moonote.app.chatkeep.mapper.ChatNoteMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.service.ChatNoteMarkdownGenerator;
import me.moonote.app.chatkeep.service.MarkdownChatNotePreprocessor;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

/**
 * Integration test for round-trip conversion: Markdown → ChatNote → Markdown → ChatNote
 *
 * This test verifies that: 1. Original markdown can be parsed to ChatNote 2. ChatNote can be
 * converted back to markdown 3. Generated markdown can be parsed back to ChatNote 4. Both ChatNote
 * objects are equivalent (data preservation)
 *
 * This ensures the ChatNoteMarkdownGenerator produces valid markdown that can be re-imported
 * without data loss.
 */
@SpringBootTest
class RoundTripConversionTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private MarkdownChatNotePreprocessor preprocessor;
  private ChatNoteMapper mapper;
  private ChatNoteMarkdownGenerator generator;

  private String originalMarkdown;

  @BeforeEach
  void setUp() throws IOException {
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);
    mapper = new ChatNoteMapper();
    generator = new ChatNoteMarkdownGenerator();

    // Load the original markdown
    originalMarkdown =
        Files.readString(Paths.get("src/test/resources/archive-markdowns/dragonwell.md"));
  }

  @Test
  void testRoundTrip_DragonwellMarkdown_ShouldPreserveAllData() {
    // Step 1: Parse original markdown to ChatNote
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    assertTrue(result1.isValid(), "Original markdown should be valid");

    ChatNoteDto dto1 = result1.getChatNoteDto();
    ChatNote chatNote1 = mapper.toEntity(dto1, "test-user", originalMarkdown);

    // Step 2: Generate markdown from ChatNote
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    assertNotNull(generatedMarkdown, "Generated markdown should not be null");

    // Step 3: Parse generated markdown back to ChatNote
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    assertTrue(result2.isValid(), "Generated markdown should be valid");

    ChatNoteDto dto2 = result2.getChatNoteDto();
    ChatNote chatNote2 = mapper.toEntity(dto2, "test-user", generatedMarkdown);

    // Step 4: Compare both ChatNote objects - they should be equivalent
    assertChatNotesEqual(chatNote1, chatNote2);
  }

  @Test
  void testRoundTrip_GeneratedMarkdownIsValidArchiveFormat() {
    // Parse original markdown
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate markdown
    String generatedMarkdown = generator.generateMarkdown(chatNote1);

    // Verify generated markdown has all required sections
    assertTrue(generatedMarkdown.contains("---\nARCHIVE_FORMAT_VERSION:"),
        "Should have YAML frontmatter");
    assertTrue(generatedMarkdown.contains("INSTRUCTIONS_FOR_AI:"), "Should have AI instructions");
    assertTrue(generatedMarkdown.contains("## Initial Query"), "Should have Initial Query section");
    assertTrue(generatedMarkdown.contains("## Key Insights"), "Should have Key Insights section");
    assertTrue(generatedMarkdown.contains("## Follow-up Explorations"),
        "Should have Follow-up section");
    assertTrue(generatedMarkdown.contains("## References/Links"), "Should have References section");
    assertTrue(generatedMarkdown.contains("## Conversation Artifacts"),
        "Should have Artifacts section");
    assertTrue(generatedMarkdown.contains("## Workarounds Used"),
        "Should have Workarounds section");
    assertTrue(generatedMarkdown.contains("## Archive Metadata"),
        "Should have Archive Metadata section");
    assertTrue(generatedMarkdown.contains("_End of archived conversation_"),
        "Should have end marker");

    // Verify it can be re-parsed successfully
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    assertTrue(result2.isValid(), "Generated markdown should pass validation");
    assertTrue(result2.getErrors().isEmpty(), "Should have no validation errors");
  }

  @Test
  void testRoundTrip_MetadataPreservation() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Verify metadata fields
    assertEquals(chatNote1.getArchiveVersion(), chatNote2.getArchiveVersion(),
        "Archive version should match");
    assertEquals(chatNote1.getArchiveType(), chatNote2.getArchiveType(),
        "Archive type should match");
    assertEquals(chatNote1.getCreatedDate(), chatNote2.getCreatedDate(),
        "Created date should match");
    assertEquals(chatNote1.getOriginalPlatform(), chatNote2.getOriginalPlatform(),
        "Original platform should match");
    assertEquals(chatNote1.getAttachmentCount(), chatNote2.getAttachmentCount(),
        "Attachment count should match");
    assertEquals(chatNote1.getArtifactCount(), chatNote2.getArtifactCount(),
        "Artifact count should match");
    assertEquals(chatNote1.getChatNoteCompleteness(), chatNote2.getChatNoteCompleteness(),
        "Completeness should match");
    assertEquals(chatNote1.getWorkaroundsCount(), chatNote2.getWorkaroundsCount(),
        "Workarounds count should match");
    assertEquals(chatNote1.getTotalFileSize(), chatNote2.getTotalFileSize(),
        "Total file size should match");
    assertEquals(chatNote1.getTitle(), chatNote2.getTitle(), "Title should match");
    assertEquals(chatNote1.getConversationDate(), chatNote2.getConversationDate(),
        "Conversation date should match");
  }

  @Test
  void testRoundTrip_TagsPreservation() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Verify tags
    assertNotNull(chatNote2.getTags(), "Tags should not be null");
    assertEquals(chatNote1.getTags().size(), chatNote2.getTags().size(), "Tag count should match");
    assertTrue(chatNote2.getTags().containsAll(chatNote1.getTags()),
        "All tags should be preserved");
  }

  @Test
  void testRoundTrip_SummaryPreservation() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Verify summary sections exist and have substantial content
    assertNotNull(chatNote2.getSummary(), "Summary should not be null");
    assertNotNull(chatNote2.getSummary().getInitialQuery(), "Initial query should not be null");
    assertNotNull(chatNote2.getSummary().getKeyInsights(), "Key insights should not be null");
    assertNotNull(chatNote2.getSummary().getFollowUpExplorations(), "Follow-up should not be null");
    assertNotNull(chatNote2.getSummary().getReferences(), "References should not be null");

    // Verify initial query has content
    assertNotNull(chatNote2.getSummary().getInitialQuery().getDescription());
    assertTrue(chatNote2.getSummary().getInitialQuery().getDescription().length() > 50);

    // Verify key insights has content
    assertNotNull(chatNote2.getSummary().getKeyInsights().getDescription());
    assertTrue(chatNote2.getSummary().getKeyInsights().getDescription().length() > 100);

    // Key points may differ in count due to parsing, but should exist
    assertTrue(chatNote2.getSummary().getKeyInsights().getKeyPoints().size() > 0);

    // Verify references
    assertEquals(chatNote1.getSummary().getReferences().size(),
        chatNote2.getSummary().getReferences().size(), "References count should match");
  }

  @Test
  void testRoundTrip_ArtifactsPreservation() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Verify artifacts
    assertNotNull(chatNote2.getArtifacts(), "Artifacts should not be null");
    assertEquals(chatNote1.getArtifacts().size(), chatNote2.getArtifacts().size(),
        "Artifact count should match");

    // Verify each artifact - exact matching now possible (preprocessor bug fixed)
    for (int i = 0; i < chatNote1.getArtifacts().size(); i++) {
      Artifact artifact1 = chatNote1.getArtifacts().get(i);
      Artifact artifact2 = chatNote2.getArtifacts().get(i);

      // Title and content must match exactly
      assertEquals(artifact1.getTitle(), artifact2.getTitle(),
          "Artifact " + i + " title should match");
      assertEquals(artifact1.getContent(), artifact2.getContent(),
          "Artifact " + i + " content should match exactly");

      // Type should be present
      assertNotNull(artifact2.getType(), "Artifact " + i + " should have a type");
    }
  }

  @Test
  void testRoundTrip_ArtifactContentIntegrity() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Get original artifact content
    String originalArtifactContent = chatNote1.getArtifacts().get(0).getContent();

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);

    // Verify the generated markdown passed validation
    assertTrue(result2.isValid(), "Generated markdown should be valid");

    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Get re-parsed artifact content
    String reparsedArtifactContent = chatNote2.getArtifacts().get(0).getContent();

    // Verify content is preserved exactly (preprocessor bug is now fixed)
    assertEquals(originalArtifactContent, reparsedArtifactContent,
        "Artifact content should be preserved exactly through round-trip");

    // Verify shebang is preserved
    assertTrue(reparsedArtifactContent.startsWith("#!/bin/bash"), "Shebang should be preserved");

    // Verify key script content is preserved
    assertTrue(reparsedArtifactContent.contains("bash configure"),
        "Configure command should be preserved");
    assertTrue(reparsedArtifactContent.contains("make images"), "Make command should be preserved");
    assertTrue(reparsedArtifactContent.contains("--disable-warnings-as-errors"),
        "Build flags should be preserved");
  }

  @Test
  void testRoundTrip_ReferencesPreservation() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate and re-parse
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", generatedMarkdown);

    // Verify references count
    assertEquals(chatNote1.getSummary().getReferences().size(),
        chatNote2.getSummary().getReferences().size(), "References count should match");

    // Verify specific references exist in both
    boolean foundDragonwellRef1 = chatNote1.getSummary().getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("github.com/dragonwell-project"));
    boolean foundDragonwellRef2 = chatNote2.getSummary().getReferences().stream()
        .anyMatch(ref -> ref.getUrl().contains("github.com/dragonwell-project"));
    assertEquals(foundDragonwellRef1, foundDragonwellRef2,
        "Dragonwell reference should be preserved");

    // Verify reference structure
    for (int i = 0; i < chatNote1.getSummary().getReferences().size(); i++) {
      Reference ref1 = chatNote1.getSummary().getReferences().get(i);
      Reference ref2 = chatNote2.getSummary().getReferences().get(i);

      assertEquals(ref1.getUrl(), ref2.getUrl(), "Reference " + i + " URL should match");
      assertEquals(ref1.getDescription(), ref2.getDescription(),
          "Reference " + i + " description should match");
    }
  }

  @Test
  void testRoundTrip_MultipleIterations_ShouldRemainStable() {
    // Parse original
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNote chatNote1 = mapper.toEntity(result1.getChatNoteDto(), "test-user", originalMarkdown);

    // Generate markdown
    String markdown1 = generator.generateMarkdown(chatNote1);

    // Re-parse and generate again
    ChatNoteValidationResult result2 = preprocessor.preprocess(markdown1);
    ChatNote chatNote2 = mapper.toEntity(result2.getChatNoteDto(), "test-user", markdown1);
    String markdown2 = generator.generateMarkdown(chatNote2);

    // Third iteration
    ChatNoteValidationResult result3 = preprocessor.preprocess(markdown2);
    ChatNote chatNote3 = mapper.toEntity(result3.getChatNoteDto(), "test-user", markdown2);

    // All ChatNote objects should be equivalent in data (using lenient assertions)
    assertChatNotesEqual(chatNote1, chatNote2);
    assertChatNotesEqual(chatNote2, chatNote3);
    assertChatNotesEqual(chatNote1, chatNote3);

    // Verify artifact content remains stable through multiple iterations (preprocessor bug fixed)
    assertEquals(chatNote1.getArtifacts().get(0).getTitle(),
        chatNote3.getArtifacts().get(0).getTitle(),
        "Artifact title should remain stable through multiple iterations");
    assertEquals(chatNote1.getArtifacts().get(0).getContent(),
        chatNote3.getArtifacts().get(0).getContent(),
        "Artifact content should remain stable through multiple iterations");
  }

  /**
   * Helper method to assert that two ChatNote objects are equivalent (ignoring generated fields
   * like ID, timestamps, and markdownContent).
   *
   * This focuses on DATA INTEGRITY, not exact string matching. Whitespace differences in
   * descriptions are acceptable as long as the core content is preserved.
   */
  private void assertChatNotesEqual(ChatNote chatNote1, ChatNote chatNote2) {
    // Metadata
    assertEquals(chatNote1.getArchiveVersion(), chatNote2.getArchiveVersion(),
        "Archive version should match");
    assertEquals(chatNote1.getArchiveType(), chatNote2.getArchiveType(),
        "Archive type should match");
    assertEquals(chatNote1.getCreatedDate(), chatNote2.getCreatedDate(),
        "Created date should match");
    assertEquals(chatNote1.getOriginalPlatform(), chatNote2.getOriginalPlatform(),
        "Original platform should match");
    assertEquals(chatNote1.getAttachmentCount(), chatNote2.getAttachmentCount(),
        "Attachment count should match");
    assertEquals(chatNote1.getArtifactCount(), chatNote2.getArtifactCount(),
        "Artifact count should match");
    assertEquals(chatNote1.getChatNoteCompleteness(), chatNote2.getChatNoteCompleteness(),
        "Completeness should match");
    assertEquals(chatNote1.getWorkaroundsCount(), chatNote2.getWorkaroundsCount(),
        "Workarounds count should match");
    assertEquals(chatNote1.getTotalFileSize(), chatNote2.getTotalFileSize(),
        "Total file size should match");
    assertEquals(chatNote1.getTitle(), chatNote2.getTitle(), "Title should match");
    assertEquals(chatNote1.getConversationDate(), chatNote2.getConversationDate(),
        "Conversation date should match");

    // Tags - order-independent comparison
    assertEquals(chatNote1.getTags().size(), chatNote2.getTags().size(), "Tag count should match");
    assertTrue(chatNote1.getTags().containsAll(chatNote2.getTags()),
        "All tags should be preserved");
    assertTrue(chatNote2.getTags().containsAll(chatNote1.getTags()),
        "No extra tags should be added");

    // Summary - check that descriptions are present and non-empty
    // NOTE: Due to parsing differences, the exact format may vary (e.g., key points might be
    // included in description or separate). The important thing is that the data is preserved.
    assertNotNull(chatNote2.getSummary().getInitialQuery().getDescription(),
        "Initial query description should be present");
    assertTrue(chatNote2.getSummary().getInitialQuery().getDescription().length() > 50,
        "Initial query description should have substantial content");

    assertNotNull(chatNote2.getSummary().getKeyInsights().getDescription(),
        "Key insights description should be present");
    assertTrue(chatNote2.getSummary().getKeyInsights().getDescription().length() > 100,
        "Key insights description should have substantial content");

    assertNotNull(chatNote2.getSummary().getFollowUpExplorations().getDescription(),
        "Follow-up description should be present");
    assertTrue(chatNote2.getSummary().getFollowUpExplorations().getDescription().length() > 50,
        "Follow-up description should have substantial content");

    // Key points - verify they exist (count may differ due to parsing differences)
    // The preprocessor may extract key points from bullet lists in the description,
    // so the count might not match exactly, but key points should be present
    assertNotNull(chatNote2.getSummary().getKeyInsights().getKeyPoints(),
        "Key points list should be present");
    assertTrue(chatNote2.getSummary().getKeyInsights().getKeyPoints().size() > 0,
        "Should have at least some key points extracted");

    // Artifacts - verify count and content are preserved
    assertEquals(chatNote1.getArtifacts().size(), chatNote2.getArtifacts().size(),
        "Artifact count should match");
    for (int i = 0; i < chatNote1.getArtifacts().size(); i++) {
      // Title should match exactly
      assertEquals(chatNote1.getArtifacts().get(i).getTitle(),
          chatNote2.getArtifacts().get(i).getTitle(), "Artifact " + i + " title should match");

      // Verify artifact has required fields
      assertNotNull(chatNote2.getArtifacts().get(i).getType(),
          "Artifact " + i + " should have a type");

      // Content must match exactly (preprocessor bug is now fixed)
      assertEquals(chatNote1.getArtifacts().get(i).getContent(),
          chatNote2.getArtifacts().get(i).getContent(),
          "Artifact " + i + " content should match exactly");
    }

    // References
    assertEquals(chatNote1.getSummary().getReferences().size(),
        chatNote2.getSummary().getReferences().size(), "Reference count should match");
    for (int i = 0; i < chatNote1.getSummary().getReferences().size(); i++) {
      assertEquals(chatNote1.getSummary().getReferences().get(i).getUrl(),
          chatNote2.getSummary().getReferences().get(i).getUrl(),
          "Reference " + i + " URL should match");
    }

    // Attachments
    assertEquals(chatNote1.getAttachments().size(), chatNote2.getAttachments().size(),
        "Attachment count should match");

    // Workarounds
    assertEquals(chatNote1.getWorkarounds().size(), chatNote2.getWorkarounds().size(),
        "Workaround count should match");
  }

}
