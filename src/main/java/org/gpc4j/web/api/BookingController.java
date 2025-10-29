package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.repository.BookingRepository;
import org.gpc4j.web.repository.ClassOfferingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Simple REST controller to receive class booking information.
 * <p>
 * Endpoint: POST /api/bookings
 * Content-Type: application/json
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(path = "/api/bookings")
public class BookingController {

  private final IDocumentStore documentStore;
  private final BookingRepository repository;
  private final ClassOfferingRepository classOfferingRepository;

  @PostMapping(path = "/v2")
  public String createBookingTwo(ClassOffering offering,
                                 RedirectAttributes redirectAttributes) {

    log.info("Received booking (ClassOffering): {}", offering);

    log.info("className = " + offering.getClassName());
    log.info("Start:  + " + offering.getSchedule().getStart());

    try (IDocumentSession session = documentStore.openSession()) {
      session.advanced().setUseOptimisticConcurrency(true);

      ClassOffering classOffering = session.query(ClassOffering.class)
                                           .whereEquals("schedule.start",
                                                        offering.getSchedule().getStart())
                                           .whereEquals("classType",
                                                        offering.getClassType())
                                           .whereEquals("className",
                                                        offering.getClassName())
                                           .firstOrDefault();
      log.info("classOffering = " + classOffering);

      if (classOffering != null) {
        if (classOffering.getSlots() == 0) {
          // class is full, add message and redirect
          redirectAttributes.addFlashAttribute("message",
                                               "Class is full. Please choose another " +
                                                   "time.");
          redirectAttributes.addFlashAttribute("messageType",
                                               "error");
        } else {

          classOffering.setSlots(classOffering.getSlots() - 1);
          classOffering.setParticipants(classOffering.getParticipants() + 1);
          session.store(classOffering);
          session.saveChanges();

          redirectAttributes.addFlashAttribute("message",
                                               offering.getClassName() + " is booked.");
          redirectAttributes.addFlashAttribute("messageType",
                                               "info");
        }
      } else {
        redirectAttributes.addFlashAttribute("message",
                                             "Class not found.");
        redirectAttributes.addFlashAttribute("messageType",
                                             "error");
      }
    } catch (net.ravendb.client.exceptions.ConcurrencyException e) {

      redirectAttributes.addFlashAttribute("message",
                                           "Error booking class, please try again.");
      redirectAttributes.addFlashAttribute("messageType",
                                           "error");
    }

    return "redirect:/overview#schedule";
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
