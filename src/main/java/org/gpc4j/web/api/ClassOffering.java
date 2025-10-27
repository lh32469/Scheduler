package org.gpc4j.web.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Model representing an exercise class offering matching the provided JSON schema.
 * <p>
 * Example JSON:
 * {
 * "className": "Morning Flow Yoga",
 * "classType": "yoga",
 * "schedule": {"time":"7:00 AM - 8:00 AM","day":"Monday, Wednesday, Friday",
 * "duration":"60 min"},
 * "level": "All Levels",
 * "instructor": {"name":"Sarah Chen","bio":"Certified yoga instructor with 8+ years
 * experience in Hatha and Vinyasa styles."},
 * "availability": "12 spots available",
 * "bookingTimestamp": "2025-08-18T23:13:53.480Z",
 * "customerInfo": {"bookingId":"BOOK_1755558833480","requestSource":"website"}
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassOffering {

  private String className;
  private String classType;
  private Schedule schedule;
  private String level;
  private Instructor instructor;
  private String availability;
  private int participants;
  private int slots;

  // ISO-8601 with timezone 'Z' example -> use OffsetDateTime
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private OffsetDateTime bookingTimestamp;

  private CustomerInfo customerInfo;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Schedule {

    private String time;
    private String day;
    private LocalDateTime start;
    private String duration;

    public LocalDateTime getStart() {
      return Objects
          .requireNonNullElseGet(
              start, LocalDateTime::now);
    }

  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Instructor {

    private String name;
    private String bio;

  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CustomerInfo {

    private String bookingId;
    private String requestSource;

  }

}
