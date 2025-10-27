package me.moonote.app.chatkeep.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.moonote.app.chatkeep.service.ChatNoteService;
import me.moonote.app.chatkeep.service.OAuth2ProviderService;

/**
 * Home Controller - Renders main pages Responsible for serving full HTML pages (not fragments)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ChatNoteService chatNoteService;
  private final OAuth2ProviderService oauth2ProviderService;

  /**
   * Home page - Shows active ChatNotes GET /
   */
  @GetMapping("/")
  public String home(Model model, HttpSession session) {
    log.info("Loading home page");
    String viewMode = (String) session.getAttribute("viewMode");
    if (viewMode == null) viewMode = "masonry";

    model.addAttribute("pageTitle", "ChatKeep - ChatNotes");
    model.addAttribute("currentView", "chatnotes");
    model.addAttribute("viewMode", viewMode);
    return "pages/index";
  }

  /**
   * Favorites page GET /favorites
   */
  @GetMapping("/favorites")
  public String favorites(Model model) {
    log.info("Loading favorites page");
    model.addAttribute("pageTitle", "ChatKeep - Favorites");
    model.addAttribute("currentView", "favorites");
    return "pages/index";
  }

  /**
   * Shared (Public) ChatNotes page GET /shared
   */
  @GetMapping("/shared")
  public String shared(Model model) {
    log.info("Loading shared page");
    model.addAttribute("pageTitle", "ChatKeep - Shared");
    model.addAttribute("currentView", "shared");
    return "pages/index";
  }

  /**
   * Archive page GET /archive
   */
  @GetMapping("/archive")
  public String archive(Model model) {
    log.info("Loading archive page");
    model.addAttribute("pageTitle", "ChatKeep - Archive");
    model.addAttribute("currentView", "archive");
    return "pages/index";
  }

  /**
   * Trash page GET /trash
   */
  @GetMapping("/trash")
  public String trash(Model model) {
    log.info("Loading trash page");
    model.addAttribute("pageTitle", "ChatKeep - Trash");
    model.addAttribute("currentView", "trash");
    return "pages/index";
  }

  /**
   * Public ChatNote sharing page GET /share/{id}
   */
  @GetMapping("/share/{id}")
  public String shareView(@PathVariable String id, Model model) {
    log.info("Loading public share view for ChatNote: {}", id);

    try {
      var note = chatNoteService.getChatNoteById(id);

      // Check if note is public
      if (!note.getIsPublic()) {
        log.warn("Attempted to access non-public ChatNote via share link: {}", id);
        model.addAttribute("error", "This ChatNote is not public");
        return "pages/error";
      }

      model.addAttribute("note", note);
      model.addAttribute("pageTitle", "ChatKeep - " + note.getTitle());
      return "pages/share";

    } catch (Exception e) {
      log.error("Error loading public ChatNote", e);
      model.addAttribute("error", "ChatNote not found");
      return "pages/error";
    }
  }

  /**
   * OAuth2 Login page GET /login
   */
  @GetMapping("/login")
  public String login(Model model) {
    log.info("Loading login page");
    model.addAttribute("pageTitle", "ChatKeep - Login");
    model.addAttribute("enabledProviders", oauth2ProviderService.getEnabledProviders());
    return "pages/login";
  }

}
