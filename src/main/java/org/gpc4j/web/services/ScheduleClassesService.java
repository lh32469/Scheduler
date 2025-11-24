package org.gpc4j.web.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleClassesService {

  private final RavenDB ravenDB;

  public List<ScheduledClass> getScheduledClasses(LocalDate startDate,
                                                  LocalDate endDate) {

    List<ScheduledClass> classes = new LinkedList<>();

    try (IDocumentSession session = ravenDB.openSession()) {

      // Get all the Schedules
      List<ClassSchedule> schedules = session
          .query(ClassSchedule.class)
          .include("instructorId")
          .toList();

      schedules.forEach(schedule -> {
        List<ScheduledClass> scheduledClasses =
            schedule.getScheduledClasses(startDate, endDate);

        // Already loaded into session via include above.
        // No request sent to DB.
        UserAccount instructor =
            session.load(UserAccount.class, schedule.getInstructorId());

        scheduledClasses.forEach(clazz -> {
          if (Objects.nonNull(instructor)) {
            clazz.setInstructorAccount(instructor);
          }
          classes.add(clazz);
        });
      });

    }

    classes.sort(Comparator.comparing(ScheduledClass::getStart));

//    for (ScheduledClass aClass : classes) {
//      log.info(aClass.toString());
//    }
    return classes;
  }

}
