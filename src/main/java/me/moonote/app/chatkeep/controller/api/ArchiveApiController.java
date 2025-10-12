package me.moonote.app.chatkeep.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.UploadArchiveRequest;
import me.moonote.app.chatkeep.dto.response.ApiResponse;
import me.moonote.app.chatkeep.dto.response.ArchiveDetailResponse;
import me.moonote.app.chatkeep.dto.response.ArchiveResponse;
import me.moonote.app.chatkeep.service.ArchiveService;
import me.moonote.app.chatkeep.validation.ArchiveNotFoundException;
import me.moonote.app.chatkeep.validation.InvalidArchiveException;

@Slf4j
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class ArchiveApiController {

  private final ArchiveService archiveService;

  /**
   * Upload and process a new archive
   * POST /api/v1/archives
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ArchiveDetailResponse>> uploadArchive(
      @RequestBody UploadArchiveRequest request) {
    try {
      log.info("Received archive upload request");
      String userId = request.getUserId() != null ? request.getUserId() : "anonymous";

      ArchiveDetailResponse response =
          archiveService.uploadArchive(request.getMarkdownContent(), userId);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Archive uploaded successfully", response));
    } catch (InvalidArchiveException e) {
      log.error("Invalid archive: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Invalid archive: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Error uploading archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to upload archive: " + e.getMessage()));
    }
  }

  /**
   * Get archive by ID
   * GET /api/v1/archives/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ArchiveDetailResponse>> getArchive(@PathVariable String id) {
    try {
      ArchiveDetailResponse response = archiveService.getArchiveById(id);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (ArchiveNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Archive not found: " + id));
    } catch (Exception e) {
      log.error("Error retrieving archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve archive"));
    }
  }

  /**
   * Get all archives (paginated)
   * GET /api/v1/archives?page=0&size=20&sort=createdAt,desc
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ArchiveResponse>>> getAllArchives(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
    try {
      Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
          ? Sort.Direction.ASC
          : Sort.Direction.DESC;
      Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

      Page<ArchiveResponse> archives = archiveService.getAllArchives(pageable);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve archives"));
    }
  }

  /**
   * Get archives by user ID
   * GET /api/v1/archives/user/{userId}
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponse<java.util.List<ArchiveResponse>>> getArchivesByUser(
      @PathVariable String userId) {
    try {
      java.util.List<ArchiveResponse> archives = archiveService.getArchivesByUserId(userId);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving user archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve user archives"));
    }
  }

  /**
   * Search archives by title
   * GET /api/v1/archives/search?title=keyword
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<java.util.List<ArchiveResponse>>> searchArchives(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String tag) {
    try {
      java.util.List<ArchiveResponse> archives;

      if (title != null && !title.isEmpty()) {
        archives = archiveService.searchByTitle(title);
      } else if (tag != null && !tag.isEmpty()) {
        archives = archiveService.searchByTag(tag);
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Either 'title' or 'tag' parameter is required"));
      }

      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error searching archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to search archives"));
    }
  }

  /**
   * Get public archives
   * GET /api/v1/archives/public?page=0&size=20
   */
  @GetMapping("/public")
  public ResponseEntity<ApiResponse<Page<ArchiveResponse>>> getPublicArchives(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ArchiveResponse> archives = archiveService.getPublicArchives(pageable);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving public archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve public archives"));
    }
  }

  /**
   * Update archive visibility
   * PATCH /api/v1/archives/{id}/visibility
   */
  @PatchMapping("/{id}/visibility")
  public ResponseEntity<ApiResponse<ArchiveDetailResponse>> updateVisibility(
      @PathVariable String id, @RequestParam Boolean isPublic) {
    try {
      ArchiveDetailResponse response = archiveService.updateVisibility(id, isPublic);
      return ResponseEntity.ok(ApiResponse.success("Visibility updated successfully", response));
    } catch (ArchiveNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Archive not found: " + id));
    } catch (Exception e) {
      log.error("Error updating visibility", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update visibility"));
    }
  }

  /**
   * Delete archive
   * DELETE /api/v1/archives/{id}
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteArchive(@PathVariable String id) {
    try {
      archiveService.deleteArchive(id);
      return ResponseEntity.ok(ApiResponse.success("Archive deleted successfully", null));
    } catch (ArchiveNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Archive not found: " + id));
    } catch (Exception e) {
      log.error("Error deleting archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to delete archive"));
    }
  }

}
