package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin-only pages for user management.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminUserController {

  private final RavenUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @GetMapping("/users/new")
  public String newUserForm() {
    return "users/new"; // resolves to src/main/resources/templates/users/new.html
  }

  /**
   * Processes a request to create a new user account.
   * Validates the user input, checks for duplicate usernames, hashes the password,
   * assigns default roles if none are provided, and saves the user to the repository.
   * Adds appropriate success or error messages to the redirect attributes.
   *
   * @param user               the {@code UserAccount} object containing user details
   *                           such as username, password, and roles
   * @param redirectAttributes the {@code RedirectAttributes} object used to pass
   *                           messages to the redirected view
   * @return a URL string indicating the redirection path, typically redirecting the
   * client to the "new user" form
   */
  @PostMapping("/users")
  public String createUser(UserAccount user,
                           RedirectAttributes redirectAttributes) {
    try {
      String username = user.getUsername();
      String rawPassword = user.getPassword();

      // Basic validation
      if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
        redirectAttributes.addFlashAttribute("message",
                                             "Username and password are required.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/admin/users/new";
      }

      // Normalize roles
      List<String> roles = user.getRoles();
      if (roles == null || roles.isEmpty()) {
        user.setRoles(List.of("USER"));
      }

      // Check for existing user
      UserAccount existing = userRepository.findByUsername(username);
      if (existing != null) {
        redirectAttributes.addFlashAttribute("message",
                                             "User already exists: " + username);
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/admin/users/new";
      }

      // Hash password and clear plaintext
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
      user.setPassword(null);
      user.setEnabled(true);
      user.setAccountNonLocked(true);

      String id = userRepository.save(user);
      log.info("Created new user id={} username={}", id, username);

      redirectAttributes.addFlashAttribute("message", "User created: " + username);
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/admin/users/new";
    } catch (Exception e) {
      log.error("Failed to create user: {}", e.toString());
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to create user. Please try again.");
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/admin/users/new";
    }
  }

}
