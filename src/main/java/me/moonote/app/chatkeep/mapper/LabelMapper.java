package me.moonote.app.chatkeep.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import me.moonote.app.chatkeep.dto.request.CreateLabelRequest;
import me.moonote.app.chatkeep.dto.response.LabelResponse;
import me.moonote.app.chatkeep.model.Label;

@Component
public class LabelMapper {

  /**
   * Convert CreateLabelRequest to Label entity.
   *
   * @param request Create label request
   * @param userId Owner user ID
   * @return Label entity
   */
  public Label toEntity(CreateLabelRequest request, String userId) {
    return Label.builder()
        .userId(userId)
        .name(request.getName().trim())
        .normalizedName(normalizeString(request.getName()))
        .color(request.getColor())
        .build();
  }

  /**
   * Convert Label entity to LabelResponse DTO.
   *
   * @param label Label entity
   * @param usageCount Number of ChatNotes using this label
   * @return LabelResponse DTO
   */
  public LabelResponse toResponse(Label label, int usageCount) {
    return LabelResponse.builder()
        .id(label.getId())
        .userId(label.getUserId())
        .name(label.getName()) // Display name (original casing)
        .color(label.getColor())
        .usageCount(usageCount)
        .createdAt(label.getCreatedAt())
        .updatedAt(label.getUpdatedAt())
        .build();
  }

  /**
   * Convert list of Labels to LabelResponse DTOs with usage counts.
   *
   * @param labels List of labels
   * @param usageCounts Map of label ID to usage count
   * @return List of LabelResponse DTOs
   */
  public List<LabelResponse> toResponseList(List<Label> labels, Map<String, Integer> usageCounts) {
    return labels.stream()
        .map(label -> toResponse(label, usageCounts.getOrDefault(label.getId(), 0)))
        .collect(Collectors.toList());
  }

  /**
   * Normalize string for case-insensitive uniqueness check.
   * Trims whitespace and converts to lowercase.
   *
   * @param str Input string
   * @return Normalized string (trimmed and lowercase)
   */
  public String normalizeString(String str) {
    return str != null ? str.trim().toLowerCase() : null;
  }

}
