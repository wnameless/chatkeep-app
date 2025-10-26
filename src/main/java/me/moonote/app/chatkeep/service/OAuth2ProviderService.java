package me.moonote.app.chatkeep.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to detect and provide information about configured OAuth2 providers.
 *
 * This service checks which OAuth2 providers (Cognito, Google, GitHub, Facebook, etc.) have valid
 * Spring Security configurations and are available for user authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ProviderService {

  private final ClientRegistrationRepository clientRegistrationRepository;

  /**
   * Get list of all OAuth2 provider registration IDs that are properly configured.
   *
   * @return List of provider IDs (e.g., "cognito", "google", "github", "facebook")
   */
  public List<String> getEnabledProviders() {
    List<String> enabledProviders = new ArrayList<>();

    // Common provider IDs to check
    String[] knownProviders = {"cognito", "google", "github", "facebook"};

    for (String providerId : knownProviders) {
      if (isProviderConfigured(providerId)) {
        enabledProviders.add(providerId);
        log.debug("OAuth2 provider '{}' is configured and enabled", providerId);
      }
    }

    log.info("Enabled OAuth2 providers: {}", enabledProviders);
    return enabledProviders;
  }

  /**
   * Check if a specific OAuth2 provider is configured.
   *
   * @param providerId The provider registration ID (e.g., "google", "github")
   * @return true if provider has valid configuration, false otherwise
   */
  public boolean isProviderConfigured(String providerId) {
    try {
      ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(providerId);
      return registration != null;
    } catch (Exception e) {
      log.debug("Provider '{}' is not configured: {}", providerId, e.getMessage());
      return false;
    }
  }

}
