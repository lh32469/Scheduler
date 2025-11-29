package org.gpc4j.web.security;

import lombok.Data;

/**
 * Form object for self-service profile edits. Limits fields to safe, user-editable data.
 */
@Data
public class AccountProfileForm {
  private String name;    // required
  private String profile; // optional bio
}
