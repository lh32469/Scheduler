package org.gpc4j.web.dto;

import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

  @Test
  void getScheduledClasses() {

    LocalDate start = LocalDate.parse("2025-12-01");
    LocalDate end = LocalDate.parse("2025-12-29");

    List<ScheduledClass> classes = classSchedule.getScheduledClasses(start, end);

    for (ScheduledClass aClass : classes) {
      log.info(aClass.toString());
    }

    LocalDate previousSunday = classSchedule
        .getStartWeek()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

    log.info("Previous Sunday: {}", previousSunday);
    assertEquals(DayOfWeek.SUNDAY, previousSunday.getDayOfWeek());
    assertEquals(LocalDate.parse("2025-11-30"), previousSunday);

    assertEquals(4, classes.size());
    assertEquals(LocalDate.parse("2025-12-01"), classes.get(0).getStart().toLocalDate());
    assertEquals(LocalDate.parse("2025-12-03"), classes.get(1).getStart().toLocalDate());
    assertEquals(LocalDate.parse("2025-12-08"), classes.get(2).getStart().toLocalDate());
    assertEquals(LocalDate.parse("2025-12-10"), classes.get(3).getStart().toLocalDate());

    assertEquals(DayOfWeek.MONDAY, classes.get(0).getStart().getDayOfWeek());
    assertEquals(DayOfWeek.WEDNESDAY, classes.get(1).getStart().getDayOfWeek());
  }

}