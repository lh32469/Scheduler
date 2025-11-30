package org.gpc4j.web.security;

import lombok.Data;
import org.gpc4j.web.api.ServiceType;

import java.util.List;

/**
 * Form object for self-service profile edits. Limits fields to safe, user-editable data.
 */
@Data
public class AccountProfileForm {
  private String name;    // required
  private String profile; // optional bio
  /**
   * For instructors only: editable list of Service Types they provide.
   * Will only be respected server-side when the user has ROLE_INSTRUCTOR.
   */
  private List<ServiceType> serviceTypes;
}
