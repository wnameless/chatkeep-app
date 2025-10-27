package me.moonote.app.chatkeep.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * User preferences for UI customization (theme, language, etc.).
 *
 * This is an embedded document within the User entity, not a separate collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UserPreferences {

  /**
   * UI theme preference: "light", "dark", or "auto" (system preference).
   */
  @Builder.Default
  String theme = "auto";

  /**
   * Preferred language for UI: "en", "zh_TW", or "zh_CN".
   *
   * If null, the application will use the browser's Accept-Language header.
   */
  @Builder.Default
  String preferredLanguage = "en";

}
