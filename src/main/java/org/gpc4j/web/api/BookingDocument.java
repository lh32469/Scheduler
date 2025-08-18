package org.gpc4j.web.api;

import lombok.Data;

/**
 * Document representation for storing a booking in RavenDB.
 */
@Data
public class BookingDocument {
  private String id; // RavenDB-assigned id

  // Legacy flat booking fields (kept for backward compatibility)
  private String name;
  private String email;
  private String phone;
  private String classId;
  private String className;
  private String date;
  private String time;
  private Integer participants;
  private String notes;

  // New fields mapped from ClassOffering
  private String classType;
  private String scheduleTime;
  private String scheduleDay;
  private String scheduleDuration;
  private String level;
  private String instructorName;
  private String instructorBio;
  private String availability;
  private String bookingTimestamp;
  private String bookingId;
  private String requestSource;

  private String receivedAt; // ISO-8601 timestamp when received by our API

  public static BookingDocument fromRequest(BookingController.BookingRequest req, String receivedAt) {
    BookingDocument d = new BookingDocument();
    d.setName(req.name());
    d.setEmail(req.email());
    d.setPhone(req.phone());
    d.setClassId(req.classId());
    d.setClassName(req.className());
    d.setDate(req.date());
    d.setTime(req.time());
    d.setParticipants(req.participants());
    d.setNotes(req.notes());
    d.setReceivedAt(receivedAt);
    return d;
  }

  public static BookingDocument fromOffering(ClassOffering offering, String receivedAt) {
    BookingDocument d = new BookingDocument();
    if (offering != null) {
      d.setClassName(offering.getClassName());
      d.setClassType(offering.getClassType());
      if (offering.getSchedule() != null) {
        d.setScheduleTime(offering.getSchedule().getTime());
        d.setScheduleDay(offering.getSchedule().getDay());
        d.setScheduleDuration(offering.getSchedule().getDuration());
      }
      d.setLevel(offering.getLevel());
      if (offering.getInstructor() != null) {
        d.setInstructorName(offering.getInstructor().getName());
        d.setInstructorBio(offering.getInstructor().getBio());
      }
      d.setAvailability(offering.getAvailability());
      d.setBookingTimestamp(offering.getBookingTimestamp() != null ? offering.getBookingTimestamp().toString() : null);
      if (offering.getCustomerInfo() != null) {
        d.setBookingId(offering.getCustomerInfo().getBookingId());
        d.setRequestSource(offering.getCustomerInfo().getRequestSource());
      }
    }
    d.setReceivedAt(receivedAt);
    return d;
  }
}
