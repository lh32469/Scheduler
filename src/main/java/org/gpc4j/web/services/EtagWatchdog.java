package org.gpc4j.web.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.gpc4j.web.dto.Booking;
import org.gpc4j.web.dto.ClassSchedule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtagWatchdog {

  private final EtagChangePublisher publisher;
  private final DocumentStore documentStore;

  @SneakyThrows
  @Scheduled(fixedRateString = "${application.tag-watchdog-timer}")
  public void checkEtags() {

    // Topic is database name
    for (String topic : publisher.getTopics()) {
      try (IDocumentSession session = documentStore.openSession(topic)) {
        publisher.publish(topic, getEtag(session));
      }
      Thread.sleep(250);
    }

  }

  /**
   * Retrieves the ETag representing the current state of the query result sets
   * within the provided document session.
   *
   * @param session the current document session used to query the database
   * @return the ETag of the query result set as a String
   */
  public String getEtag(IDocumentSession session) {

    Reference<QueryStatistics> statsRef = new Reference<>();

    // Check ClassSchedules
    var query = session.query(ClassSchedule.class)
                       .statistics(statsRef)
                       .include("instructorId");

    var schedules = query.toList();
    Long schedulesEtag = statsRef.value.getResultEtag();

    // Check Bookings
    statsRef = new Reference<>();
    var bookingQuery = session.query(Booking.class)
                              .statistics(statsRef);

    var bookings = bookingQuery.toList();
    Long bookingsEtag = statsRef.value.getResultEtag();

    return String.valueOf(bookingsEtag + schedulesEtag);
  }

}
