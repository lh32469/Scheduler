package org.gpc4j.web.dto;

import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class ClassScheduleTest {

  ClassSchedule classSchedule;

  @BeforeEach
  void setUp() {
    classSchedule = new ClassSchedule();

    classSchedule.setClassName("Test Class");
    classSchedule.setStartWeek(LocalDate.parse("2025-12-01"));
    classSchedule.setClassStartTime(LocalTime.of(10, 0));
    classSchedule.setNumberOfWeeks(2);
    classSchedule.setClassType(ClassType.spin);
    classSchedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
  }


  /**
   * test that if the ClassSchedule start is before the current date/month
   * the classes for the current month are listed.
   */
  @Test
  void getScheduledInProgress() {

    classSchedule = new ClassSchedule();

    classSchedule.setClassName("Test Class");
    classSchedule.setStartWeek(LocalDate.parse("2025-12-31"));
    classSchedule.setClassStartTime(LocalTime.of(10, 0));
    classSchedule.setNumberOfWeeks(10);
    classSchedule.setClassType(ClassType.spin);
    classSchedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

    YearMonth yearMonth = YearMonth.parse("2026-02");

    List<ScheduledClass> classes = classSchedule.getScheduledClasses(yearMonth);

    for (ScheduledClass aClass : classes) {
      log.info(aClass.getStart().toString());
    }

  }

  @Test
  void getRecurringClasses() {

    classSchedule = new ClassSchedule();

    classSchedule.setClassName("Test Class");
    classSchedule.setStartWeek(LocalDate.parse("2025-12-31"));
    classSchedule.setClassStartTime(LocalTime.of(10, 0));
    classSchedule.setNumberOfWeeks(-1);
    classSchedule.setClassType(ClassType.spin);
    classSchedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

    YearMonth yearMonth = YearMonth.parse("2026-05");

    List<ScheduledClass> classes = classSchedule.getScheduledClasses(yearMonth);

    for (ScheduledClass aClass : classes) {
      log.info(aClass.getStart().toString());
    }

  }

}