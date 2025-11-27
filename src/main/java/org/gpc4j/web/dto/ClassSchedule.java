package org.gpc4j.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassType;
import org.gpc4j.web.security.UserAccount;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSchedule {

  private String id; // RavenDB document id (e.g., ClassSchedule/1-A)

  private String className;
  private String classDescription;
  private ClassType classType;
  private String level;

  /**
   * Represents the RavenDB document ID of the Instructor.
   */
  private String instructorId;
  @JsonIgnore
  private UserAccount instructorAccount;

  /**
   * The Sunday of the week the first class(es) will run.
   */
  private LocalDate startWeek;

  /**
   * Number of weeks the class will run.  Zero (0) for all weeks.
   */
  private int numberOfWeeks;

  /**
   * Day(s) of the week the class will run.
   */
  private Set<DayOfWeek> daysOfWeek = Collections.emptySet();

  /**
   * Time of day the class will run.
   */
  private LocalTime classStartTime;

  /**
   * Duration of the class in minutes.
   */
  private int duration;

  /**
   * Initial Number of slots available for the class.  If value is negative,
   * the class is considered unlimited.
   */
  private int slots;

  /**
   * Location of the class.  Studio, Pool, etc.
   */
  private String location;

  public List<ScheduledClass> getScheduledClasses(LocalDate startDate,
                                                  LocalDate endDate) {

    List<ScheduledClass> classes = new LinkedList<>();

    log.debug(className);
    log.debug("Generating classes for {} to {}.", startDate, endDate);
    log.debug("Number of weeks: {}", numberOfWeeks);
    log.debug("Start week: {}", startWeek);

    if (numberOfWeeks == 0) {
      numberOfWeeks = 5;
    }

    for (int i = 0; i < numberOfWeeks; i++) {

      LocalDate week = startWeek.plusWeeks(i);
      log.debug("Checking for classes in week {}: {}", i, week);

      if ((week.isAfter(startDate) || week.equals(startDate))
          && (week.isBefore(endDate))) {
        // Generate classes for this week
        for (DayOfWeek dayOfWeek : daysOfWeek) {
          ScheduledClass scheduledClass = new ScheduledClass();
          scheduledClass.setStart(
              week.plusDays(dayOfWeek.getValue()).atTime(classStartTime));
          scheduledClass.setDuration(duration);
          scheduledClass.setLocation(location);
          scheduledClass.setSlots(slots);

          scheduledClass.setClassName(className);
          scheduledClass.setClassDescription(classDescription);
          scheduledClass.setClassType(classType);
          scheduledClass.setLevel(level);

          classes.add(scheduledClass);
        }
      }
    }

    classes.sort(Comparator.comparing(ScheduledClass::getStart));
    return classes;
  }

  /**
   * Helper for views: returns the selected days of week as a concise, human-friendly
   * comma-separated string using standard 3-letter abbreviations with capitalization,
   * sorted Monday through Sunday (e.g., "Mon, Wed, Fri").
   */
  public String getDaysOfWeekAbbrevText() {
    if (daysOfWeek == null || daysOfWeek.isEmpty()) {
      return "";
    }
    return daysOfWeek.stream()
                     .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                     .map(d -> d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                     .map(s -> s.substring(0, 1).toUpperCase(Locale.ENGLISH)
                         + s.substring(1).toLowerCase(Locale.ENGLISH))
                     .distinct()
                     .reduce((a, b) -> a + ", " + b)
                     .orElse("");
  }

}
