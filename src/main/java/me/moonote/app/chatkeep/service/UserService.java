package me.moonote.app.chatkeep.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.UpdateUserPreferencesRequest;
import me.moonote.app.chatkeep.dto.response.UserResponse;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.model.UserPreferences;
import me.moonote.app.chatkeep.repository.UserRepository;
import me.moonote.app.chatkeep.security.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  /**
   * Get current user information.
   *
   * @return UserResponse
   * @throws IllegalArgumentException if user not found
   */
  public UserResponse getCurrentUser() {
    String userId = SecurityUtils.getCurrentUserId();

    log.info("Fetching current user: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    return mapToResponse(user);
  }

  /**
   * Update user preferences (theme and/or language).
   *
   * @param request Update preferences request (theme and/or language can be null)
   * @return Updated UserResponse
   * @throws IllegalArgumentException if user not found
   */
  public UserResponse updateUserPreferences(UpdateUserPreferencesRequest request) {
    String userId = SecurityUtils.getCurrentUserId();

    log.info("Updating preferences for user: {} - theme: {}, language: {}", userId,
        request.getTheme(), request.getPreferredLanguage());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    // Get existing preferences or create new one
    UserPreferences preferences = user.getPreferences();
    if (preferences == null) {
      preferences = UserPreferences.builder().build();
    }

    // Update only non-null fields
    if (request.getTheme() != null) {
      preferences.setTheme(request.getTheme());
    }
    if (request.getPreferredLanguage() != null) {
      preferences.setPreferredLanguage(request.getPreferredLanguage());
    }

    user.setPreferences(preferences);
    User saved = userRepository.save(user);

    log.info("Preferences updated successfully for user: {}", userId);

    return mapToResponse(saved);
  }

  /**
   * Map User entity to UserResponse DTO.
   */
  private UserResponse mapToResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .anonymousUuid(user.getAnonymousUuid())
        .email(user.getEmail())
        .username(user.getUsername())
        .userType(user.getUserType() != null ? user.getUserType().toString() : null)
        .preferences(user.getPreferences())
        .registeredAt(user.getRegisteredAt())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }

}
