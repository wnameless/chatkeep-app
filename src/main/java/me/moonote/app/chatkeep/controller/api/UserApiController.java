package me.moonote.app.chatkeep.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.dto.request.UpdateUserPreferencesRequest;
import me.moonote.app.chatkeep.dto.response.ApiResponse;
import me.moonote.app.chatkeep.dto.response.UserResponse;
import me.moonote.app.chatkeep.service.UserService;

/**
 * RESTful API endpoints for user management Returns JSON responses for programmatic access
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserApiController {

  private final UserService userService;

  /**
   * Get current user information GET /api/v1/user/me Returns the currently authenticated user
   * (anonymous or OAuth2)
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
    try {
      UserResponse userResponse = userService.getCurrentUser();
      log.info("Current user retrieved: {}", userResponse.getId());
      return ResponseEntity.ok(ApiResponse.success(userResponse));

    } catch (IllegalArgumentException e) {
      log.error("User not found", e);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));

    } catch (Exception e) {
      log.error("Error retrieving current user", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve user information"));
    }
  }

  /**
   * Update user preferences (theme and/or language) PATCH /api/v1/user/preferences
   *
   * @param request Update preferences request (theme and/or language can be null)
   * @return Updated user information
   */
  @PatchMapping("/preferences")
  public ResponseEntity<ApiResponse<UserResponse>> updatePreferences(
      @Valid @RequestBody UpdateUserPreferencesRequest request) {
    try {
      UserResponse userResponse = userService.updateUserPreferences(request);
      log.info("User preferences updated successfully: {}", userResponse.getId());
      return ResponseEntity.ok(ApiResponse.success(userResponse));

    } catch (IllegalArgumentException e) {
      log.error("Error updating preferences", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));

    } catch (Exception e) {
      log.error("Error updating user preferences", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update preferences"));
    }
  }

}
