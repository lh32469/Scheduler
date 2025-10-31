package org.gpc4j.web.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document representation for storing a booking in RavenDB.
 */
@Data
public class BookingDocument {

  private String id; // RavenDB-assigned id
  private String domain; // Client hostname with domain.
  private String username;
  private String userId;
  private String classId; // RavenDB ClassOffering Id.
  private LocalDateTime dateBooked;

}
