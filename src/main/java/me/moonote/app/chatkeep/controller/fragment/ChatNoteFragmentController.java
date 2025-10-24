package me.moonote.app.chatkeep.controller.fragment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.response.ChatNoteDetailLightResponse;
import me.moonote.app.chatkeep.dto.response.ChatNoteResponse;
import me.moonote.app.chatkeep.dto.response.LabelResponse;
import me.moonote.app.chatkeep.mapper.ChatNoteMapper;
import me.moonote.app.chatkeep.mapper.LabelMapper;
import me.moonote.app.chatkeep.model.Artifact;
import me.moonote.app.chatkeep.model.Attachment;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.Label;
import me.moonote.app.chatkeep.repository.ArtifactRepository;
import me.moonote.app.chatkeep.repository.AttachmentRepository;
import me.moonote.app.chatkeep.repository.ChatNoteRepository;
import me.moonote.app.chatkeep.repository.LabelRepository;
import me.moonote.app.chatkeep.security.SecurityUtils;
import me.moonote.app.chatkeep.service.ChatNoteService;

/**
 * Fragment controller for HTMX-based interactions Returns fully-rendered HTML fragments via
 * Thymeleaf Session stores view preferences (grid/list)
 */
@Slf4j
@Controller
@RequestMapping("/fragments")
@SessionAttributes("viewMode")
@RequiredArgsConstructor
public class ChatNoteFragmentController {

  private final ChatNoteService chatNoteService;
  private final ChatNoteRepository chatNoteRepository;
  private final ArtifactRepository artifactRepository;
  private final AttachmentRepository attachmentRepository;
  private final ChatNoteMapper chatNoteMapper;
  private final ObjectMapper objectMapper;
  private final LabelRepository labelRepository;
  private final LabelMapper labelMapper;

  // ==================== Main Grid Loading ====================

  /**
   * Load ChatNotes grid GET /fragments/chat-notes?view=masonry&filter=chatnotes
   * Supports infinite scroll via append parameter
   */
  @GetMapping("/chat-notes")
  public String getChatNotes(@RequestParam(required = false) String view,
      @RequestParam(defaultValue = "chatnotes") String filter,
      @RequestParam(required = false) String labelIds,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean append,
      Model model, HttpSession session) {
    log.info("Loading chat notes: view={}, filter={}, labelIds={}, search={}, page={}, append={}",
        view, filter, labelIds, search, page, append);

    // Store view preference in session
    if (view != null) {
      session.setAttribute("viewMode", view);
    }

    // Read view from session if not provided
    String currentView = (String) session.getAttribute("viewMode");
    if (currentView == null) currentView = "masonry";

    String userId = SecurityUtils.getCurrentUserId();

    if (userId == null) {
      log.warn("No authenticated user");
      model.addAttribute("notes", List.of());
      model.addAttribute("viewMode", currentView);
      model.addAttribute("filter", normalizeFilter(filter));
      return currentView.equals("list") ? "fragments/chat-note-cards-list"
          : "fragments/chat-note-cards";
    }

    // Load notes based on filter or label
    List<ChatNoteResponse> notes;
    if (labelIds != null && !labelIds.isEmpty()) {
      // Filter by label(s) - show only active notes
      // Parse comma-separated labelIds into list (supports multiple label filtering)
      List<String> labelIdList = Arrays.asList(labelIds.split(","));
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      notes = chatNoteService.filterActiveByLabelsForUser(userId, labelIdList, "OR", pageable)
          .getContent();
    } else {
      // Regular filter
      notes = loadNotesByFilter(filter, userId, page, size);
    }

    // Batch fetch all unique label IDs to avoid N+1 query
    Set<String> allLabelIds = notes.stream()
        .filter(note -> note.getLabelIds() != null)
        .flatMap(note -> note.getLabelIds().stream())
        .collect(Collectors.toSet());

    // Single batch query to fetch all labels
    Map<String, Label> labelMap = allLabelIds.isEmpty() ? Map.of()
        : labelRepository.findAllById(allLabelIds).stream()
            .collect(Collectors.toMap(Label::getId, label -> label));

    // Add content previews and labels
    notes = notes.stream().map(note -> {
      ChatNote entity = chatNoteRepository.findById(note.getId()).orElse(null);
      if (entity != null) {
        note.setContentPreview(chatNoteMapper.generateContentPreview(entity));
        // Populate labels using pre-fetched label map
        if (note.getLabelIds() != null && !note.getLabelIds().isEmpty()) {
          List<LabelResponse> labels = note.getLabelIds().stream()
              .map(labelMap::get)
              .filter(java.util.Objects::nonNull)
              .map(labelMapper::toResponse)
              .collect(Collectors.toList());
          note.setLabels(labels);
        }
      }
      return note;
    }).collect(Collectors.toList());

    // Normalize filter name for template
    String normalizedFilter = normalizeFilter(filter);

    // Detect if there are more results for infinite scroll
    boolean hasMore = notes.size() == size;

    model.addAttribute("notes", notes);
    model.addAttribute("viewMode", currentView);
    model.addAttribute("filter", normalizedFilter);
    model.addAttribute("hasMore", hasMore);
    model.addAttribute("currentPage", page);

    // Preserve current filter state for infinite scroll sentinel
    model.addAttribute("currentFilter", filter);
    model.addAttribute("currentLabelIds", labelIds != null ? labelIds : "");
    model.addAttribute("currentSearch", search != null ? search : "");

    // Return cards-only fragment when appending, full grid otherwise
    if (append) {
      return currentView.equals("list") ? "fragments/chat-note-cards-list :: cards-only"
          : "fragments/chat-note-cards :: cards-only";
    }

    return currentView.equals("list") ? "fragments/chat-note-cards-list :: list-cards"
        : "fragments/chat-note-cards :: cards";
  }

  /**
   * Get single chat note card GET /fragments/chat-note-card-single?id={id}
   */
  @GetMapping("/chat-note-card-single")
  public String getSingleCard(@RequestParam String id, Model model) {
    log.info("Loading single card for note: {}", id);

    try {
      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      Map<String, Object> noteData = buildNoteModel(entity);

      model.addAttribute("note", noteData);

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error loading single card for note {}", id, e);
      return "fragments/empty";
    }
  }

  /**
   * Helper: Load notes by filter type
   */
  private List<ChatNoteResponse> loadNotesByFilter(String filter, String userId, int page,
      int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    switch (filter) {
      case "favorites":
        return chatNoteService.getFavoriteChatNotes(userId, pageable).getContent();
      case "shared":
        return chatNoteService.getPublicChatNotes(pageable).getContent();
      case "archive":
        return chatNoteService.getArchivedChatNotes(userId);
      case "trash":
        return chatNoteService.getTrashedChatNotes(userId);
      case "chatnotes":
      default:
        return chatNoteService.getActiveChatNotes(userId, pageable).getContent();
    }
  }

  // ==================== CRUD Actions ====================

  /**
   * Toggle favorite POST /fragments/chat-notes/{id}/favorite?isFavorite=true
   */
  @PostMapping("/chat-notes/{id}/favorite")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String toggleFavorite(@PathVariable String id, @RequestParam Boolean isFavorite,
      Model model, HttpServletResponse response) {

    log.info("Toggle favorite: id={}, isFavorite={}", id, isFavorite);

    try {
      chatNoteService.toggleFavorite(id, isFavorite);

      // Load updated note
      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      Map<String, Object> noteData = buildNoteModel(entity);

      model.addAttribute("note", noteData);

      // Add toast trigger
      String message = isFavorite ? "Added to favorites" : "Removed from favorites";
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"" + message + "\",\"type\":\"success\"}}");

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error toggling favorite", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error updating favorite\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  /**
   * Archive/Unarchive POST /fragments/chat-notes/{id}/archive?isArchived=true
   */
  @PostMapping("/chat-notes/{id}/archive")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String toggleArchive(@PathVariable String id, @RequestParam Boolean isArchived,
      Model model, HttpServletResponse response) {

    log.info("Toggle archive: id={}, isArchived={}", id, isArchived);

    try {
      chatNoteService.updateArchiveStatus(id, isArchived);

      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      model.addAttribute("note", buildNoteModel(entity));

      String message = isArchived ? "Archived" : "Unarchived";
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"" + message + "\",\"type\":\"success\"}}");

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error toggling archive", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error updating archive\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  /**
   * Move to trash POST /fragments/chat-notes/{id}/trash
   */
  @PostMapping("/chat-notes/{id}/trash")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String moveToTrash(@PathVariable String id, Model model, HttpServletResponse response) {

    log.info("Move to trash: id={}", id);

    try {
      chatNoteService.moveToTrash(id);

      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      model.addAttribute("note", buildNoteModel(entity));

      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Moved to trash\",\"type\":\"success\"}}");

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error moving to trash", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error moving to trash\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  /**
   * Restore from trash POST /fragments/chat-notes/{id}/restore
   */
  @PostMapping("/chat-notes/{id}/restore")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String restoreFromTrash(@PathVariable String id, Model model,
      HttpServletResponse response) {

    log.info("Restore from trash: id={}", id);

    try {
      chatNoteService.restoreFromTrash(id);

      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      model.addAttribute("note", buildNoteModel(entity));

      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Restored from trash\",\"type\":\"success\"}}");

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error restoring from trash", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error restoring\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  /**
   * Toggle visibility POST /fragments/chat-notes/{id}/visibility?isPublic=true
   */
  @PostMapping("/chat-notes/{id}/visibility")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String toggleVisibility(@PathVariable String id, @RequestParam Boolean isPublic,
      Model model, HttpServletResponse response) {

    log.info("Toggle visibility: id={}, isPublic={}", id, isPublic);

    try {
      chatNoteService.updateVisibility(id, isPublic);

      ChatNote entity = chatNoteRepository.findById(id).orElseThrow();
      model.addAttribute("note", buildNoteModel(entity));

      String message = isPublic ? "Made public" : "Made private";
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"" + message + "\",\"type\":\"success\"}}");

      return "fragments/chat-note-card :: card";

    } catch (Exception e) {
      log.error("Error toggling visibility", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error updating visibility\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  /**
   * Permanently delete DELETE /fragments/chat-notes/{id}/permanent
   */
  @DeleteMapping("/chat-notes/{id}/permanent")
  @HxRequest
  @HxTrigger("noteUpdated")
  public String permanentlyDelete(@PathVariable String id, HttpServletResponse response) {

    log.info("Permanently delete: id={}", id);

    try {
      chatNoteService.permanentlyDeleteChatNote(id);

      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Permanently deleted\",\"type\":\"success\"}}");

      return "fragments/empty";

    } catch (Exception e) {
      log.error("Error permanently deleting", e);
      response.setHeader("HX-Trigger-After-Settle",
          "{\"showToast\":{\"message\":\"Error deleting\",\"type\":\"error\"}}");
      return "fragments/empty";
    }
  }

  // ==================== Search & Filter ====================

  /**
   * Search chat notes GET /fragments/search?query=keyword Comprehensive search across title, tags,
   * and content for user's active notes
   */
  @GetMapping("/search")
  public String search(@RequestParam String query, Model model, HttpSession session) {

    log.info("Search: query={}", query);

    String userId = SecurityUtils.getCurrentUserId();

    if (userId == null) {
      model.addAttribute("notes", List.of());
      model.addAttribute("viewMode", "masonry");
      model.addAttribute("filter", "search");
      return "fragments/chat-note-cards :: cards";
    }

    // If query is empty, return active notes (same as default view)
    if (query == null || query.trim().isEmpty()) {
      Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
      List<ChatNoteResponse> notes =
          chatNoteService.getActiveChatNotes(userId, pageable).getContent();

      // Batch fetch all unique label IDs to avoid N+1 query
      Set<String> allLabelIds = notes.stream()
          .filter(note -> note.getLabelIds() != null)
          .flatMap(note -> note.getLabelIds().stream())
          .collect(Collectors.toSet());

      // Single batch query to fetch all labels
      Map<String, Label> labelMap = allLabelIds.isEmpty() ? Map.of()
          : labelRepository.findAllById(allLabelIds).stream()
              .collect(Collectors.toMap(Label::getId, label -> label));

      // Add content previews and labels
      notes = notes.stream().map(note -> {
        ChatNote entity = chatNoteRepository.findById(note.getId()).orElse(null);
        if (entity != null) {
          note.setContentPreview(chatNoteMapper.generateContentPreview(entity));
          // Populate labels using pre-fetched label map
          if (note.getLabelIds() != null && !note.getLabelIds().isEmpty()) {
            List<LabelResponse> labels = note.getLabelIds().stream()
                .map(labelMap::get)
                .filter(java.util.Objects::nonNull)
                .map(labelMapper::toResponse)
                .collect(Collectors.toList());
            note.setLabels(labels);
          }
        }
        return note;
      }).collect(Collectors.toList());

      String viewMode = (String) session.getAttribute("viewMode");
      model.addAttribute("notes", notes);
      model.addAttribute("viewMode", viewMode != null ? viewMode : "masonry");
      model.addAttribute("filter", "active");

      return viewMode != null && viewMode.equals("list") ? "fragments/chat-note-cards-list :: cards"
          : "fragments/chat-note-cards :: cards";
    }

    try {
      // Use comprehensive search: title, tags, and content
      List<ChatNoteResponse> results = chatNoteService.searchUserChatNotes(userId, query);

      // Batch fetch all unique label IDs to avoid N+1 query
      Set<String> allLabelIds = results.stream()
          .filter(note -> note.getLabelIds() != null)
          .flatMap(note -> note.getLabelIds().stream())
          .collect(Collectors.toSet());

      // Single batch query to fetch all labels
      Map<String, Label> labelMap = allLabelIds.isEmpty() ? Map.of()
          : labelRepository.findAllById(allLabelIds).stream()
              .collect(Collectors.toMap(Label::getId, label -> label));

      // Add content previews and labels
      results = results.stream().map(note -> {
        ChatNote entity = chatNoteRepository.findById(note.getId()).orElse(null);
        if (entity != null) {
          note.setContentPreview(chatNoteMapper.generateContentPreview(entity));
          // Populate labels using pre-fetched label map
          if (note.getLabelIds() != null && !note.getLabelIds().isEmpty()) {
            List<LabelResponse> labels = note.getLabelIds().stream()
                .map(labelMap::get)
                .filter(java.util.Objects::nonNull)
                .map(labelMapper::toResponse)
                .collect(Collectors.toList());
            note.setLabels(labels);
          }
        }
        return note;
      }).collect(Collectors.toList());

      String viewMode = (String) session.getAttribute("viewMode");
      model.addAttribute("notes", results);
      model.addAttribute("viewMode", viewMode != null ? viewMode : "masonry");
      model.addAttribute("filter", "search");

      return viewMode != null && viewMode.equals("list") ? "fragments/chat-note-cards-list :: cards"
          : "fragments/chat-note-cards :: cards";

    } catch (Exception e) {
      log.error("Error searching", e);
      model.addAttribute("notes", List.of());
      model.addAttribute("filter", "search");
      return "fragments/chat-note-cards :: cards";
    }
  }

  // ==================== Modal ====================

  /**
   * Get chat note modal GET /fragments/chat-note-modal?id={id}
   */
  @GetMapping("/chat-note-modal")
  public String getChatNoteModal(@RequestParam String id, Model model) {

    log.info("Load modal for ID: {}", id);

    try {
      ChatNoteDetailLightResponse note = chatNoteService.getChatNoteById(id);

      // Set additional fields
      note.setPlatform(note.getOriginalPlatform());

      // Serialize references to JSON string for template
      String referencesJson = "[]";
      if (note.getSummary() != null && note.getSummary().getReferences() != null
          && !note.getSummary().getReferences().isEmpty()) {
        try {
          referencesJson = objectMapper.writeValueAsString(note.getSummary().getReferences());
        } catch (Exception e) {
          log.warn("Failed to serialize references to JSON", e);
        }
      }

      model.addAttribute("note", note);
      model.addAttribute("conversationContent", note.getConversationContent());
      model.addAttribute("referencesJson", referencesJson);

      // Fetch artifacts and attachments from separate collections
      List<Artifact> artifacts = artifactRepository.findByChatNoteIdOrderByCreatedAtDesc(id);
      List<Attachment> attachments = attachmentRepository.findByChatNoteIdOrderByCreatedAtDesc(id);

      model.addAttribute("artifacts", artifacts);
      model.addAttribute("attachments", attachments);

      return "fragments/chat-note-modal :: modal";

    } catch (Exception e) {
      log.error("Error loading modal", e);
      model.addAttribute("error", "Failed to load ChatNote: " + e.getMessage());
      return "fragments/error";
    }
  }

  /**
   * Get artifact content GET /fragments/artifact/{noteId}/{index}
   */
  @GetMapping("/artifact/{noteId}/{index}")
  public String getArtifact(@PathVariable String noteId, @PathVariable int index, Model model) {

    log.info("Get artifact: noteId={}, index={}", noteId, index);

    try {
      Artifact artifact = chatNoteService.getArtifactContent(noteId, index);
      model.addAttribute("artifact", artifact);
      return "fragments/artifact-content";

    } catch (Exception e) {
      log.error("Error loading artifact", e);
      model.addAttribute("error", "Failed to load artifact");
      return "fragments/error";
    }
  }

  /**
   * Get attachment content GET /fragments/attachment/{noteId}/{index}
   */
  @GetMapping("/attachment/{noteId}/{index}")
  public String getAttachment(@PathVariable String noteId, @PathVariable int index, Model model) {

    log.info("Get attachment: noteId={}, index={}", noteId, index);

    try {
      Attachment attachment = chatNoteService.getAttachmentContent(noteId, index);
      model.addAttribute("attachment", attachment);
      return "fragments/attachment-content";

    } catch (Exception e) {
      log.error("Error loading attachment", e);
      model.addAttribute("error", "Failed to load attachment");
      return "fragments/error";
    }
  }

  // ==================== Helper Methods ====================

  /**
   * Normalize filter name for template consistency
   */
  private String normalizeFilter(String filter) {
    switch (filter) {
      case "chatnotes":
        return "active";
      case "archive":
        return "archived";
      default:
        return filter;
    }
  }

  /**
   * Build note model for Thymeleaf template
   */
  private Map<String, Object> buildNoteModel(ChatNote entity) {
    Map<String, Object> noteData = new HashMap<>();
    noteData.put("id", entity.getId());
    noteData.put("title", entity.getTitle());
    noteData.put("conversationDate", entity.getConversationDate());
    noteData.put("contentPreview", chatNoteMapper.generateContentPreview(entity));
    noteData.put("tags", entity.getTags() != null ? entity.getTags() : List.of());
    noteData.put("labelIds", entity.getLabelIds() != null ? entity.getLabelIds() : List.of());

    // Populate labels with full details (batch fetch to avoid N+1 query)
    if (entity.getLabelIds() != null && !entity.getLabelIds().isEmpty()) {
      List<LabelResponse> labels = labelRepository.findAllById(entity.getLabelIds()).stream()
          .map(labelMapper::toResponse)
          .collect(Collectors.toList());
      noteData.put("labels", labels);
    } else {
      noteData.put("labels", List.of());
    }

    noteData.put("isFavorite", entity.getIsFavorite() != null ? entity.getIsFavorite() : false);
    noteData.put("isArchived", entity.getIsArchived() != null ? entity.getIsArchived() : false);
    noteData.put("isPublic", entity.getIsPublic() != null ? entity.getIsPublic() : false);
    noteData.put("isTrashed", entity.getIsTrashed() != null ? entity.getIsTrashed() : false);

    // Use counts from ChatNote entity (already stored there)
    noteData.put("artifactCount", entity.getArtifactCount() != null ? entity.getArtifactCount() : 0);
    noteData.put("attachmentCount",
        entity.getAttachmentCount() != null ? entity.getAttachmentCount() : 0);
    return noteData;
  }

  /**
   * Serve the paste archive modal fragment
   */
  @GetMapping("/paste-archive-modal")
  public String getPasteArchiveModal() {
    return "fragments/paste-archive-modal :: modal";
  }

}
