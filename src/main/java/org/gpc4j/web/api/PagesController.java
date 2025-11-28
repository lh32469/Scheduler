package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple controller serving static informational pages: Privacy, Terms, Contact.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PagesController {

  @GetMapping("/privacy")
  public String privacy(Model model) {
    model.addAttribute("title", "Privacy Notice");
    return "privacy";
  }

  @GetMapping("/terms")
  public String terms(Model model) {
    model.addAttribute("title", "Terms & Conditions");
    return "terms";
  }

  @GetMapping("/contact")
  public String contact(Model model) {
    model.addAttribute("title", "Contact Us");
    return "contact";
  }
}
