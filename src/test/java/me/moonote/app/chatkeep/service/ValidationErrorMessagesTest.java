package me.moonote.app.chatkeep.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.moonote.app.chatkeep.validation.ChatNoteValidationResult;
import me.moonote.app.chatkeep.validation.JsonSchemaValidator;

@SpringBootTest
class ValidationErrorMessagesTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JsonSchemaValidator schemaValidator;

  private MarkdownChatNotePreprocessor preprocessor;

  @BeforeEach
  void setUp() {
    preprocessor = new MarkdownChatNotePreprocessor(objectMapper, schemaValidator);
  }

  @Test
  void testGeminiExample_ShouldPassWithCalculatedCounts() throws IOException {
    // Arrange - Load the Gemini example that previously had count mismatches in YAML
    // Now that counts are calculated by backend, this should pass validation
    String geminiMarkdown =
        Files.readString(Paths.get("/Users/wmw/Downloads/gemini_example.md"));

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(geminiMarkdown);

    // Assert - Should now pass validation because counts are calculated from content
    assertTrue(result.isValid(), "Gemini example should pass validation with calculated counts");
    assertNotNull(result.getChatNoteDto(), "ChatNoteDto should be populated");
    assertTrue(result.getErrors().isEmpty(), "Should have no validation errors");

    // Verify that counts were correctly calculated from actual content (not from YAML)
    assertNotNull(result.getChatNoteDto().getMetadata().getArtifactCount(),
        "Should have calculated artifact count from actual :::artifact markers");
  }

  @Test
  void testMissingYamlFrontmatter_ShouldReturnHelpfulError() {
    // Arrange
    String invalidMarkdown = "# Just a title\nNo YAML frontmatter here";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(invalidMarkdown);

    // Assert
    assertFalse(result.isValid(), "Should fail validation");
    assertFalse(result.getErrors().isEmpty(), "Should have validation errors");

    boolean hasYamlError = result.getErrors().stream()
        .anyMatch(error -> error.contains("Missing YAML frontmatter")
            && error.contains("Archives must start with '---'"));

    assertTrue(hasYamlError, "Should have error message about missing YAML frontmatter");
  }

  @Test
  void testMalformedYaml_ShouldReturnHelpfulError() {
    // Arrange - YAML without closing ---
    String invalidMarkdown = "---\nARCHIVE_FORMAT_VERSION: 1.0\n\n# Title\n## Initial Query\n";

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(invalidMarkdown);

    // Assert
    assertFalse(result.isValid(), "Should fail validation");

    boolean hasYamlError = result.getErrors().stream()
        .anyMatch(error -> error.contains("Malformed YAML frontmatter")
            && error.contains("properly closed"));

    assertTrue(hasYamlError, "Should have error message about malformed YAML");
  }

  @Test
  void testMissingRequiredSections_ShouldReturnHelpfulError() {
    // Arrange - Valid YAML but missing required sections
    String invalidMarkdown = """
        ---
        ARCHIVE_FORMAT_VERSION: 1.0
        ARCHIVE_TYPE: conversation_summary
        CREATED_DATE: 2025-10-22
        ORIGINAL_PLATFORM: TestAI
        ATTACHMENT_COUNT: 0
        ARTIFACT_COUNT: 0
        ARCHIVE_COMPLETENESS: COMPLETE
        WORKAROUNDS_COUNT: 0
        TOTAL_FILE_SIZE: 1 KB
        ---

        # Test Archive

        ## Some Random Section

        Content here.
        """;

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(invalidMarkdown);

    // Assert
    assertFalse(result.isValid(), "Should fail validation");

    boolean hasInitialQueryError = result.getErrors().stream()
        .anyMatch(error -> error.contains("Missing required section: '## Initial Query'"));

    boolean hasKeyInsightsError = result.getErrors().stream()
        .anyMatch(error -> error.contains("Missing required section: '## Key Insights'"));

    assertTrue(hasInitialQueryError, "Should have error about missing Initial Query section");
    assertTrue(hasKeyInsightsError, "Should have error about missing Key Insights section");
  }

  @Test
  void testCountMismatch_ShouldCalculateFromContent() {
    // Arrange - YAML claims 5 artifacts, but there are none. Backend should calculate 0.
    String markdownWithWrongCount = """
        ---
        ARCHIVE_FORMAT_VERSION: 1.0
        ARCHIVE_TYPE: conversation_summary
        CREATED_DATE: 2025-10-22
        ORIGINAL_PLATFORM: TestAI
        INSTRUCTIONS_FOR_AI: |
          Test instructions
        ---

        # Test Archive

        **Date:** 2025-10-22

        ---

        ## Initial Query

        Test query

        ---

        ## Key Insights

        Test insights

        ---

        ## Follow-up Explorations

        None

        ---

        ## Workarounds Used

        None

        ---

        ## Archive Metadata

        **Original conversation date:** 2025-10-22
        **Archive created:** 2025-10-22
        """;

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(markdownWithWrongCount);

    // Assert - Should pass validation and calculate correct counts
    assertTrue(result.isValid(), "Should pass validation with calculated counts");
    assertEquals(0, result.getChatNoteDto().getMetadata().getArtifactCount(),
        "Should calculate 0 artifacts from actual content, ignoring YAML value");
    assertEquals(0, result.getChatNoteDto().getMetadata().getAttachmentCount(),
        "Should calculate 0 attachments from actual content");
  }
}
