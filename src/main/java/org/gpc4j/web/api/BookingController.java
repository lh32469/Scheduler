package org.gpc4j.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import org.gpc4j.web.dto.Booking;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gpc4j.web.components.JsonUtil.MAPPER;
import static org.gpc4j.web.configs.RavenConfig.DB_NAME;

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

  private static final String LOCK_KEY = "locks/bookings";

  private final IDocumentSession session;
  private final DocumentStore documentStore;

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
                                    .orderBy("classStart")
                                    .skip(skip)
                                    .take(safeSize + 1) // over-fetch by one to
                                    // detect if a next page exists
                                    .toList();

    boolean hasNext = bookings.size() > safeSize;
    if (hasNext) {
      bookings = bookings.subList(0, safeSize);
    }

    model.addAttribute("title", "My Bookings");
    model.addAttribute("bookings", bookings);
    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);
    model.addAttribute("hasNext", hasNext);

    return "bookings/index";
  }

  @PostMapping()
  public String createBooking(@RequestParam("json") String scheduleJson,
                              Authentication authentication,
                              @RequestAttribute(DB_NAME) String databaseName,
                              RedirectAttributes redirectAttributes)
      throws JsonProcessingException {

    log.info("Create booking: {}", scheduleJson);

    ScheduledClass sClass = MAPPER.readValue(scheduleJson, ScheduledClass.class);

    log.info("Received booking (ScheduledClass): {}", sClass);

    log.info("className = " + sClass.getClassName());
    log.info("Start:  + " + sClass.getStart());

    String username = authentication.getName();

    UserAccount user = session.query(UserAccount.class)
                              .whereEquals("username", username)
                              .firstOrDefault();

    Booking booking = new Booking(sClass);

    // Pass through data
    booking.setUserId(user.getId());
    booking.setUsername(username);

    SessionOptions sessionOptions = new SessionOptions();
    sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
    sessionOptions.setDatabase(databaseName);

    // Unique identifier for this lock holder
    final String lockValue = UUID.randomUUID().toString();

    try (IDocumentSession clusterSession = documentStore.openSession(sessionOptions)) {

      clusterSession.advanced()
                    .clusterTransaction()
                    .createCompareExchangeValue(LOCK_KEY, lockValue);
      clusterSession.saveChanges();

      // After saveChanges, check if it was successful by retrieving the value
      CompareExchangeValue<String> result =
          clusterSession.advanced().clusterTransaction()
                        .getCompareExchangeValue(String.class, LOCK_KEY);

      if (result != null && lockValue.equals(result.getValue())) {
        try {
          // Lock acquired, do work
          log.debug("Lock acquired: {}", LOCK_KEY);

          int availableSlots = session.query(Booking.class)
                                      .whereEquals("classId", sClass.getId())
                                      .count();

          log.debug(" {} slots already booked for {} ",
                   availableSlots, sClass.getId());

          clusterSession.store(booking);
          clusterSession.saveChanges();

          redirectAttributes.addFlashAttribute("message",
                                               sClass.getClassName() + " is booked.");
          redirectAttributes.addFlashAttribute("messageType",
                                               "info");
        } finally {
          // Release lock
          clusterSession.advanced()
                        .clusterTransaction()
                        .deleteCompareExchangeValue(LOCK_KEY, result.getIndex());
          clusterSession.saveChanges();
          log.info("Lock released: {}", LOCK_KEY);
        }

      }

    }

    return "redirect:/bookings";
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

    log.debug("Cancel booking: {}", bookingId);

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

    session.delete(booking);
    session.saveChanges();

    redirectAttributes.addFlashAttribute("message",
                                         "Your booking has been canceled.");
    redirectAttributes.addFlashAttribute("messageType", "info");

    return "redirect:/bookings?page=" + safePage + "&size=" + safeSize;
  }

}
