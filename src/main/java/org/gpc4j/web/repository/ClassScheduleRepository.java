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
import org.gpc4j.web.dto.Booking;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.gpc4j.web.services.EtagWatchdog;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassScheduleRepository {

  private final IDocumentSession session;
  private final EtagWatchdog etagWatchdog;

  public List<ScheduledClass> getClassesForMonth(YearMonth month,
                                                 ZoneId zoneId) {

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Reference<QueryStatistics> statsRef = new Reference<>();

    // Start the Query
    IDocumentQuery<ClassSchedule> query =
        session.query(ClassSchedule.class)
               .statistics(statsRef)
               .include("instructorId");

    List<ClassSchedule> schedules = query.toList();

    List<String> classScheduleIds =
        Collections.synchronizedList(new LinkedList<>());

    List<ScheduledClass> classes =
        schedules.stream()
                 .map(sched -> {
                        classScheduleIds.add(sched.getId());

                        List<ScheduledClass> classList = sched.getScheduledClasses(month);

                        // Already loaded into session via include above.
                        // No request sent to DB.
                        UserAccount instructor =
                            session.load(UserAccount.class, sched.getInstructorId());

                        AtomicInteger index = new AtomicInteger();

                        classList.forEach(clazz -> {
                          if (Objects.nonNull(instructor)) {
                            clazz.setInstructorAccount(instructor);
                            clazz.setClassScheduleId(sched.getId());
                            clazz.setId(sched.getId() + "." + index.getAndIncrement());
                          }
                        });
                        return classList;
                      }
                 )

                 .flatMap(List::stream)
                 // Filter out classes in the past
                 .filter(clazz -> clazz.getStart().isAfter(LocalDateTime.now(zoneId)))
                 .sorted(Comparator.comparing(ScheduledClass::getStart))
                 .toList();

    log.debug("ClassScheduleIds: " + classScheduleIds);

    List<Booking> bookings =
        session.query(Booking.class)
               .whereIn("classScheduleId", classScheduleIds)
               .toList();

    log.debug("Bookings " + bookings);

    classes.stream()
           // Only apply to classes with slots > 0
           .filter(c -> c.getSlots() > 0)
           .forEach(c -> {
             long bookedCount =
                 bookings.stream()
                         .filter(b -> b.getClassId().equals(c.getId()))
                         .count();
             c.setSlots((int) (c.getSlots() - bookedCount));
           });

    log.info("Found " + classes.size() + " classes for " + month + " with "
                 + bookings.size() + " bookings"
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
    etagWatchdog.checkEtags();
    return id;
  }

  public String store(ClassSchedule schedule) {
    return store(schedule, null);
  }

}
