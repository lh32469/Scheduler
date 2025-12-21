package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

  private final UserRepository userRepository;

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/profile")
  public String editProfile(Authentication authentication, Model model, RedirectAttributes redirect) {
    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }
    String username = authentication.getName().trim().toLowerCase(Locale.ENGLISH);
    UserAccount user = userRepository.findByUsername(username);
    if (user == null) {
      redirect.addFlashAttribute("message", "Authenticated user not found.");
      redirect.addFlashAttribute("messageType", "error");
      return "redirect:/login";
    }

    if (!model.containsAttribute("form")) {
      AccountProfileForm form = new AccountProfileForm();
      form.setName(user.getName());
      form.setProfile(user.getProfile());
      // Pre-populate service types only for instructors
      if (hasRole(user, "ROLE_INSTRUCTOR")) {
        form.setServiceTypes(user.getServiceTypes());
      }
      model.addAttribute("form", form);
    }
    model.addAttribute("title", "My Account");
    model.addAttribute("username", user.getUsername());
    // Flag for conditional UI rendering
    model.addAttribute("isInstructor", hasRole(user, "ROLE_INSTRUCTOR"));
    return "account/profile";
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/profile")
  public String updateProfile(Authentication authentication,
                              AccountProfileForm form,
                              RedirectAttributes redirect) {
    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }
    String username = authentication.getName().trim().toLowerCase(Locale.ENGLISH);
    UserAccount user = userRepository.findByUsername(username);
    if (user == null) {
      redirect.addFlashAttribute("message", "Authenticated user not found.");
      redirect.addFlashAttribute("messageType", "error");
      return "redirect:/login";
    }

    // Validate
    String name = form != null ? safeTrim(form.getName()) : null;
    String profile = form != null ? safeTrim(form.getProfile()) : null;
    if (!StringUtils.hasText(name)) {
      redirect.addFlashAttribute("message", "Full name is required.");
      redirect.addFlashAttribute("messageType", "error");
      redirect.addFlashAttribute("form", form);
      return "redirect:/account/profile";
    }
    if (profile != null && profile.length() > 2000) {
      profile = profile.substring(0, 2000);
    }

    // Apply changes to current user's own account
    user.setName(name);
    user.setProfile(profile);
    // Only instructors can edit their service types; ignore otherwise
    if (hasRole(user, "ROLE_INSTRUCTOR") && form != null) {
      // Null-safe: allow clearing selection by setting empty list
      user.setServiceTypes(form.getServiceTypes());
    }
    userRepository.save(user);

    redirect.addFlashAttribute("message", "Your profile has been updated.");
    redirect.addFlashAttribute("messageType", "info");
    return "redirect:/account/profile";
  }

  private String safeTrim(String s) {
    return s == null ? null : s.trim();
  }

  private boolean hasRole(UserAccount user, String roleWithPrefix) {
    if (user == null || user.getRoles() == null) return false;
    // Accept entries with or without ROLE_ prefix in storage
    String target = roleWithPrefix;
    String alt = roleWithPrefix.startsWith("ROLE_") ? roleWithPrefix.substring(5) : ("ROLE_" + roleWithPrefix);
    for (String r : user.getRoles()) {
      if (r == null) continue;
      String rr = r.trim().toUpperCase(Locale.ENGLISH);
      if (rr.equalsIgnoreCase(target) || rr.equalsIgnoreCase(alt)) {
        return true;
      }
    }
    return false;
  }
}
