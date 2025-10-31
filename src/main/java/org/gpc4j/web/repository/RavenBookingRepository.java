package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.BookingDocument;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RavenBookingRepository implements BookingRepository {

  private final IDocumentStore documentStore;

  @Override
  public String save(BookingDocument doc) {
    try (IDocumentSession session = documentStore.openSession()) {
      session.advanced().setUseOptimisticConcurrency(true);
      session.store(doc);
      session.saveChanges();
      String id = doc.getId();
      log.info("Stored booking in RavenDB with id={}", id);
      return id;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to store booking via DocumentStore: " + e.getMessage(), e);
    }
  }

}
