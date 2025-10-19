package me.moonote.app.chatkeep.controller.fragment;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.CreateLabelRequest;
import me.moonote.app.chatkeep.dto.request.UpdateLabelRequest;
import me.moonote.app.chatkeep.dto.response.LabelResponse;
import me.moonote.app.chatkeep.service.LabelService;
import me.moonote.app.chatkeep.repository.ChatNoteRepository;
import me.moonote.app.chatkeep.model.ChatNote;

/**
 * Fragment controller for label management via HTMX Returns HTML fragments rendered by Thymeleaf
 */
@Slf4j
@Controller
@RequestMapping("/fragments/labels")
@RequiredArgsConstructor
public class LabelFragmentController {

  private final LabelService labelService;
  private final ChatNoteRepository chatNoteRepository;

  /**
   * Render label list for sidebar GET /fragments/labels/list
   */
  @GetMapping("/list")
  @HxRequest
  public String getLabelList(Model model) {
    log.info("Loading label list fragment");

    List<LabelResponse> labels = labelService.getUserLabels();
    model.addAttribute("labels", labels);

    return "fragments/labels/label-list :: list";
  }

  /**
   * Render label management page GET /fragments/labels/management
   */
  @GetMapping("/management")
  public String getLabelManagement(Model model) {
    log.info("Loading label management page");

    List<LabelResponse> labels = labelService.getUserLabels();
    model.addAttribute("labels", labels);

    return "fragments/labels/label-management :: management";
  }

  /**
   * Render create label modal GET /fragments/labels/create-modal
   */
  @GetMapping("/create-modal")
  @HxRequest
  public String getCreateLabelModal(Model model) {
    log.info("Opening create label modal");

    // Predefined color palette
    List<String> colorPalette = List.of("#FF5733", "#33FF57", "#3357FF", "#FF33A1", "#FFC300",
        "#DAF7A6", "#C70039", "#900C3F", "#581845", "#1ABC9C");

    model.addAttribute("colors", colorPalette);

    return "fragments/labels/create-label-modal :: modal";
  }

  /**
   * Create a new label POST /fragments/labels Returns updated label list
   */
  @PostMapping
  @HxRequest
  @HxTrigger("labelCreated")
  public String createLabel(@RequestParam String name, @RequestParam String color, Model model) {
    log.info("Creating label: name={}, color={}", name, color);

    try {
      CreateLabelRequest request = CreateLabelRequest.builder().name(name).color(color).build();

      labelService.createLabel(request);

      // Return updated label list
      List<LabelResponse> labels = labelService.getUserLabels();
      model.addAttribute("labels", labels);

      return "fragments/labels/label-list :: list";

    } catch (IllegalArgumentException e) {
      log.warn("Label creation failed: {}", e.getMessage());
      model.addAttribute("error", e.getMessage());
      return "fragments/labels/error :: message";
    }
  }

  /**
   * Render edit label modal GET /fragments/labels/{id}/edit-modal
   */
  @GetMapping("/{id}/edit-modal")
  @HxRequest
  public String getEditLabelModal(@PathVariable String id, Model model) {
    log.info("Opening edit label modal for: {}", id);

    try {
      LabelResponse label = labelService.getLabelById(id);

      // Predefined color palette
      List<String> colorPalette = List.of("#FF5733", "#33FF57", "#3357FF", "#FF33A1", "#FFC300",
          "#DAF7A6", "#C70039", "#900C3F", "#581845", "#1ABC9C");

      model.addAttribute("label", label);
      model.addAttribute("colors", colorPalette);

      return "fragments/labels/edit-label-modal :: modal";

    } catch (IllegalArgumentException e) {
      log.warn("Label not found: {}", e.getMessage());
      model.addAttribute("error", e.getMessage());
      return "fragments/labels/error :: message";
    }
  }

  /**
   * Update a label PATCH /fragments/labels/{id} Returns updated label list
   */
  @PatchMapping("/{id}")
  @HxRequest
  @HxTrigger("labelUpdated")
  public String updateLabel(@PathVariable String id, @RequestParam String name,
      @RequestParam String color, Model model) {
    log.info("Updating label {}: name={}, color={}", id, name, color);

    try {
      UpdateLabelRequest request = UpdateLabelRequest.builder().name(name).color(color).build();

      labelService.updateLabel(id, request);

      // Return updated label list
      List<LabelResponse> labels = labelService.getUserLabels();
      model.addAttribute("labels", labels);

      return "fragments/labels/label-list :: list";

    } catch (IllegalArgumentException e) {
      log.warn("Label update failed: {}", e.getMessage());
      model.addAttribute("error", e.getMessage());
      return "fragments/labels/error :: message";
    }
  }

  /**
   * Render delete confirmation modal GET /fragments/labels/{id}/delete-confirm
   */
  @GetMapping("/{id}/delete-confirm")
  @HxRequest
  public String getDeleteConfirmModal(@PathVariable String id, Model model) {
    log.info("Opening delete confirmation for label: {}", id);

    try {
      LabelResponse label = labelService.getLabelById(id);
      model.addAttribute("label", label);

      return "fragments/labels/delete-confirm-modal :: modal";

    } catch (IllegalArgumentException e) {
      log.warn("Label not found: {}", e.getMessage());
      model.addAttribute("error", e.getMessage());
      return "fragments/labels/error :: message";
    }
  }

  /**
   * Delete a label DELETE /fragments/labels/{id} Returns updated label list
   */
  @DeleteMapping("/{id}")
  @HxRequest
  @HxTrigger("labelDeleted")
  public String deleteLabel(@PathVariable String id, Model model) {
    log.info("Deleting label: {}", id);

    try {
      labelService.deleteLabel(id);

      // Return updated label list
      List<LabelResponse> labels = labelService.getUserLabels();
      model.addAttribute("labels", labels);

      return "fragments/labels/label-list :: list";

    } catch (IllegalArgumentException e) {
      log.warn("Label deletion failed: {}", e.getMessage());
      model.addAttribute("error", e.getMessage());
      return "fragments/labels/error :: message";
    }
  }

  /**
   * Render label selector for a ChatNote GET /fragments/labels/selector?noteId=123
   */
  @GetMapping("/selector")
  @HxRequest
  public String getLabelSelector(@RequestParam String noteId, Model model) {
    log.info("Loading label selector for ChatNote: {}", noteId);

    List<LabelResponse> labels = labelService.getUserLabels();

    // Fetch note's current labels
    ChatNote chatNote = chatNoteRepository.findById(noteId).orElse(null);
    List<String> selectedLabelIds = (chatNote != null && chatNote.getLabelIds() != null)
        ? chatNote.getLabelIds()
        : List.of();

    model.addAttribute("noteId", noteId);
    model.addAttribute("labels", labels);
    model.addAttribute("selectedLabelIds", selectedLabelIds);

    return "fragments/labels/label-selector :: selector";
  }

}
