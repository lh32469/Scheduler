package org.gpc4j.web.security;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ADMIN-only dashboard for managing users and the Banner content.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

  private final IDocumentSession session;
  private final UserRepository userRepository;
  private final org.springframework.security.crypto.password.PasswordEncoder
      passwordEncoder;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public String root() {
    return "redirect:/admin/users";
  }

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public String users(Model model) {
    List<UserAccount> accounts;
    // Load up to 1000 users for the admin table. Adjust as needed.
    accounts = session.query(UserAccount.class)
                      .take(1000)
                      .toList();

    AdminUsersForm form = new AdminUsersForm();
    List<AdminUserRow> rows = new ArrayList<>();
    for (UserAccount ua : accounts) {
      AdminUserRow row = new AdminUserRow();
      row.setId(ua.getId());
      row.setName(ua.getName());
      row.setUsername(ua.getUsername());
      row.setEnabled(ua.isEnabled());
      // Normalize roles for UI: strip ROLE_ prefix
      List<String> roles = (ua.getRoles() == null ? List.<String>of() : ua.getRoles())
          .stream()
          .map(String::valueOf)
          .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
          .collect(Collectors.toList());
      row.setRoles(roles);
      rows.add(row);
    }
    form.setUsers(rows);

    model.addAttribute("form", form);
    model.addAttribute("title", "Admin · Users");
    return "admin/users";
  }

  /**
   * Render the Create User form.
   */
  @GetMapping("/users/new")
  @PreAuthorize("hasRole('ADMIN')")
  public String newUser(Model model) {
    if (!model.containsAttribute("userForm")) {
      model.addAttribute("userForm", new UserAccount());
    }
    model.addAttribute("title", "Admin · Create User");
    return "users/new";
  }

  /**
   * Create a new user account. Uses distinct path to avoid collision with bulk update
   * POST /admin/users.
   */
  @PostMapping("/users/new")
  @PreAuthorize("hasRole('ADMIN')")
  public String createUser(@ModelAttribute("userForm") UserAccount user,
                           RedirectAttributes redirectAttributes) {
    try {
      String username = user.getUsername();
      String rawPassword = user.getPassword();

      if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
        redirectAttributes.addFlashAttribute("message",
                                             "Username and password are required.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        redirectAttributes.addFlashAttribute("userForm", user);
        return "redirect:/admin/users/new";
      }

      // Check if user already exists
      UserAccount existing = userRepository.findByUsername(username);
      if (existing != null) {
        redirectAttributes.addFlashAttribute("message",
                                             "User already exists: " + username);
        redirectAttributes.addFlashAttribute("messageType", "error");
        redirectAttributes.addFlashAttribute("userForm", user);
        return "redirect:/admin/users/new";
      }

      // Normalize roles to ROLE_*
      List<String> roles = user.getRoles();
      if (roles == null || roles.isEmpty()) {
        roles = new java.util.ArrayList<>();
      }
      roles = roles.stream()
                   .filter(java.util.Objects::nonNull)
                   .map(String::trim)
                   .filter(s -> !s.isEmpty())
                   .map(String::toUpperCase)
                   .map(r -> r.startsWith("ROLE_") ? r : ("ROLE_" + r))
                   .distinct()
                   .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
      if (roles.isEmpty()) {
        roles.add("ROLE_USER");
      }
      user.setRoles(roles);

      // Hash password and set flags
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
      user.setPassword(null);
      if (user.getName() == null || user.getName().isBlank()) {
        user.setName("Blank");
      }
      user.setEnabled(true);
      user.setAccountNonLocked(true);

      String id = userRepository.save(user);
      log.info("Admin created user id={} username={}", id, username);

      redirectAttributes.addFlashAttribute("message", "User created: " + username);
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/admin/users/new";
    } catch (Exception e) {
      log.error("Failed to create user", e);
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to create user. Please try again.");
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/admin/users/new";
    }
  }

  @PostMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public String updateUsers(@ModelAttribute("form") AdminUsersForm form,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
    if (form == null || CollectionUtils.isEmpty(form.getUsers())) {
      redirectAttributes.addFlashAttribute("message", "No updates to apply.");
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/admin/users";
    }

    String currentUsername = authentication != null ? authentication.getName() : null;
    UserAccount current =
        (currentUsername != null) ? userRepository.findByUsername(currentUsername) : null;
    Set<String> warnings = new HashSet<>();

    try {
      for (AdminUserRow row : form.getUsers()) {
        if (row.getId() == null) {
          continue;
        }
        UserAccount ua = session.load(UserAccount.class, row.getId());
        if (ua == null) {
          continue;
        }

        // Update basic fields
        ua.setName(row.getName());

        // Compute normalized roles with ROLE_ prefix
        List<String> newRoles =
            (row.getRoles() == null ? List.<String>of() : row.getRoles())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(r -> r.startsWith("ROLE_") ? r : ("ROLE_" + r))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        // Ensure at least ROLE_USER
        if (newRoles.isEmpty()) {
          newRoles.add("ROLE_USER");
        }

        // Prevent self-demotion: if editing own account, do not allow removing ROLE_ADMIN
        if (current != null && ua.getId().equals(current.getId())) {
          boolean containsAdmin = newRoles.contains("ROLE_ADMIN");
          if (!containsAdmin) {
            warnings.add(
                "Cannot remove ROLE_ADMIN from your own account. Change skipped for " + ua.getUsername());
            // Keep existing roles for this user
          } else {
            ua.setRoles(newRoles);
          }
        } else {
          ua.setRoles(newRoles);
        }

        ua.setEnabled(row.isEnabled());
      }
      session.saveChanges();
    } catch (Exception e) {
      log.error("Failed to update users", e);
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to update users: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/admin/users";
    }

    String msg = "User updates applied." + (!warnings.isEmpty() ?
        " " + String.join(" ", warnings) :
        "");
    redirectAttributes.addFlashAttribute("message", msg);
    redirectAttributes.addFlashAttribute("messageType",
                                         warnings.isEmpty() ? "info" : "error");
    return "redirect:/admin/users";
  }

  @GetMapping("/banner")
  @PreAuthorize("hasRole('ADMIN')")
  public String banner(Model model) {
    Banner banner;
    banner = session.query(Banner.class).firstOrDefault();
    if (banner == null) {
      banner = new Banner();
    }

    model.addAttribute("banner", banner);
    model.addAttribute("title", "Admin · Banner");
    return "admin/banner";
  }

  @PostMapping("/banner")
  @PreAuthorize("hasRole('ADMIN')")
  public String saveBanner(@ModelAttribute Banner banner,
                           RedirectAttributes redirectAttributes) {
    try {
      Banner existing = session.query(Banner.class).firstOrDefault();
      if (existing == null) {
        // Create new
        session.store(banner);
      } else {
        existing.setCompanyName(banner.getCompanyName());
        existing.setTabTitle(banner.getTabTitle());
        existing.setTitle(banner.getTitle());
        existing.setSubTitle(banner.getSubTitle());
      }
      session.saveChanges();
    } catch (Exception e) {
      log.error("Failed to save banner", e);
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to save banner: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/admin/banner";
    }
    redirectAttributes.addFlashAttribute("message", "Banner saved.");
    redirectAttributes.addFlashAttribute("messageType", "info");
    return "redirect:/admin/banner";
  }

  @Data
  public static class AdminUsersForm {

    private List<AdminUserRow> users = new ArrayList<>();

  }

  @Data
  public static class AdminUserRow {

    private String id;
    private String name;
    private String username;
    private boolean enabled;
    private List<String> roles = new ArrayList<>(); // values: USER, INSTRUCTOR, ADMIN

  }

}
