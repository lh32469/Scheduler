package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.IAdvancedSessionOperations;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassScheduleRepository {

  private final IDocumentSession session;

  //  @Cacheable(value = CLASS_LIST, keyGenerator = "customKeyGenerator")
  public List<ScheduledClass> getClassesDuring(LocalDate startDate,
                                               LocalDate endDate) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    List<ScheduledClass> classes = new LinkedList<>();

    Reference<QueryStatistics> statsRef = new Reference<>();

    // Start the Query
    IDocumentQuery<ClassSchedule> query =
        session.query(ClassSchedule.class)
               .statistics(statsRef)
               .include("instructorId");

    List<ClassSchedule> schedules = query.toList();

    QueryStatistics value = statsRef.value;
    log.debug("Query ETag: " + value.getResultEtag());

    if (value.getDurationInMs() == -1) {
      log.debug("Query served from cache");
    } else {
      log.debug("Query fetched from server (took {}ms)",
               statsRef.value.getDurationInMs());
    }

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

    // Filter classes in the past
    classes.removeIf(c -> c.getStart()
                           .toLocalDate()
                           .isBefore(LocalDate.now(ZoneOffset.UTC)));

    classes.sort(Comparator.comparing(ScheduledClass::getStart));

    stopWatch.stop();
    log.debug("Found " + classes.size() + " classes for "
                 + startDate + " to " + endDate
                 + " in " + stopWatch.getTotalTimeMillis() + " ms");

    return classes;
  }

  public Optional<ClassSchedule> findById(String id) {
    ClassSchedule found = session.load(ClassSchedule.class, id);
    if (found == null) {
      log.warn("ClassSchedule not found for id={}", id);
    }
    return Optional.ofNullable(found);
  }

  public String store(ClassSchedule schedule, @Nullable String id) {

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
  }

  public String store(ClassSchedule schedule) {
    return store(schedule, null);
  }

}
