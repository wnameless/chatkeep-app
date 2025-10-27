package me.moonote.app.chatkeep.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom LocaleResolver that:
 * 1. Checks cookie for stored language preference
 * 2. Falls back to Accept-Language header auto-detection
 * 3. Maps browser locales to supported languages (en, zh-TW, zh-CN)
 * 4. Defaults to English if no match
 */
@Slf4j
public class CustomLocaleResolver extends CookieLocaleResolver {

  private static final List<Locale> SUPPORTED_LOCALES = List.of(
      Locale.ENGLISH, // en
      Locale.TRADITIONAL_CHINESE, // zh-TW
      Locale.SIMPLIFIED_CHINESE // zh-CN
  );

  public CustomLocaleResolver() {
    super("LOCALE"); // Cookie name
    setDefaultLocale(Locale.ENGLISH);
    setCookieMaxAge(Duration.ofDays(365)); // 1 year
    setCookieHttpOnly(true);
    setCookieSameSite("Lax");
  }

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    // 1. Check cookie first
    Locale cookieLocale = (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
    if (cookieLocale != null) {
      log.debug("Using locale from cookie: {}", cookieLocale);
      return cookieLocale;
    }

    // 2. Try to get from cookie using parent implementation
    Locale locale = super.resolveLocale(request);
    if (locale != null && !locale.equals(getDefaultLocale())) {
      log.debug("Using locale from cookie (via parent): {}", locale);
      return locale;
    }

    // 3. Auto-detect from Accept-Language header
    Locale browserLocale = detectBrowserLocale(request);
    if (browserLocale != null) {
      log.debug("Auto-detected locale from browser: {}", browserLocale);
      return browserLocale;
    }

    // 4. Fallback to default
    log.debug("Using default locale: {}", getDefaultLocale());
    return getDefaultLocale();
  }

  /**
   * Auto-detect browser language from Accept-Language header.
   * Maps to closest supported language.
   *
   * @param request HTTP request
   * @return Matched supported locale, or null if no match
   */
  @Nullable
  private Locale detectBrowserLocale(HttpServletRequest request) {
    // Get preferred locales from Accept-Language header
    List<Locale> browserLocales = new ArrayList<>();
    Enumeration<Locale> localeEnum = request.getLocales();
    while (localeEnum.hasMoreElements()) {
      browserLocales.add(localeEnum.nextElement());
    }

    if (browserLocales.isEmpty()) {
      return null;
    }

    log.debug("Browser requested locales: {}", browserLocales);

    // Try exact match first (language + country)
    for (Locale browserLocale : browserLocales) {
      for (Locale supportedLocale : SUPPORTED_LOCALES) {
        if (matchesExactly(browserLocale, supportedLocale)) {
          log.debug("Exact locale match: {} -> {}", browserLocale, supportedLocale);
          return supportedLocale;
        }
      }
    }

    // Try language-only match (ignore country)
    for (Locale browserLocale : browserLocales) {
      for (Locale supportedLocale : SUPPORTED_LOCALES) {
        if (matchesLanguage(browserLocale, supportedLocale)) {
          log.debug("Language match: {} -> {}", browserLocale, supportedLocale);
          return supportedLocale;
        }
      }
    }

    return null;
  }

  /**
   * Check if two locales match exactly (language + country).
   */
  private boolean matchesExactly(Locale locale1, Locale locale2) {
    return locale1.getLanguage().equalsIgnoreCase(locale2.getLanguage())
        && locale1.getCountry().equalsIgnoreCase(locale2.getCountry());
  }

  /**
   * Check if two locales match by language only.
   */
  private boolean matchesLanguage(Locale locale1, Locale locale2) {
    return locale1.getLanguage().equalsIgnoreCase(locale2.getLanguage());
  }

}
