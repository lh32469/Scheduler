package org.gpc4j.web.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.Utils;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.repository.ClassScheduleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gpc4j.web.Utils.generateETag;

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

  @Scheduled(fixedRate = 30000) // Check every 30 seconds
  public void checkEtag() {
    LocalDate sunday = Utils.getSunday();

    for (String topic : publisher.getTopics()) {

      try (IDocumentSession session = documentStore.openSession(topic)) {

        ClassScheduleRepository repo = new ClassScheduleRepository(session);

        List<ScheduledClass> classes =
            repo.getClassesDuring(sunday, sunday.plusWeeks(4));
        String currentEtag = generateETag(classes);

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

}
