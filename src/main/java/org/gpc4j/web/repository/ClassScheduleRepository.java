package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.IAdvancedSessionOperations;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassScheduleRepository {

  private final RavenDB ravenDB;

  public List<ScheduledClass> listClassesForPeriod(LocalDate startDate,
                                                   LocalDate endDate,
                                                   @Nullable String classType,
                                                   @Nullable String instructorId) {

    List<ScheduledClass> classes = new LinkedList<>();

    try (IDocumentSession session = ravenDB.openSession()) {

      // Start the Query
      IDocumentQuery<ClassSchedule> query =
          session.query(ClassSchedule.class)
                 .include("instructorId");

      if (StringUtils.hasText(classType)) {
        log.debug("Filtering classes by class type " + classType);
        query.whereEquals("classType", classType.trim());
      }

      if (StringUtils.hasText(instructorId)) {
        if (!instructorId.startsWith("UserAccounts")) {
          instructorId = "UserAccounts/" + instructorId;
        }
        log.debug("Filtering classes by instructorId " + instructorId);
        query.whereEquals("instructorId", instructorId.trim());
      }

      List<ClassSchedule> schedules = query.toList();

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
            classes.add(clazz);
          }
        });
      });

    }

    // Filter classes in the past
    classes.removeIf(c -> c.getStart()
                           .toLocalDate()
                           .isBefore(LocalDate.now(ZoneOffset.UTC)));

    classes.sort(Comparator.comparing(ScheduledClass::getStart));
    log.info("Found " + classes.size() + " classes for "
                 + startDate + " to " + endDate
                 + " for classType " + classType
                 + " and instructorId " + instructorId);

    return classes;
  }

  public ClassSchedule findById(String id) {
    try (IDocumentSession session = ravenDB.openSession()) {
      ClassSchedule found = session.load(ClassSchedule.class, id);
      if (found == null) {
        log.info("ClassSchedule not found for id={}", id);
      }
      return found;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to load ClassSchedule id=" +
              id + ": " + e.getMessage(), e);
    }
  }

  public String store(ClassSchedule schedule, @Nullable String id) {

    try (IDocumentSession session = ravenDB.openSession()) {

      IAdvancedSessionOperations advanced = session.advanced();

      if (StringUtils.hasText(id)) {
        session.store(schedule, id);
      } else {
        session.store(schedule);
        id = advanced.getDocumentId(schedule);
      }

      LocalDate ends = schedule
          .getStartWeek()
          .plusWeeks(schedule.getNumberOfWeeks());

      Date date = Date.from(ends.atStartOfDay().toInstant(ZoneOffset.UTC));

      IMetadataDictionary metadata = advanced.getMetadataFor(schedule);
      metadata.put(Constants.Documents.Metadata.EXPIRES, date);

      session.saveChanges();
      log.info("Stored ClassSchedule in RavenDB with id={}", id);
      return id;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to store ClassSchedule via DocumentStore:"
              + " " + e.getMessage(), e);
    }
  }

  public String store(ClassSchedule schedule) {
    return store(schedule, null);
  }

}
