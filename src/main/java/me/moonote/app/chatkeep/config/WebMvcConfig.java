package me.moonote.app.chatkeep.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Web MVC configuration for internationalization (i18n).
 *
 * Supports locale detection from: 1. Query parameter (?lang=en, ?lang=zh_TW, ?lang=zh_CN) 2.
 * Session storage 3. Browser Accept-Language header
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /**
   * LocaleResolver that stores locale in session and falls back to Accept-Language header.
   */
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    return resolver;
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
