package me.moonote.app.chatkeep.mapper;

import java.util.List;
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
   * @return LabelResponse DTO
   */
  public LabelResponse toResponse(Label label) {
    return LabelResponse.builder()
        .id(label.getId())
        .userId(label.getUserId())
        .name(label.getName()) // Display name (original casing)
        .color(label.getColor())
        .createdAt(label.getCreatedAt())
        .updatedAt(label.getUpdatedAt())
        .build();
  }

  /**
   * Convert list of Labels to LabelResponse DTOs.
   *
   * @param labels List of labels
   * @return List of LabelResponse DTOs
   */
  public List<LabelResponse> toResponseList(List<Label> labels) {
    return labels.stream()
        .map(this::toResponse)
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
