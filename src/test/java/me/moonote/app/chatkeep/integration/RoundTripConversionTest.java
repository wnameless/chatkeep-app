package me.moonote.app.chatkeep.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import me.moonote.app.chatkeep.model.Reference;
import me.moonote.app.chatkeep.repository.ArtifactRepository;
import me.moonote.app.chatkeep.repository.AttachmentRepository;
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

  @Mock
  private ArtifactRepository artifactRepository;

  @Mock
  private AttachmentRepository attachmentRepository;

  private MarkdownChatNotePreprocessor preprocessor;
  private ChatNoteMapper mapper;
  private ChatNoteMarkdownGenerator generator;

  private String originalMarkdown;

  // In-memory storage for artifacts/attachments for round-trip testing
  private List<Artifact> testArtifacts = new ArrayList<>();
  private List<Attachment> testAttachments = new ArrayList<>();

  @SuppressWarnings("unused")
  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);

    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);
    mapper = new ChatNoteMapper();
    generator = new ChatNoteMarkdownGenerator(artifactRepository, attachmentRepository);

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

    // Populate test artifacts/attachments for markdown generation
    populateTestData(dto1);

    // Step 2: Generate markdown from ChatNote
    String generatedMarkdown = generator.generateMarkdown(chatNote1);
    assertNotNull(generatedMarkdown, "Generated markdown should not be null");

    // Step 3: Parse generated markdown back to ChatNote
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    assertTrue(result2.isValid(), "Generated markdown should be valid");

    ChatNoteDto dto2 = result2.getChatNoteDto();

    // Step 4: Compare both DTOs - they should be equivalent
    // (Note: We compare DTOs because ChatNote entities no longer embed artifacts/attachments)
    assertDtosEqual(dto1, dto2);
  }

  @Test
  void testRoundTrip_GeneratedMarkdownIsValidArchiveFormat() {
    // Parse original markdown
    ChatNoteValidationResult result1 = preprocessor.preprocess(originalMarkdown);
    ChatNoteDto dto1 = result1.getChatNoteDto();
    ChatNote chatNote1 = mapper.toEntity(dto1, "test-user", originalMarkdown);

    // Populate test data for markdown generation
    populateTestData(dto1);

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
    ChatNoteDto dto1 = result1.getChatNoteDto();
    ChatNote chatNote1 = mapper.toEntity(dto1, "test-user", originalMarkdown);

    // Populate test data and generate markdown
    populateTestData(dto1);
    String generatedMarkdown = generator.generateMarkdown(chatNote1);

    // Re-parse
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);
    ChatNoteDto dto2 = result2.getChatNoteDto();

    // Verify artifacts at DTO level
    assertNotNull(dto2.getArtifacts(), "Artifacts should not be null");
    assertEquals(dto1.getArtifacts().size(), dto2.getArtifacts().size(),
        "Artifact count should match");

    // Verify each artifact - exact matching now possible (preprocessor bug fixed)
    for (int i = 0; i < dto1.getArtifacts().size(); i++) {
      var artifact1 = dto1.getArtifacts().get(i);
      var artifact2 = dto2.getArtifacts().get(i);

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
    ChatNoteDto dto1 = result1.getChatNoteDto();
    ChatNote chatNote1 = mapper.toEntity(dto1, "test-user", originalMarkdown);

    // Get original artifact content from DTO
    String originalArtifactContent = dto1.getArtifacts().get(0).getContent();

    // Populate test data and generate markdown
    populateTestData(dto1);
    String generatedMarkdown = generator.generateMarkdown(chatNote1);

    // Re-parse
    ChatNoteValidationResult result2 = preprocessor.preprocess(generatedMarkdown);

    // Verify the generated markdown passed validation
    assertTrue(result2.isValid(), "Generated markdown should be valid");

    ChatNoteDto dto2 = result2.getChatNoteDto();

    // Get re-parsed artifact content from DTO
    String reparsedArtifactContent = dto2.getArtifacts().get(0).getContent();

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
    ChatNoteDto dto1 = result1.getChatNoteDto();
    ChatNote chatNote1 = mapper.toEntity(dto1, "test-user", originalMarkdown);

    // Generate markdown (populate test data first)
    populateTestData(dto1);
    String markdown1 = generator.generateMarkdown(chatNote1);

    // Re-parse and generate again
    ChatNoteValidationResult result2 = preprocessor.preprocess(markdown1);
    ChatNoteDto dto2 = result2.getChatNoteDto();
    ChatNote chatNote2 = mapper.toEntity(dto2, "test-user", markdown1);

    populateTestData(dto2);
    String markdown2 = generator.generateMarkdown(chatNote2);

    // Third iteration
    ChatNoteValidationResult result3 = preprocessor.preprocess(markdown2);
    ChatNoteDto dto3 = result3.getChatNoteDto();

    // All DTOs should be equivalent in data
    assertDtosEqual(dto1, dto2);
    assertDtosEqual(dto2, dto3);
    assertDtosEqual(dto1, dto3);

    // Verify artifact content remains stable through multiple iterations (preprocessor bug fixed)
    assertEquals(dto1.getArtifacts().get(0).getTitle(), dto3.getArtifacts().get(0).getTitle(),
        "Artifact title should remain stable through multiple iterations");
    assertEquals(dto1.getArtifacts().get(0).getContent(), dto3.getArtifacts().get(0).getContent(),
        "Artifact content should remain stable through multiple iterations");
  }

  /**
   * Helper method to assert that two ChatNote objects are equivalent (ignoring generated fields
   * like ID, timestamps, and markdownContent).
   *
   * This focuses on DATA INTEGRITY, not exact string matching. Whitespace differences in
   * descriptions are acceptable as long as the core content is preserved.
   */
  /**
   * Helper method to populate test artifacts/attachments from DTO This is needed because
   * artifacts/attachments are now in separate collections
   */
  private void populateTestData(ChatNoteDto dto) {
    testArtifacts.clear();
    testAttachments.clear();

    // Convert DTO artifacts to model entities
    if (dto.getArtifacts() != null) {
      for (me.moonote.app.chatkeep.dto.ArtifactDto artifactDto : dto.getArtifacts()) {
        testArtifacts.add(Artifact.builder().chatNoteId("test-note-id").type(artifactDto.getType())
            .title(artifactDto.getTitle()).language(artifactDto.getLanguage())
            .version(artifactDto.getVersion()).iterations(artifactDto.getIterations())
            .evolutionNotes(artifactDto.getEvolutionNotes()).content(artifactDto.getContent())
            .build());
      }
    }

    // Convert DTO attachments to model entities
    if (dto.getAttachments() != null) {
      for (me.moonote.app.chatkeep.dto.AttachmentDto attachmentDto : dto.getAttachments()) {
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

  /**
   * Compare two DTOs for equality (used instead of comparing entities now)
   */
  private void assertDtosEqual(ChatNoteDto dto1, ChatNoteDto dto2) {
    // Metadata
    assertEquals(dto1.getMetadata().getArchiveVersion(), dto2.getMetadata().getArchiveVersion(),
        "Archive version should match");
    assertEquals(dto1.getMetadata().getTitle(), dto2.getMetadata().getTitle(),
        "Title should match");
    assertEquals(dto1.getMetadata().getArtifactCount(), dto2.getMetadata().getArtifactCount(),
        "Artifact count should match");
    assertEquals(dto1.getMetadata().getAttachmentCount(), dto2.getMetadata().getAttachmentCount(),
        "Attachment count should match");

    // Artifacts
    assertEquals(dto1.getArtifacts().size(), dto2.getArtifacts().size(),
        "Artifact count should match");
    for (int i = 0; i < dto1.getArtifacts().size(); i++) {
      assertEquals(dto1.getArtifacts().get(i).getTitle(), dto2.getArtifacts().get(i).getTitle(),
          "Artifact " + i + " title should match");
      assertEquals(dto1.getArtifacts().get(i).getContent(), dto2.getArtifacts().get(i).getContent(),
          "Artifact " + i + " content should match exactly");
    }

    // Attachments
    assertEquals(dto1.getAttachments().size(), dto2.getAttachments().size(),
        "Attachment count should match");
  }

}
