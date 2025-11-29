package org.gpc4j.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Sets per-role HTTP session timeouts after successful authentication.
 * <p>
 * Rules:
 * - ROLE_ADMIN      → 30 minutes
 * - ROLE_INSTRUCTOR → 60 minutes
 * - ROLE_USER       → 4 hours
 * <p>
 * If a user has multiple roles, the shortest applicable timeout is used.
 * If no known role is present, the global server.session.timeout applies.
 */
@Slf4j
public class RoleBasedSessionTimeoutSuccessHandler
    implements AuthenticationSuccessHandler {

  private final SavedRequestAwareAuthenticationSuccessHandler delegate =
      new SavedRequestAwareAuthenticationSuccessHandler();

  public RoleBasedSessionTimeoutSuccessHandler() {
    // Preserve previous behavior: always go to "/" after login
    delegate.setDefaultTargetUrl("/");
    delegate.setAlwaysUseDefaultTargetUrl(true);
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication)
      throws IOException, ServletException {

    HttpSession session = request.getSession(false);
    if (session != null && authentication != null) {
      Integer seconds = resolveTimeoutSeconds(authentication.getAuthorities());
      if (seconds != null) {
        session.setMaxInactiveInterval(seconds);
        if (log.isInfoEnabled()) {
          log.info("Applied role-based session timeout: {} seconds for user {}",
                   seconds, authentication.getName());
        }
      } else if (log.isDebugEnabled()) {
        log.debug("No role-based timeout match; using server default for user {}",
                  authentication.getName());
      }
    }

    delegate.onAuthenticationSuccess(request, response, authentication);
  }

  /**
   * Determine timeout based on authorities. Returns seconds or null to use default.
   */
  private Integer resolveTimeoutSeconds(Collection<? extends GrantedAuthority> authorities) {
    if (authorities == null || authorities.isEmpty()) {
      return null;
    }

    boolean isAdmin =
        authorities.stream()
                   .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    boolean isInstructor =
        authorities.stream()
                   .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));
    boolean isUser =
        authorities.stream()
                   .anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));

    // Seconds for each role
    final int ADMIN = (int) TimeUnit.MINUTES.toSeconds(30);       // 30 minutes
    final int INSTRUCTOR = (int) TimeUnit.MINUTES.toSeconds(60);  // 60 minutes
    final int USER = (int) TimeUnit.HOURS.toSeconds(4);           // 4 hours

    Integer timeout = null;
    if (isAdmin) {
      timeout = min(timeout, ADMIN);
    }
    if (isInstructor) {
      timeout = min(timeout, INSTRUCTOR);
    }
    if (isUser) {
      timeout = min(timeout, USER);
    }
    return timeout; // may be null
  }

  private Integer min(Integer current, int candidate) {
    if (current == null) {
      return candidate;
    }
    return Math.min(current, candidate);
  }

}
