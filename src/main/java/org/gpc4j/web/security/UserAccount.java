package org.gpc4j.web.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * RavenDB user document used for authentication/authorization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

  private String id; // RavenDB document id (e.g., users/1-A)

  private String name = "Blank";
  private String username;

  /**
   * For Instructors
   */
  private String profile;

  private String passwordHash; // BCrypt
  // Plaintext password field for form binding only (not persisted)
  @JsonIgnore
  @ToString.Exclude
  private transient String password;

  private List<String> roles; // e.g., ["ROLE_USER"], ["ROLE_ADMIN"]

  private boolean enabled = true;
  private boolean accountNonLocked = true;

}
