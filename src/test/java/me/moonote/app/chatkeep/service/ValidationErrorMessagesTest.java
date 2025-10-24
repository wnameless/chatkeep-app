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
  void testGeminiExample_ShouldReturnHelpfulValidationErrors() throws IOException {
    // Arrange - Load the Gemini example that lacks proper wrappers
    String geminiMarkdown =
        Files.readString(Paths.get("/Users/wmw/Downloads/gemini_example.md"));

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(geminiMarkdown);

    // Assert - Should fail validation
    assertFalse(result.isValid(), "Gemini example should fail validation");
    assertNull(result.getChatNoteDto(), "ChatNoteDto should be null on validation failure");
    assertFalse(result.getErrors().isEmpty(), "Should have validation errors");

    // Print errors for verification
    System.out.println("\n=== Validation Errors for Gemini Example ===");
    result.getErrors().forEach(error -> System.out.println("- " + error));
    System.out.println("===========================================\n");

    // Check for specific error messages about missing fence markers
    boolean hasArtifactWrapperError = result.getErrors().stream()
        .anyMatch(error -> error.contains("ARTIFACT_COUNT") && error.contains("but no artifacts were found")
            && error.contains(":::artifact"));

    boolean hasAttachmentWrapperError = result.getErrors().stream()
        .anyMatch(error -> error.contains("ATTACHMENT_COUNT") && error.contains("but no attachments were found")
            && error.contains(":::attachment"));

    assertTrue(hasArtifactWrapperError,
        "Should have error message about missing artifact wrappers");
    assertTrue(hasAttachmentWrapperError,
        "Should have error message about missing attachment wrappers");

    // Check that errors mention the AI might not have followed the spec
    boolean mentionsAIIssue = result.getErrors().stream()
        .anyMatch(error -> error.contains("AI") || error.contains("Gemini")
            || error.contains("archiving specification"));

    assertTrue(mentionsAIIssue, "Should mention that the AI may not have followed the spec");
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
  void testCountMismatch_ShouldReturnHelpfulError() {
    // Arrange - Claims to have artifacts but section is missing
    String invalidMarkdown = """
        ---
        ARCHIVE_FORMAT_VERSION: 1.0
        ARCHIVE_TYPE: conversation_summary
        CREATED_DATE: 2025-10-22
        ORIGINAL_PLATFORM: TestAI
        ATTACHMENT_COUNT: 0
        ARTIFACT_COUNT: 5
        ARCHIVE_COMPLETENESS: COMPLETE
        WORKAROUNDS_COUNT: 0
        TOTAL_FILE_SIZE: 1 KB
        ---

        # Test Archive

        ## Initial Query

        Test query

        ## Key Insights

        Test insights
        """;

    // Act
    ChatNoteValidationResult result = preprocessor.preprocess(invalidMarkdown);

    // Assert
    assertFalse(result.isValid(), "Should fail validation");

    boolean hasArtifactSectionError = result.getErrors().stream()
        .anyMatch(error -> error.contains("Missing '## Conversation Artifacts'")
            && error.contains("ARTIFACT_COUNT is 5"));

    assertTrue(hasArtifactSectionError,
        "Should have error about missing Conversation Artifacts section");
  }
}
