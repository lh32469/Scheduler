package org.gpc4j.web.dto;

import lombok.Data;
import org.gpc4j.web.api.ClassType;

import java.time.LocalDateTime;

/**
 * Document representation for storing a booking in RavenDB.
 */
@Data
public class Booking {

  private String id;
  private String username;
  private String userId;
  private String classId; // Specific instance of ScheduledClass

  private String classScheduleId;

  private String className;
  private ClassType classType;
  private String classDescription;
  private String level;
  private int duration;
  private String instructorName;

  private LocalDateTime dateBooked;
  private LocalDateTime classStart;

  public Booking() {
  }

  public Booking(ScheduledClass scheduledClass) {
    this.classId = scheduledClass.getId();
    this.className = scheduledClass.getClassName();
    this.classType = scheduledClass.getClassType();
    this.classScheduleId = scheduledClass.getClassScheduleId();
    this.dateBooked = LocalDateTime.now();
    this.classStart = scheduledClass.getStart();
    this.classDescription = scheduledClass.getClassDescription();
    this.level = scheduledClass.getLevel();
    this.duration = scheduledClass.getDuration();
    this.instructorName = scheduledClass.getInstructorAccount().getName();
  }

}
