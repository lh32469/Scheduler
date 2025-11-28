package org.gpc4j.web.security;

import lombok.Data;

@Data
public class SetPasswordForm {
  private String token;            // verification token
  private String password;         // new password
  private String confirmPassword;  // confirmation
}
