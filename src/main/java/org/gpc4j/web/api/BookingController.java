package org.gpc4j.web.api;

import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.BookingRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Simple REST controller to receive class booking information.
 *
 * Endpoint: POST /api/bookings
 * Content-Type: application/json
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/bookings")
public class BookingController {

  private final BookingRepository repository;

  public BookingController(BookingRepository repository) {
    this.repository = repository;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BookingResponse> createBooking(
      @RequestBody ClassOffering offering) {
    String receivedAt = OffsetDateTime.now().toString();
    log.info("Received booking (ClassOffering): {}", offering);

    try {
      String id = repository.save(offering);
      BookingResponse response = new BookingResponse(
          "stored",
          offering,
          receivedAt,
          id
      );
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to store booking: {}", e.toString());
      BookingResponse response = new BookingResponse(
          "error",
          offering,
          receivedAt,
          null
      );
      return ResponseEntity.status(502).body(response);
    }
  }

  /**
   * Incoming booking payload. Uses Java 21 record for brevity.
   */
  public record BookingRequest(
      String name,
      String email,
      String phone,
      String classId,
      String className,
      String date,   // ISO-8601 date string (e.g., 2025-08-18)
      String time,   // Local time string (e.g., 14:30)
      Integer participants,
      String notes
  ) {

  }

  /**
   * Simple response wrapper.
   */
  public record BookingResponse(
      String status,
      ClassOffering booking,
      String receivedAt,
      String id
  ) {

  }

}
