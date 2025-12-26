package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple controller to handle class bookings and show user's bookings.
 * <p>
 * POST /bookings — create a booking
 * GET  /bookings — list current user's bookings
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(path = "/bookings")
public class BookingController {

  private final IDocumentSession session;

  @GetMapping
  public String listUserBookings(@RequestParam(name = "page", defaultValue = "1") int page,
                                 @RequestParam(name = "size", defaultValue = "50") int size,
                                 Authentication authentication,
                                 Model model) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 200);
    int skip = (safePage - 1) * safeSize;

    if (authentication == null || authentication.getName() == null) {
      // SecurityConfig already requires auth for /bookings/**, but guard just in case
      return "redirect:/login";
    }

    String username = authentication.getName();

    // Fetch bookings for current user, newest first
    List<Booking> bookings = session.query(Booking.class)
                                    .whereEquals("username", username)
                                    .orderByDescending("dateBooked")
                                    .skip(skip)
                                    .take(safeSize + 1) // over-fetch by one to
                                    // detect if a next page exists
                                    .toList();

    boolean hasNext = bookings.size() > safeSize;
    if (hasNext) {
      bookings = bookings.subList(0, safeSize);
    }

    // Preload and map related ClassOffering docs for display
    Map<String, ClassOffering> offeringsById = new HashMap<>();
    for (Booking b : bookings) {
      if (b.getClassId() != null && !offeringsById.containsKey(b.getClassId())) {
        ClassOffering off = session.load(ClassOffering.class, b.getClassId());
        if (off != null) {
          offeringsById.put(b.getClassId(), off);
        }
      }
    }

    model.addAttribute("title", "My Bookings");
    model.addAttribute("bookings", bookings);
    model.addAttribute("offeringsById", offeringsById);
    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);
    model.addAttribute("hasNext", hasNext);

    return "bookings/index";
  }

  @PostMapping()
  public String createBooking(@ModelAttribute("class") ScheduledClass sClass,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

    log.info("Received booking (ScheduledClass): {}", sClass);

    log.info("className = " + sClass.getClassName());
    log.info("Start:  + " + sClass.getStart());

    try {

      String username = authentication.getName();

      UserAccount user = session.query(UserAccount.class)
                                .whereEquals("username",
                                             username)
                                .firstOrDefault();

      int classHashCode = Math.abs(sClass.hashCode());
      log.info("classHashCode = " + classHashCode);

      // See if anyone has already booked this class
      ScheduledClass scheduledClass =
          session.load(ScheduledClass.class, "ScheduledClass/" + classHashCode);

      log.info("scheduledClass = " + scheduledClass);

      if (scheduledClass == null) {
        // class has not been booked yet, create a new one
        scheduledClass = sClass;
      }

      if (scheduledClass.getSlots() == 0) {
        // class is full, add message and redirect
        redirectAttributes.addFlashAttribute("message",
                                             "Class is full. Please choose another " +
                                                 "time.");
        redirectAttributes.addFlashAttribute("messageType",
                                             "error");
      } else {

        scheduledClass.setSlots(scheduledClass.getSlots() - 1);
        session.store(scheduledClass, "ScheduledClass/" + classHashCode);
        String scheduledClassId = session.advanced().getDocumentId(scheduledClass);

        Booking booking = new Booking();
        booking.setClassId(scheduledClassId);

        // Pass through data
        booking.setUserId(user.getId());
        booking.setUsername(username);

        booking.setDateBooked(LocalDateTime.now());
        session.store(booking);

        session.saveChanges();

        redirectAttributes.addFlashAttribute("message",
                                             sClass.getClassName() + " is booked.");
        redirectAttributes.addFlashAttribute("messageType",
                                             "info");
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

  @PostMapping("/cancel")
  public String cancelBooking(@RequestParam("bookingId") String bookingId,
                              @RequestParam(name = "page", defaultValue = "1") int page,
                              @RequestParam(name = "size", defaultValue = "50") int size,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 200);

    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }

    String username = authentication.getName();

    try {

      Booking booking = session.load(Booking.class, bookingId);
      if (booking == null) {
        redirectAttributes.addFlashAttribute("message", "Booking not found.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/bookings?page=" + safePage + "&size=" + safeSize;
      }

      if (!Objects.equals(username, booking.getUsername())) {
        redirectAttributes.addFlashAttribute("message",
                                             "You are not allowed to cancel this " +
                                                 "booking.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/bookings?page=" + safePage + "&size=" + safeSize;
      }

      // Restore slot counts on the class offering if available
      if (booking.getClassId() != null) {
        ClassOffering off = session.load(ClassOffering.class, booking.getClassId());
        if (off != null) {
          off.setSlots(off.getSlots() + 1);
          off.setParticipants(Math.max(0, off.getParticipants() - 1));
          session.store(off);
        }
      }

      session.delete(booking);
      session.saveChanges();

      redirectAttributes.addFlashAttribute("message", "Your booking has been canceled.");
      redirectAttributes.addFlashAttribute("messageType", "info");
    } catch (ConcurrencyException e) {
      redirectAttributes.addFlashAttribute("message",
                                           "Could not cancel booking due to a " +
                                               "concurrent update. Please try again.");
      redirectAttributes.addFlashAttribute("messageType", "error");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to cancel booking: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
    }

    return "redirect:/bookings?page=" + safePage + "&size=" + safeSize;
  }

}
