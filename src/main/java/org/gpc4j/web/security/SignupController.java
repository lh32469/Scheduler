package org.gpc4j.web.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.RavenUserRepository;
import org.gpc4j.web.repository.RavenVerificationTokenRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping
public class SignupController {

  private final RavenUserRepository userRepository;
  private final RavenVerificationTokenRepository tokenRepository;
  private final JavaMailSender mailSender;
  private final PasswordEncoder passwordEncoder;

  @GetMapping("/signup")
  public String signupForm(Model model) {
    model.addAttribute("signup", new SignupForm());
    return "signup";
  }

  @PostMapping("/signup")
  public String handleSignup(SignupForm form,
                             HttpServletRequest request,
                             Model model) {
    // Basic validation
    String email = (form.getEmail() == null) ? null : form.getEmail().trim().toLowerCase(Locale.ENGLISH);
    String name = (form.getName() == null) ? null : form.getName().trim();
    String profile = (form.getProfile() == null) ? null : form.getProfile().trim();

    if (!StringUtils.hasText(email) || !email.contains("@")) {
      model.addAttribute("error", "Please provide a valid email address.");
      model.addAttribute("signup", form);
      return "signup";
    }
    if (!StringUtils.hasText(name)) {
      model.addAttribute("error", "Please provide your full name.");
      model.addAttribute("signup", form);
      return "signup";
    }

    // Check for existing account
    if (userRepository.findByUsername(email) != null) {
      model.addAttribute("error", "An account with that email already exists.");
      model.addAttribute("signup", form);
      return "signup";
    }

    // Create disabled user with a random password hash placeholder
    UserAccount user = new UserAccount();
    user.setUsername(email);
    user.setName(name);
    user.setProfile(profile);
    user.setEnabled(false);
    user.setAccountNonLocked(true);
    user.setRoles(List.of("USER"));
    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

    String userId = userRepository.save(user);
    user.setId(userId);

    // Create token valid for 48 hours
    String tokenValue = UUID.randomUUID().toString();
    VerificationToken token = new VerificationToken();
    token.setUserId(userId);
    token.setToken(tokenValue);
    token.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
    token.setUsed(false);
    tokenRepository.save(token);

    // Build verification URL
    String verifyUrl = buildVerificationUrl(request, tokenValue);
    log.info("Verification link for {}: {}", email, verifyUrl);

    // Send email
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(email);
      msg.setFrom("permits.pdx@gmail.com");
      msg.setSubject("Verify your email address");
      msg.setText("Hello " + name + ",\n\n" +
          "Thanks for signing up. Please verify your email address by clicking the link below:\n\n" +
          verifyUrl + "\n\n" +
          "If you did not request this, you can ignore this email.\n\n" +
          "Thank you.");
      mailSender.send(msg);
    } catch (Exception e) {
      log.error("Failed to send verification email to {}: {}", email, e.toString());
      // Continue but show info to the user
    }

    model.addAttribute("submitted", true);
    model.addAttribute("email", email);
    return "signup";
  }

  @GetMapping("/verify")
  public String verify(@RequestParam("token") String tokenValue, Model model) {
    VerificationToken token = tokenRepository.findByToken(tokenValue);
    if (token == null) {
      model.addAttribute("status", "invalid");
      return "verify";
    }
    if (token.isUsed() || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
      model.addAttribute("status", token.isUsed() ? "used" : "expired");
      return "verify";
    }

    // Enable the user
    UserAccount user = userRepository.findById(token.getUserId());
    if (user != null && !user.isEnabled()) {
      user.setEnabled(true);
      userRepository.save(user);
    }

    // Mark token used (delete to keep things tidy)
    token.setUsed(true);
    tokenRepository.delete(token.getId());

    model.addAttribute("status", "success");
    return "verify";
  }

  private String buildVerificationUrl(HttpServletRequest request, String tokenValue) {
    String scheme = firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
    String host = firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getServerName());
    String context = request.getContextPath() != null ? request.getContextPath() : "";
    String base = scheme + "://" + host + context;
    // If host header contains host:port already, keep it; otherwise append local port if non-standard
    try {
      URI u = URI.create(base);
      if (u.getPort() == -1 && request.getServerPort() != 80 && request.getServerPort() != 443
          && (request.getHeader("X-Forwarded-Host") == null)) {
        base = scheme + "://" + host + ":" + request.getServerPort() + context;
      }
    } catch (Exception ignored) {}
    return base + "/verify?token=" + tokenValue;
  }

  private String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    return b;
  }
}
