package me.moonote.app.chatkeep.controller.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API controller for serving conversation archive templates
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

  private static final String TEMPLATE_PATH = "ai-specs/conversation_archive_template.md";

  /**
   * Get conversation archive template GET /api/v1/templates/archive
   *
   * Returns the markdown template file as plain text for users to copy and use.
   */
  @GetMapping(value = "/archive", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getArchiveTemplate() {
    try {
      log.info("Fetching archive template from: {}", TEMPLATE_PATH);

      // Load template from classpath
      ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);

      if (!resource.exists()) {
        log.error("Template file not found at: {}", TEMPLATE_PATH);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Template file not found");
      }

      // Read template content
      String templateContent =
          new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

      log.info("Successfully loaded archive template ({} bytes)", templateContent.length());
      return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(templateContent);

    } catch (IOException e) {
      log.error("Error reading template file", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to read template file: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error fetching template", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to fetch template: " + e.getMessage());
    }
  }
}
