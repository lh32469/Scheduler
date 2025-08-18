package org.gpc4j.web.api;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Simple REST controller to receive class booking information.
 * <p>
 * Endpoint: POST /api/bookings
 * Content-Type: application/json
 * <p>
 * This implementation only receives and echoes the posted data with a status and
 * timestamp.
 * There is no persistence or validation by design to keep changes minimal.
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/bookings")
public class BookingController {

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BookingResponse> createBooking(
      @RequestBody BookingRequest booking) {
    log.info("Received booking: {}", booking);
    BookingResponse response = new BookingResponse(
        "received",
        booking,
        OffsetDateTime.now().toString()
    );
    return ResponseEntity.ok(response);
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
      BookingRequest booking,
      String receivedAt
  ) {

  }

}
