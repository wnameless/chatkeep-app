package me.moonote.app.chatkeep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Web MVC configuration for internationalization (i18n).
 *
 * Supports locale detection from:
 * 1. Query parameter (?lang=en, ?lang=zh_TW, ?lang=zh_CN)
 * 2. Cookie storage (1 year persistence)
 * 3. Browser Accept-Language header auto-detection
 * 4. Defaults to English
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /**
   * Custom LocaleResolver that:
   * - Stores locale in cookie (persists for 1 year)
   * - Auto-detects from Accept-Language header
   * - Maps browser locales to supported languages (en, zh-TW, zh-CN)
   */
  @Bean
  public LocaleResolver localeResolver() {
    return new CustomLocaleResolver();
  }

  /**
   * Interceptor to change locale based on 'lang' query parameter.
   *
   * Examples: - /: English (default) - /?lang=en: English - /?lang=zh_TW: Traditional Chinese -
   * /?lang=zh_CN: Simplified Chinese
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }

}
