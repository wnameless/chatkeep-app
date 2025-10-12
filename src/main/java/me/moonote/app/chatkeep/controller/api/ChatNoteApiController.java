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
import me.moonote.app.chatkeep.dto.request.UploadChatNoteRequest;
import me.moonote.app.chatkeep.dto.response.ApiResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteDetailLightResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteDetailResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteResponse;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.service.ChatNoteService;
import me.moonote.app.chatkeep.validation.ChatNoteNotFoundException;
import me.moonote.app.chatkeep.validation.InvalidChatNoteException;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat-notes")
@RequiredArgsConstructor
public class ChatNoteApiController {

  private final ChatNoteService chatNoteService;

  /**
   * Upload and process a new archive
   * POST /api/v1/chat-notes
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> uploadChatNote(
      @RequestBody UploadChatNoteRequest request) {
    try {
      log.info("Received archive upload request");
      String userId = request.getUserId() != null ? request.getUserId() : "anonymous";

      ChatNoteDetailResponse response =
          chatNoteService.uploadChatNote(request.getMarkdownContent(), userId);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Chat note uploaded successfully", response));
    } catch (InvalidChatNoteException e) {
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
   * Get archive by ID (lightweight - without artifact/attachment content)
   * GET /api/v1/chat-notes/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ChatNoteDetailLightResponse>> getArchive(
      @PathVariable String id) {
    try {
      ChatNoteDetailLightResponse response = chatNoteService.getChatNoteById(id);
      return ResponseEntity.ok(ApiResponse.success(response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error retrieving archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve archive"));
    }
  }

  /**
   * Get artifact content by index
   * GET /api/v1/chat-notes/{id}/artifacts/{index}
   */
  @GetMapping("/{id}/artifacts/{index}")
  public ResponseEntity<ApiResponse<Artifact>> getArtifactContent(@PathVariable String id,
      @PathVariable int index) {
    try {
      Artifact artifact = chatNoteService.getArtifactContent(id, index);
      return ResponseEntity.ok(ApiResponse.success(artifact));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error retrieving artifact content", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve artifact content"));
    }
  }

  /**
   * Get attachment content by index
   * GET /api/v1/chat-notes/{id}/attachments/{index}
   */
  @GetMapping("/{id}/attachments/{index}")
  public ResponseEntity<ApiResponse<Attachment>> getAttachmentContent(@PathVariable String id,
      @PathVariable int index) {
    try {
      Attachment attachment = chatNoteService.getAttachmentContent(id, index);
      return ResponseEntity.ok(ApiResponse.success(attachment));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error retrieving attachment content", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve attachment content"));
    }
  }

  /**
   * Get all archives (paginated)
   * GET /api/v1/chat-notes?page=0&size=20&sort=createdAt,desc
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getAllChatNotes(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
    try {
      Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
          ? Sort.Direction.ASC
          : Sort.Direction.DESC;
      Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

      Page<ChatNoteResponse> archives = chatNoteService.getAllChatNotes(pageable);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve archives"));
    }
  }

  /**
   * Get archives by user ID
   * GET /api/v1/chat-notes/user/{userId}
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponse<java.util.List<ChatNoteResponse>>> getArchivesByUser(
      @PathVariable String userId) {
    try {
      java.util.List<ChatNoteResponse> archives = chatNoteService.getChatNotesByUserId(userId);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving user archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve user archives"));
    }
  }

  /**
   * Search archives by title
   * GET /api/v1/chat-notes/search?title=keyword
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<java.util.List<ChatNoteResponse>>> searchArchives(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String tag) {
    try {
      java.util.List<ChatNoteResponse> archives;

      if (title != null && !title.isEmpty()) {
        archives = chatNoteService.searchByTitle(title);
      } else if (tag != null && !tag.isEmpty()) {
        archives = chatNoteService.searchByTag(tag);
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
   * GET /api/v1/chat-notes/public?page=0&size=20
   */
  @GetMapping("/public")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getPublicChatNotes(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> archives = chatNoteService.getPublicChatNotes(pageable);
      return ResponseEntity.ok(ApiResponse.success(archives));
    } catch (Exception e) {
      log.error("Error retrieving public archives", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve public archives"));
    }
  }

  /**
   * Update archive visibility
   * PATCH /api/v1/chat-notes/{id}/visibility
   */
  @PatchMapping("/{id}/visibility")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateVisibility(
      @PathVariable String id, @RequestParam Boolean isPublic) {
    try {
      ChatNoteDetailResponse response = chatNoteService.updateVisibility(id, isPublic);
      return ResponseEntity.ok(ApiResponse.success("Visibility updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating visibility", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update visibility"));
    }
  }

  /**
   * Delete archive
   * DELETE /api/v1/chat-notes/{id}
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteChatNote(@PathVariable String id) {
    try {
      chatNoteService.deleteChatNote(id);
      return ResponseEntity.ok(ApiResponse.success("Chat note deleted successfully", null));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error deleting archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to delete archive"));
    }
  }

}
