package org.gpc4j.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gpc4j.web.api.ClassType;
import org.gpc4j.web.security.UserAccount;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledClass {

  private String id; // RavenDB document id (e.g., ClassOffering/1-A)
  private String className;
  private ClassType classType;
  private String level;
  private LocalDateTime start;
  private int duration;
  private String location;
  private int slots;
  private UserAccount instructorAccount;

}
