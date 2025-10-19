package me.moonote.app.chatkeep.controller.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.CreateLabelRequest;
import me.moonote.app.chatkeep.dto.request.UpdateLabelRequest;
import me.moonote.app.chatkeep.dto.response.ApiResponse;
import me.moonote.app.chatkeep.dto.response.LabelResponse;
import me.moonote.app.chatkeep.service.LabelService;

/**
 * RESTful API endpoints for label management. Returns JSON responses for programmatic access.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/labels")
@RequiredArgsConstructor
public class LabelApiController {

  private final LabelService labelService;

  /**
   * Create a new label POST /api/v1/labels
   *
   * @param request Create label request
   * @return Created label
   */
  @PostMapping
  public ResponseEntity<ApiResponse<LabelResponse>> createLabel(
      @Valid @RequestBody CreateLabelRequest request) {
    try {
      LabelResponse created = labelService.createLabel(request);
      log.info("Label created: {} (id: {})", created.getName(), created.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    } catch (IllegalArgumentException e) {
      log.warn("Label creation failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error creating label", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to create label"));
    }
  }

  /**
   * Get all labels for current user GET /api/v1/labels
   *
   * @return List of labels with usage counts
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<LabelResponse>>> getUserLabels() {
    try {
      List<LabelResponse> labels = labelService.getUserLabels();
      log.info("Retrieved {} labels for current user", labels.size());
      return ResponseEntity.ok(ApiResponse.success(labels));
    } catch (Exception e) {
      log.error("Error retrieving labels", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve labels"));
    }
  }

  /**
   * Get label by ID GET /api/v1/labels/{id}
   *
   * @param id Label ID
   * @return Label details
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<LabelResponse>> getLabelById(@PathVariable String id) {
    try {
      LabelResponse label = labelService.getLabelById(id);
      return ResponseEntity.ok(ApiResponse.success(label));
    } catch (IllegalArgumentException e) {
      log.warn("Label not found or unauthorized: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error retrieving label {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve label"));
    }
  }

  /**
   * Update label PATCH /api/v1/labels/{id}
   *
   * @param id Label ID
   * @param request Update label request
   * @return Updated label
   */
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<LabelResponse>> updateLabel(@PathVariable String id,
      @Valid @RequestBody UpdateLabelRequest request) {
    try {
      LabelResponse updated = labelService.updateLabel(id, request);
      log.info("Label updated: {} (id: {})", updated.getName(), updated.getId());
      return ResponseEntity.ok(ApiResponse.success(updated));
    } catch (IllegalArgumentException e) {
      log.warn("Label update failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error updating label {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update label"));
    }
  }

  /**
   * Delete label DELETE /api/v1/labels/{id} Cascade deletes: removes label from all ChatNotes
   *
   * @param id Label ID
   * @return Success message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteLabel(@PathVariable String id) {
    try {
      labelService.deleteLabel(id);
      log.info("Label deleted: {}", id);
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } catch (IllegalArgumentException e) {
      log.warn("Label deletion failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error deleting label {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to delete label"));
    }
  }

}
