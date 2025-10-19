package me.moonote.app.chatkeep.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.CreateLabelRequest;
import me.moonote.app.chatkeep.dto.request.UpdateLabelRequest;
import me.moonote.app.chatkeep.dto.response.LabelResponse;
import me.moonote.app.chatkeep.mapper.LabelMapper;
import me.moonote.app.chatkeep.model.ChatNote;
import me.moonote.app.chatkeep.model.Label;
import me.moonote.app.chatkeep.repository.ChatNoteRepository;
import me.moonote.app.chatkeep.repository.LabelRepository;
import me.moonote.app.chatkeep.security.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

  private final LabelRepository labelRepository;
  private final ChatNoteRepository chatNoteRepository;
  private final LabelMapper labelMapper;

  /**
   * Create a new label with case-insensitive uniqueness check.
   *
   * @param request Create label request
   * @return LabelResponse
   * @throws IllegalArgumentException if label name already exists (case-insensitive)
   */
  public LabelResponse createLabel(CreateLabelRequest request) {
    String userId = SecurityUtils.getCurrentUserId();
    String normalizedName = labelMapper.normalizeString(request.getName());

    log.info("Creating label '{}' for user: {}", request.getName(), userId);

    // Check case-insensitive uniqueness
    if (labelRepository.existsByUserIdAndNormalizedName(userId, normalizedName)) {
      log.warn("Label '{}' already exists for user: {} (case-insensitive)", request.getName(),
          userId);
      throw new IllegalArgumentException(
          "Label '" + request.getName() + "' already exists (case-insensitive)");
    }

    // Create and save label
    Label label = labelMapper.toEntity(request, userId);
    Label saved = labelRepository.save(label);

    log.info("Label '{}' created successfully with id: {}", saved.getName(), saved.getId());

    return labelMapper.toResponse(saved);
  }

  /**
   * Get all labels for the current user.
   *
   * @return List of LabelResponse
   */
  public List<LabelResponse> getUserLabels() {
    String userId = SecurityUtils.getCurrentUserId();

    log.info("Fetching labels for user: {}", userId);

    List<Label> labels = labelRepository.findByUserId(userId);

    return labelMapper.toResponseList(labels);
  }

  /**
   * Get a label by ID, verifying user ownership.
   *
   * @param id Label ID
   * @return LabelResponse
   * @throws IllegalArgumentException if label not found or not owned by current user
   */
  public LabelResponse getLabelById(String id) {
    String userId = SecurityUtils.getCurrentUserId();

    log.info("Fetching label {} for user: {}", id, userId);

    Label label = labelRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Label not found: " + id));

    // Verify ownership
    if (!label.getUserId().equals(userId)) {
      log.warn("User {} attempted to access label {} owned by {}", userId, id, label.getUserId());
      throw new IllegalArgumentException("You do not have permission to access this label");
    }

    return labelMapper.toResponse(label);
  }

  /**
   * Update a label with case-insensitive uniqueness check.
   *
   * @param id Label ID
   * @param request Update label request
   * @return LabelResponse
   * @throws IllegalArgumentException if label not found, not owned, or duplicate name
   */
  public LabelResponse updateLabel(String id, UpdateLabelRequest request) {
    String userId = SecurityUtils.getCurrentUserId();
    String normalizedName = labelMapper.normalizeString(request.getName());

    log.info("Updating label {} for user: {}", id, userId);

    Label label = labelRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Label not found: " + id));

    // Verify ownership
    if (!label.getUserId().equals(userId)) {
      log.warn("User {} attempted to update label {} owned by {}", userId, id, label.getUserId());
      throw new IllegalArgumentException("You do not have permission to update this label");
    }

    // Check case-insensitive uniqueness (allow same name with different casing)
    if (!label.getNormalizedName().equals(normalizedName)
        && labelRepository.existsByUserIdAndNormalizedName(userId, normalizedName)) {
      log.warn("Label '{}' already exists for user: {} (case-insensitive)", request.getName(),
          userId);
      throw new IllegalArgumentException(
          "Label '" + request.getName() + "' already exists (case-insensitive)");
    }

    // Update fields
    label.setName(request.getName().trim());
    label.setNormalizedName(normalizedName);
    label.setColor(request.getColor());

    Label updated = labelRepository.save(label);

    log.info("Label '{}' updated successfully", updated.getName());

    return labelMapper.toResponse(updated);
  }

  /**
   * Delete a label and remove it from all ChatNotes (cascade delete).
   *
   * @param id Label ID
   * @throws IllegalArgumentException if label not found or not owned by current user
   */
  public void deleteLabel(String id) {
    String userId = SecurityUtils.getCurrentUserId();

    log.info("Deleting label {} for user: {}", id, userId);

    Label label = labelRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Label not found: " + id));

    // Verify ownership
    if (!label.getUserId().equals(userId)) {
      log.warn("User {} attempted to delete label {} owned by {}", userId, id, label.getUserId());
      throw new IllegalArgumentException("You do not have permission to delete this label");
    }

    // Find all ChatNotes with this label and remove the label ID
    List<ChatNote> chatNotes = chatNoteRepository.findByLabelIdsContaining(id);

    log.info("Removing label {} from {} ChatNotes", id, chatNotes.size());

    // Remove label from all notes and batch save (avoid N+1 updates)
    chatNotes.forEach(chatNote -> chatNote.getLabelIds().remove(id));
    chatNoteRepository.saveAll(chatNotes);

    // Delete the label
    labelRepository.deleteById(id);

    log.info("Label '{}' deleted successfully", label.getName());
  }

}
