package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Simple REST controller to receive class booking information.
 * <p>
 * Endpoint: POST /bookings
 * Content-Type: application/json
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {

  private final RavenDB ravenDB;

  @PostMapping()
  public String createBooking(ClassOffering offering,
                              RedirectAttributes redirectAttributes) {

    log.info("Received booking (ClassOffering): {}", offering);

    log.info("className = " + offering.getClassName());
    log.info("Start:  + " + offering.getSchedule().getStart());

    try (IDocumentSession session = ravenDB.getDocumentStore().openSession()) {
      session.advanced().setUseOptimisticConcurrency(true);

      UserAccount user = session.query(UserAccount.class)
                                .whereEquals("username",
                                             offering.getCustomerInfo().getUsername())
                                .firstOrDefault();

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

          Booking booking = new Booking();
          booking.setClassId(offering.getId());

          // Pass through data
          booking.setUserId(user.getId());
          booking.setUsername(offering.getCustomerInfo().getUsername());
          booking.setDomain(offering.getDomain());

          booking.setDateBooked(LocalDateTime.now());
          session.store(booking);

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
    } catch (ConcurrencyException e) {
      // Document(s) has been modified by another transaction since we fetched it.

      redirectAttributes.addFlashAttribute("message",
                                           "Error booking class, please try again.");
      redirectAttributes.addFlashAttribute("messageType",
                                           "error");
    }

    return "redirect:/#schedule";
  }

}
