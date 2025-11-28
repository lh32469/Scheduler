package org.gpc4j.web.security;

import lombok.Data;

@Data
public class SignupForm {
  private String email;   // used as username
  private String name;    // full name
  private String profile; // optional profile/bio
}
