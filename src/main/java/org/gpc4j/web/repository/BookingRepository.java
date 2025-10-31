package org.gpc4j.web.repository;

import org.gpc4j.web.api.BookingDocument;
import org.gpc4j.web.api.ClassOffering;

import java.time.OffsetDateTime;

/**
 * Repository abstraction for persisting BookingDocument instances.
 */
public interface BookingRepository {

  /**
   * Persist the given BookingDocument and return its RavenDB id.
   */
  String save(BookingDocument doc);

}
