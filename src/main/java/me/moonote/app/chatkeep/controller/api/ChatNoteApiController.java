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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
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
   * Upload and process a new archive POST /api/v1/chat-notes Also supports copying an existing
   * public note to user's workspace
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> uploadChatNote(
      @RequestBody UploadChatNoteRequest request) {
    try {
      // Get current user from security context (works for both anonymous and authenticated users)
      String userId = me.moonote.app.chatkeep.security.SecurityUtils.getCurrentUserId();

      if (userId == null) {
        log.error("Upload attempted without authenticated user");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse
            .error("User not authenticated. Please provide X-Anonymous-User-Id header or login."));
      }

      // Check if this is a copy request
      if (request.getSourceNoteId() != null && Boolean.TRUE.equals(request.getCopyFromPublic())) {
        log.info("Received copy request for note: {} by user: {}", request.getSourceNoteId(),
            userId);
        ChatNoteDetailResponse response =
            chatNoteService.copyChatNoteToWorkspace(request.getSourceNoteId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Chat note copied to workspace successfully", response));
      }

      // Standard upload
      log.info("Received archive upload request for user: {}", userId);
      ChatNoteDetailResponse response =
          chatNoteService.uploadChatNote(request.getMarkdownContent(), userId);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Chat note uploaded successfully", response));
    } catch (InvalidChatNoteException e) {
      log.error("Invalid archive: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Invalid archive: " + e.getMessage()));
    } catch (ChatNoteNotFoundException e) {
      log.error("Source note not found: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Source note not found: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Error uploading archive", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to upload archive: " + e.getMessage()));
    }
  }

  /**
   * Upload markdown file (multipart file upload) POST /api/v1/chat-notes/upload
   */
  @PostMapping("/upload")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> uploadMarkdownFile(
      @RequestParam("file") MultipartFile file) {
    try {
      // Get current user from security context
      String userId = me.moonote.app.chatkeep.security.SecurityUtils.getCurrentUserId();

      if (userId == null) {
        log.error("Upload attempted without authenticated user");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse
            .error("User not authenticated. Please provide X-Anonymous-User-Id header or login."));
      }

      // Validate file
      if (file.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("File is empty"));
      }

      // Check file extension
      String originalFilename = file.getOriginalFilename();
      if (originalFilename == null
          || (!originalFilename.endsWith(".md") && !originalFilename.endsWith(".markdown"))) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Only .md or .markdown files are allowed"));
      }

      // Read file content
      String markdownContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);

      log.info("Received markdown file upload for user: {}, filename: {}", userId,
          originalFilename);
      ChatNoteDetailResponse response = chatNoteService.uploadChatNote(markdownContent, userId);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Markdown file uploaded successfully", response));
    } catch (InvalidChatNoteException e) {
      log.error("Invalid markdown file: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Invalid markdown file: " + e.getMessage()));
    } catch (java.io.IOException e) {
      log.error("Error reading file", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to read file: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Error uploading markdown file", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to upload markdown file: " + e.getMessage()));
    }
  }

  /**
   * Get archive by ID (lightweight - without artifact/attachment content) GET
   * /api/v1/chat-notes/{id}
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
   * Get artifact content by index GET /api/v1/chat-notes/{id}/artifacts/{index}
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
   * Get attachment content by index GET /api/v1/chat-notes/{id}/attachments/{index}
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
   * Download full markdown archive GET /api/v1/chat-notes/{id}/download
   */
  @GetMapping("/{id}/download")
  public ResponseEntity<String> downloadChatNote(@PathVariable String id) {
    try {
      ChatNoteDetailLightResponse chatNote = chatNoteService.getChatNoteById(id);
      String markdown = chatNote.getFullMarkdown();

      // Generate safe filename from title
      String filename = chatNote.getTitle().replaceAll("[^a-zA-Z0-9-]", "_") + ".md";

      return ResponseEntity.ok().header("Content-Type", "text/markdown; charset=UTF-8")
          .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .body(markdown);
    } catch (ChatNoteNotFoundException e) {
      log.error("Chat note not found for download: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat note not found: " + id);
    } catch (Exception e) {
      log.error("Error downloading chat note", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to download chat note: " + e.getMessage());
    }
  }

  /**
   * Get all archives (paginated) GET /api/v1/chat-notes?page=0&size=20&sort=createdAt,desc
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getAllChatNotes(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
    try {
      Sort.Direction direction =
          sort.length > 1 && sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
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
   * Get archives by user ID GET /api/v1/chat-notes/user/{userId}
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
   * Search archives by title GET /api/v1/chat-notes/search?title=keyword
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<java.util.List<ChatNoteResponse>>> searchArchives(
      @RequestParam(required = false) String title, @RequestParam(required = false) String tag) {
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
   * Get public archives GET /api/v1/chat-notes/public?page=0&size=20
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

  // ==================== Tag-Based Filtering Endpoints ====================

  /**
   * Filter chat notes by multiple tags (global) GET
   * /api/v1/chat-notes/filter/tags?tags=java,spring&operator=AND
   */
  @GetMapping("/filter/tags")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> filterByTags(
      @RequestParam java.util.List<String> tags,
      @RequestParam(defaultValue = "AND") String operator,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> chatNotes = chatNoteService.filterByTags(tags, operator, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error filtering chat notes by tags", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to filter chat notes by tags"));
    }
  }

  /**
   * Filter chat notes by tags for a specific user GET
   * /api/v1/chat-notes/user/{userId}/filter/tags?tags=java,spring&operator=AND&lifecycle=active
   */
  @GetMapping("/user/{userId}/filter/tags")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> filterByTagsForUser(
      @PathVariable String userId, @RequestParam java.util.List<String> tags,
      @RequestParam(defaultValue = "AND") String operator,
      @RequestParam(defaultValue = "active") String lifecycle,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> chatNotes;

      if ("active".equalsIgnoreCase(lifecycle)) {
        chatNotes = chatNoteService.filterActiveByTagsForUser(userId, tags, operator, pageable);
      } else if ("all".equalsIgnoreCase(lifecycle)) {
        chatNotes = chatNoteService.filterByTagsForUser(userId, tags, operator, pageable);
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid lifecycle parameter. Use 'active' or 'all'"));
      }

      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error filtering user chat notes by tags", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to filter user chat notes by tags"));
    }
  }

  /**
   * Filter active (not archived, not trashed) chat notes by tags (global) GET
   * /api/v1/chat-notes/filter/tags/active?tags=java,spring&operator=OR
   */
  @GetMapping("/filter/tags/active")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> filterActiveByTags(
      @RequestParam java.util.List<String> tags,
      @RequestParam(defaultValue = "AND") String operator,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> chatNotes =
          chatNoteService.filterActiveByTags(tags, operator, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error filtering active chat notes by tags", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to filter active chat notes by tags"));
    }
  }

  /**
   * Update archive visibility PATCH /api/v1/chat-notes/{id}/visibility
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

  // ==================== Favorites Management Endpoints ====================

  /**
   * Toggle favorite status (star/unstar) PATCH /api/v1/chat-notes/{id}/favorite
   */
  @PatchMapping("/{id}/favorite")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> toggleFavorite(@PathVariable String id,
      @RequestParam Boolean isFavorite) {
    try {
      ChatNoteDetailResponse response = chatNoteService.toggleFavorite(id, isFavorite);
      return ResponseEntity
          .ok(ApiResponse.success("Favorite status updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating favorite status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update favorite status"));
    }
  }

  /**
   * Get all favorite chat notes for a user GET /api/v1/chat-notes/user/{userId}/favorites
   */
  @GetMapping("/user/{userId}/favorites")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getFavoriteChatNotes(
      @PathVariable String userId, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
      Page<ChatNoteResponse> chatNotes = chatNoteService.getFavoriteChatNotes(userId, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving favorite chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve favorite chat notes"));
    }
  }

  /**
   * Get favorite active chat notes for a user (not archived, not trashed) GET
   * /api/v1/chat-notes/user/{userId}/favorites/active
   */
  @GetMapping("/user/{userId}/favorites/active")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getFavoriteActiveChatNotes(
      @PathVariable String userId, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
      Page<ChatNoteResponse> chatNotes =
          chatNoteService.getFavoriteActiveChatNotes(userId, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving favorite active chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve favorite active chat notes"));
    }
  }

  /**
   * Delete archive DELETE /api/v1/chat-notes/{id}
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

  // ==================== Lifecycle Management Endpoints ====================

  /**
   * Update archive status (archive/unarchive) PATCH /api/v1/chat-notes/{id}/archive
   */
  @PatchMapping("/{id}/archive")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateArchiveStatus(
      @PathVariable String id, @RequestParam Boolean isArchived) {
    try {
      ChatNoteDetailResponse response = chatNoteService.updateArchiveStatus(id, isArchived);
      return ResponseEntity
          .ok(ApiResponse.success("Archive status updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating archive status", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update archive status"));
    }
  }

  /**
   * Move chat note to trash (soft delete) PATCH /api/v1/chat-notes/{id}/trash
   */
  @PatchMapping("/{id}/trash")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> moveToTrash(@PathVariable String id) {
    try {
      ChatNoteDetailResponse response = chatNoteService.moveToTrash(id);
      return ResponseEntity.ok(ApiResponse.success("Chat note moved to trash", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error moving chat note to trash", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to move chat note to trash"));
    }
  }

  /**
   * Restore chat note from trash PATCH /api/v1/chat-notes/{id}/restore
   */
  @PatchMapping("/{id}/restore")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> restoreFromTrash(
      @PathVariable String id) {
    try {
      ChatNoteDetailResponse response = chatNoteService.restoreFromTrash(id);
      return ResponseEntity.ok(ApiResponse.success("Chat note restored from trash", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error restoring chat note from trash", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to restore chat note from trash"));
    }
  }

  /**
   * Permanently delete chat note (hard delete) DELETE /api/v1/chat-notes/{id}/permanent
   */
  @DeleteMapping("/{id}/permanent")
  public ResponseEntity<ApiResponse<Void>> permanentlyDeleteChatNote(@PathVariable String id) {
    try {
      chatNoteService.permanentlyDeleteChatNote(id);
      return ResponseEntity.ok(ApiResponse.success("Chat note permanently deleted", null));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error permanently deleting chat note", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to permanently delete chat note"));
    }
  }

  /**
   * Get active chat notes for a user GET /api/v1/chat-notes/user/{userId}/active
   */
  @GetMapping("/user/{userId}/active")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getActiveChatNotes(
      @PathVariable String userId, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> chatNotes = chatNoteService.getActiveChatNotes(userId, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving active chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve active chat notes"));
    }
  }

  /**
   * Get archived chat notes for a user GET /api/v1/chat-notes/user/{userId}/archived
   */
  @GetMapping("/user/{userId}/archived")
  public ResponseEntity<ApiResponse<java.util.List<ChatNoteResponse>>> getArchivedChatNotes(
      @PathVariable String userId) {
    try {
      java.util.List<ChatNoteResponse> chatNotes = chatNoteService.getArchivedChatNotes(userId);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving archived chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve archived chat notes"));
    }
  }

  /**
   * Get trashed chat notes for a user GET /api/v1/chat-notes/user/{userId}/trash
   */
  @GetMapping("/user/{userId}/trash")
  public ResponseEntity<ApiResponse<java.util.List<ChatNoteResponse>>> getTrashedChatNotes(
      @PathVariable String userId) {
    try {
      java.util.List<ChatNoteResponse> chatNotes = chatNoteService.getTrashedChatNotes(userId);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving trashed chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve trashed chat notes"));
    }
  }

  /**
   * Get all archived chat notes (paginated) - admin/global view GET /api/v1/chat-notes/archived
   */
  @GetMapping("/archived")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getAllArchivedChatNotes(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<ChatNoteResponse> chatNotes = chatNoteService.getAllArchivedChatNotes(pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving all archived chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve all archived chat notes"));
    }
  }

  /**
   * Get all trashed chat notes (paginated) - admin/global view GET /api/v1/chat-notes/trash
   */
  @GetMapping("/trash")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> getAllTrashedChatNotes(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "trashedAt"));
      Page<ChatNoteResponse> chatNotes = chatNoteService.getAllTrashedChatNotes(pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error retrieving all trashed chat notes", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve all trashed chat notes"));
    }
  }

  // ==================== Field Update Endpoints ====================

  /**
   * Update chat note title PUT /api/v1/chat-notes/{id}/title
   */
  @PutMapping("/{id}/title")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateTitle(@PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateTitleRequest request) {
    try {
      ChatNoteDetailResponse response = chatNoteService.updateTitle(id, request.getTitle());
      return ResponseEntity.ok(ApiResponse.success("Title updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating title", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update title"));
    }
  }

  /**
   * Update chat note tags PUT /api/v1/chat-notes/{id}/tags
   */
  @PutMapping("/{id}/tags")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateTags(@PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateTagsRequest request) {
    try {
      ChatNoteDetailResponse response = chatNoteService.updateTags(id, request.getTags());
      return ResponseEntity.ok(ApiResponse.success("Tags updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating tags", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update tags"));
    }
  }

  /**
   * Update conversation date PUT /api/v1/chat-notes/{id}/conversation-date
   */
  @PutMapping("/{id}/conversation-date")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateConversationDate(
      @PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateConversationDateRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateConversationDate(id, request.getConversationDate());
      return ResponseEntity
          .ok(ApiResponse.success("Conversation date updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating conversation date", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update conversation date"));
    }
  }

  /**
   * Update initial query section PUT /api/v1/chat-notes/{id}/initial-query
   */
  @PutMapping("/{id}/initial-query")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateInitialQuery(
      @PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateInitialQueryRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateInitialQuery(id, request.getDescription());
      return ResponseEntity.ok(ApiResponse.success("Initial query updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating initial query", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update initial query"));
    }
  }

  /**
   * Update key insights section PUT /api/v1/chat-notes/{id}/key-insights
   */
  @PutMapping("/{id}/key-insights")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateKeyInsights(
      @PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateKeyInsightsRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateKeyInsights(id, request.getDescription(), request.getKeyPoints());
      return ResponseEntity.ok(ApiResponse.success("Key insights updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating key insights", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update key insights"));
    }
  }

  /**
   * Update follow-up explorations section PUT /api/v1/chat-notes/{id}/follow-up
   */
  @PutMapping("/{id}/follow-up")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateFollowUpExplorations(
      @PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateFollowUpRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateFollowUpExplorations(id, request.getDescription());
      return ResponseEntity
          .ok(ApiResponse.success("Follow-up explorations updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating follow-up explorations", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update follow-up explorations"));
    }
  }

  /**
   * Update references list PUT /api/v1/chat-notes/{id}/references
   */
  @PutMapping("/{id}/references")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateReferences(
      @PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateReferencesRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateReferences(id, request.getReferences());
      return ResponseEntity.ok(ApiResponse.success("References updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error updating references", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update references"));
    }
  }

  /**
   * Update artifact content by index PUT /api/v1/chat-notes/{id}/artifacts/{index}
   */
  @PutMapping("/{id}/artifacts/{index}")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateArtifactContent(
      @PathVariable String id, @PathVariable int index,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateContentRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateArtifactContent(id, index, request.getContent());
      return ResponseEntity
          .ok(ApiResponse.success("Artifact content updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error updating artifact content", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update artifact content"));
    }
  }

  /**
   * Update attachment content by index PUT /api/v1/chat-notes/{id}/attachments/{index}
   */
  @PutMapping("/{id}/attachments/{index}")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> updateAttachmentContent(
      @PathVariable String id, @PathVariable int index,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.UpdateContentRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.updateAttachmentContent(id, index, request.getContent());
      return ResponseEntity
          .ok(ApiResponse.success("Attachment content updated successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error updating attachment content", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update attachment content"));
    }
  }

  // ==================== Label Management ====================

  /**
   * Assign labels to a chat note POST /api/v1/chat-notes/{id}/labels
   */
  @PostMapping("/{id}/labels")
  @HxTrigger("labelAssigned")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> assignLabels(@PathVariable String id,
      @RequestBody @jakarta.validation.Valid me.moonote.app.chatkeep.dto.request.AssignLabelsRequest request) {
    try {
      ChatNoteDetailResponse response =
          chatNoteService.assignLabelsToNote(id, request.getLabelIds());
      return ResponseEntity.ok(ApiResponse.success("Labels assigned successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error assigning labels to chat note {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to assign labels"));
    }
  }

  /**
   * Remove a label from a chat note DELETE /api/v1/chat-notes/{id}/labels/{labelId}
   */
  @DeleteMapping("/{id}/labels/{labelId}")
  @HxTrigger("labelRemoved")
  public ResponseEntity<ApiResponse<ChatNoteDetailResponse>> removeLabel(@PathVariable String id,
      @PathVariable String labelId) {
    try {
      ChatNoteDetailResponse response = chatNoteService.removeLabelFromNote(id, labelId);
      return ResponseEntity.ok(ApiResponse.success("Label removed successfully", response));
    } catch (ChatNoteNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("Chat note not found: " + id));
    } catch (Exception e) {
      log.error("Error removing label {} from chat note {}", labelId, id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to remove label"));
    }
  }

  /**
   * Filter chat notes by labels GET /api/v1/chat-notes/filter/labels?labelIds=id1,id2&operator=AND
   */
  @GetMapping("/filter/labels")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> filterByLabels(
      @RequestParam java.util.List<String> labelIds,
      @RequestParam(defaultValue = "AND") String operator,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
      Page<ChatNoteResponse> chatNotes =
          chatNoteService.filterByLabels(labelIds, operator, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error filtering chat notes by labels", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to filter chat notes by labels"));
    }
  }

  /**
   * Filter active chat notes by labels for current user GET
   * /api/v1/chat-notes/filter/labels/active?labelIds=id1,id2&operator=OR
   */
  @GetMapping("/filter/labels/active")
  public ResponseEntity<ApiResponse<Page<ChatNoteResponse>>> filterActiveByLabels(
      @RequestParam java.util.List<String> labelIds,
      @RequestParam(defaultValue = "AND") String operator,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    try {
      String userId = me.moonote.app.chatkeep.security.SecurityUtils.getCurrentUserId();
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
      Page<ChatNoteResponse> chatNotes =
          chatNoteService.filterActiveByLabelsForUser(userId, labelIds, operator, pageable);
      return ResponseEntity.ok(ApiResponse.success(chatNotes));
    } catch (Exception e) {
      log.error("Error filtering active chat notes by labels", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to filter active chat notes by labels"));
    }
  }

}
