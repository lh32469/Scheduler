package org.gpc4j.web.dto;

import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.repository.ClassScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@SpringBootTest
public class ClassScheduleIT {

  @Autowired
  IDocumentSession session;

  @Autowired
  ClassScheduleRepository repository;

  @BeforeEach
  public void setUp() throws Exception {
    log.info("IDocumentSession " + session);
  }

  @Test
  public void createClassSchedule() throws Exception {
    ClassSchedule schedule =
        session.load(ClassSchedule.class, "ClassSchedules/8-A");

    YearMonth yearMonth = YearMonth.parse("2026-01");

    List<ScheduledClass> classes = schedule.getScheduledClasses(yearMonth);

    for (ScheduledClass aClass : classes) {
      log.info(aClass.getStart().toString());
    }

  }

  @Test
  public void getClassesFromRepository() throws Exception {

    YearMonth yearMonth = YearMonth.parse("2026-01");

    ZoneId zoneId = ZoneId.of("Europe/London");

    List<ScheduledClass> classes =
        repository.getClassesForMonth(yearMonth, zoneId);

    for (ScheduledClass aClass : classes) {
      log.info(aClass.getStart().toString() + "  " + aClass.getClassName());
    }

  }

}
