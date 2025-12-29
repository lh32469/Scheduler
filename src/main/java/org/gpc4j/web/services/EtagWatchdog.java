package org.gpc4j.web.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.gpc4j.web.dto.ClassSchedule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtagWatchdog {

  private final EtagChangePublisher publisher;
  private final DocumentStore documentStore;

  /**
   * Map of DB Names (topics) to ETags
   */
  private final Map<String, String> etags = new HashMap<>();

  @Scheduled(fixedRateString = "${application.tag-watchdog-timer}")
  public void checkEtag() {

    // Topic is database name
    for (String topic : publisher.getTopics()) {

      try (IDocumentSession session = documentStore.openSession(topic)) {

        String currentEtag = getEtag(session);
        String lastEtag = etags.get(topic);

        if (lastEtag == null) {
          etags.put(topic, currentEtag);
          return;
        }

        if (!lastEtag.equals(currentEtag)) {
          log.info("ETag changed for {} from {} to {}. Notifying clients.",
                   topic, lastEtag, currentEtag);
          etags.put(topic, currentEtag);
          publisher.publish(topic, currentEtag);
        } else {
          log.debug("Etag for topic " + topic + " has not changed.");
        }
      }

    }

  }

  /**
   * Retrieves the ETag representing the current state of the query result set
   * within the provided document session.
   *
   * @param session the current document session used to query the database
   * @return the ETag of the query result set as a String
   */
  public String getEtag(IDocumentSession session) {

    Reference<QueryStatistics> statsRef = new Reference<>();

    // Start the Query
    IDocumentQuery<ClassSchedule> query =
        session.query(ClassSchedule.class)
               .statistics(statsRef)
               .include("instructorId");

    List<ClassSchedule> schedules = query.toList();

    QueryStatistics value = statsRef.value;
    return String.valueOf(value.getResultEtag());
  }

}
