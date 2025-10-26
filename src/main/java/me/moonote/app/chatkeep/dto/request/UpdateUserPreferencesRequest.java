package me.moonote.app.chatkeep.dto.request;

import static lombok.AccessLevel.PRIVATE;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UpdateUserPreferencesRequest {

  /**
   * UI theme preference: "light", "dark", or "auto"
   */
  @Pattern(regexp = "light|dark|auto", message = "Theme must be 'light', 'dark', or 'auto'")
  String theme;

  /**
   * Preferred language: "en", "zh_TW", or "zh_CN"
   */
  @Pattern(regexp = "en|zh_TW|zh_CN", message = "Language must be 'en', 'zh_TW', or 'zh_CN'")
  String preferredLanguage;

}
