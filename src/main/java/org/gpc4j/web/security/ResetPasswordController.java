package org.gpc4j.web.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.UserRepository;
import org.gpc4j.web.repository.RavenVerificationTokenRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping
public class ResetPasswordController {

  private final UserRepository userRepository;
  private final RavenVerificationTokenRepository tokenRepository;
  private final JavaMailSender mailSender;
  private final PasswordEncoder passwordEncoder;

  /**
   * Page for authenticated users to initiate password reset by re-verifying their email.
   */
  @GetMapping("/account/password")
  public String passwordResetRequest(Authentication authentication, Model model) {
    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }
    String username = authentication.getName().trim().toLowerCase(Locale.ENGLISH);
    UserAccount user = userRepository.findByUsername(username);
    if (user == null) {
      return "redirect:/login";
    }
    model.addAttribute("email", user.getUsername());
    return "account/password_reset_request";
  }

  /**
   * Sends a verification email containing a short-lived PASSWORD_RESET token.
   */
  @PostMapping("/account/password")
  public String sendResetEmail(Authentication authentication,
                               HttpServletRequest request,
                               Model model) {
    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }
    String username = authentication.getName().trim().toLowerCase(Locale.ENGLISH);
    UserAccount user = userRepository.findByUsername(username);
    if (user == null) {
      return "redirect:/login";
    }

    // Create token valid for 2 hours
    String tokenValue = UUID.randomUUID().toString();
    VerificationToken token = new VerificationToken();
    token.setUserId(user.getId());
    token.setToken(tokenValue);
    token.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
    token.setUsed(false);
    token.setType(VerificationToken.TokenType.PASSWORD_RESET);
    tokenRepository.save(token);

    String verifyUrl = buildResetUrl(request, tokenValue);
    log.info("Password reset link for {}: {}", username, verifyUrl);

    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(user.getUsername());
      msg.setFrom("permits.pdx@gmail.com");
      msg.setSubject("Reset your password");
      msg.setText("Hello " + (user.getName() != null ? user.getName() : "") + ",\n\n" +
          "You requested to reset your password. Please confirm your email and set a new password by clicking the link below within 2 hours:\n\n" +
          verifyUrl + "\n\n" +
          "If you did not request this, you can ignore this email.\n\n" +
          "Thank you.");
      mailSender.send(msg);
    } catch (Exception e) {
      log.error("Failed to send password reset email: {}", e.toString());
      // Continue to show submitted state regardless to avoid information leakage
    }

    model.addAttribute("submitted", true);
    model.addAttribute("email", user.getUsername());
    return "account/password_reset_request";
  }

  /**
   * Token landing page. If valid, render set-password form; else show error state.
   */
  @GetMapping("/verify-reset")
  public String verifyReset(@RequestParam("token") String tokenValue, Model model) {
    VerificationToken token = tokenRepository.findByToken(tokenValue);
    if (token == null || token.isUsed() || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())
        || token.getType() != VerificationToken.TokenType.PASSWORD_RESET) {
      model.addAttribute("status", token == null ? "invalid" : (token.isUsed() ? "used" : "expired"));
      return "verify";
    }

    SetPasswordForm form = new SetPasswordForm();
    form.setToken(tokenValue);
    model.addAttribute("setPassword", form);

    UserAccount user = userRepository.findById(token.getUserId());
    if (user != null) {
      model.addAttribute("email", user.getUsername());
      model.addAttribute("name", user.getName());
    }
    return "set_password_reset";
  }

  /**
   * Processes the new password for a valid PASSWORD_RESET token.
   */
  @PostMapping("/verify-reset")
  public String completeReset(SetPasswordForm form, Model model) {
    String tokenValue = form != null ? form.getToken() : null;
    if (!StringUtils.hasText(tokenValue)) {
      model.addAttribute("status", "invalid");
      return "verify";
    }
    VerificationToken token = tokenRepository.findByToken(tokenValue);
    if (token == null || token.isUsed() || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())
        || token.getType() != VerificationToken.TokenType.PASSWORD_RESET) {
      model.addAttribute("status", token == null ? "invalid" : (token.isUsed() ? "used" : "expired"));
      return "verify";
    }

    String error = validatePassword(form.getPassword(), form.getConfirmPassword());
    if (error != null) {
      model.addAttribute("setPassword", form);
      UserAccount user = userRepository.findById(token.getUserId());
      if (user != null) {
        model.addAttribute("email", user.getUsername());
        model.addAttribute("name", user.getName());
      }
      model.addAttribute("error", error);
      return "set_password_reset";
    }

    UserAccount user = userRepository.findById(token.getUserId());
    if (user == null) {
      model.addAttribute("status", "invalid");
      return "verify";
    }

    user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
    userRepository.save(user);

    token.setUsed(true);
    tokenRepository.delete(token.getId());

    return "redirect:/login?verified";
  }

  private String validatePassword(String password, String confirm) {
    if (!StringUtils.hasText(password) || !StringUtils.hasText(confirm)) {
      return "Password and confirmation are required.";
    }
    if (!password.equals(confirm)) {
      return "Passwords do not match.";
    }
    if (password.length() < 8) {
      return "Password must be at least 8 characters long.";
    }
    boolean hasLetter = password.chars().anyMatch(Character::isLetter);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    if (!(hasLetter && hasDigit)) {
      return "Password must include at least one letter and one number.";
    }
    return null;
  }

  private String buildResetUrl(HttpServletRequest request, String tokenValue) {
    String scheme = firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
    String host = firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getServerName());
    String context = request.getContextPath() != null ? request.getContextPath() : "";
    String base = scheme + "://" + host + context;
    try {
      URI u = URI.create(base);
      if (u.getPort() == -1 && request.getServerPort() != 80 && request.getServerPort() != 443
          && (request.getHeader("X-Forwarded-Host") == null)) {
        base = scheme + "://" + host + ":" + request.getServerPort() + context;
      }
    } catch (Exception ignored) {}
    return base + "/verify-reset?token=" + tokenValue;
  }

  private String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    return b;
  }
}
