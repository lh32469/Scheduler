package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.Booking;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RavenBookingRepository implements BookingRepository {

  private final IDocumentSession session;

  @Override
  public String save(Booking doc) {
    try {
      session.advanced().setUseOptimisticConcurrency(true);
      session.store(doc);
      session.saveChanges();
      String id = doc.getId();
      log.info("Stored booking in RavenDB with id=" + id);
      return id;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to store booking via DocumentStore: " + e.getMessage(), e);
    }
  }

}
