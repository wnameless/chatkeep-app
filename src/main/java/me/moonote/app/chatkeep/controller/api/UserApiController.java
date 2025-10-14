package me.moonote.app.chatkeep.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.response.ApiResponse;
import me.moonote.app.chatkeep.dto.response.UserResponse;
import me.moonote.app.chatkeep.model.User;
import me.moonote.app.chatkeep.repository.UserRepository;
import me.moonote.app.chatkeep.security.SecurityUtils;

/**
 * RESTful API endpoints for user management Returns JSON responses for programmatic access
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserApiController {

  private final UserRepository userRepository;

  /**
   * Get current user information GET /api/v1/user/me Returns the currently authenticated user
   * (anonymous or OAuth2)
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
    try {
      String userId = SecurityUtils.getCurrentUserId();

      if (userId == null) {
        log.warn("No user found in security context");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Not authenticated"));
      }

      // Fetch user from database
      User user = userRepository.findById(userId).orElse(null);

      if (user == null) {
        log.error("User not found in database: {}", userId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("User not found"));
      }

      // Convert to response DTO
      UserResponse userResponse =
          UserResponse.builder().id(user.getId()).anonymousUuid(user.getAnonymousUuid())
              .email(user.getEmail()).username(user.getUsername())
              .userType(user.getUserType().toString()).registeredAt(user.getRegisteredAt())
              .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();

      log.info("Current user retrieved: {} ({})", user.getId(), user.getUserType());
      return ResponseEntity.ok(ApiResponse.success(userResponse));

    } catch (Exception e) {
      log.error("Error retrieving current user", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve user information"));
    }
  }

}
