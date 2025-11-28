package org.gpc4j.web.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
  private String id;        // RavenDB id e.g., VerificationTokens/1-A
  private String userId;    // UserAccount id
  private String token;     // random UUID string
  private Instant expiresAt; // expiry timestamp
  private boolean used;     // set true once consumed
  /**
   * Token purpose. Defaults to SIGNUP when null for backward compatibility with older tokens.
   */
  private VerificationToken.TokenType type = TokenType.SIGNUP;

  public enum TokenType {
    SIGNUP,
    PASSWORD_RESET
  }
}
