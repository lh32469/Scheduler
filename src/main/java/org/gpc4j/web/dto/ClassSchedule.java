package org.gpc4j.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassType;
import org.gpc4j.web.security.UserAccount;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
  @DateTimeFormat(pattern = "HH:mm")
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

  public List<ScheduledClass> getScheduledClasses(YearMonth month) {

    log.debug("StartWeek: " + startWeek);
    log.debug("Generating classes for {}.", month);

    LocalDate begin = month.atDay(1).minusDays(1);
    LocalDate end = month.atEndOfMonth().plusDays(1);

    // Whatever date is provided find the corresponding Sunday of that week
    startWeek = startWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    log.debug("StartWeek Sunday: " + startWeek);

    List<ScheduledClass> classes = new LinkedList<>();

    for (int i = 0; i < numberOfWeeks; i++) {

      LocalDate week = startWeek.plusWeeks(i);

      for (DayOfWeek dayOfWeek : daysOfWeek) {

        LocalDateTime startTime =
            week.plusDays(dayOfWeek.getValue()).atTime(classStartTime);
        LocalDate startDay = startTime.toLocalDate();

        if (startDay.isBefore(end) && startDay.isAfter(begin)) {
          classes.add(createClass(startTime));
        }

      }

    }

    if (numberOfWeeks == -1) {

      // Begin at startWeek and continue until we've passed through
      // the desired month.
      LocalDate week = startWeek;

      while (week.isBefore(end)) {

        for (DayOfWeek dayOfWeek : daysOfWeek) {

          LocalDateTime startTime =
              week.plusDays(dayOfWeek.getValue()).atTime(classStartTime);
          LocalDate startDay = startTime.toLocalDate();

          if (startDay.isBefore(end) && startDay.isAfter(begin)) {
            classes.add(createClass(startTime));
          }
        }

        week = week.plusWeeks(1);
      }

    }

    return classes;
  }

  ScheduledClass createClass(LocalDateTime classStartTime) {

    ScheduledClass scheduledClass = new ScheduledClass();
    scheduledClass.setStart(classStartTime);
    scheduledClass.setDuration(duration);
    scheduledClass.setLocation(location);
    scheduledClass.setSlots(slots);
    scheduledClass.setClassName(className);
    scheduledClass.setClassDescription(classDescription);
    scheduledClass.setClassType(classType);
    scheduledClass.setLevel(level);

    return scheduledClass;
  }

}
